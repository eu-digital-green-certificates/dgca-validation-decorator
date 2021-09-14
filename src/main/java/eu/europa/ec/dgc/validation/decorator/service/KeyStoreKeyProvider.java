/*-
 * ---license-start
 * European Digital COVID Certificate Validation Decorator Service / dgca-validation-decorator
 * ---
 * Copyright (C) 2021 T-Systems International GmbH and all other contributors
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package eu.europa.ec.dgc.validation.decorator.service;

import eu.europa.ec.dgc.utils.CertificateUtils;
import eu.europa.ec.dgc.validation.decorator.config.DgcProperties;
import eu.europa.ec.dgc.validation.decorator.entity.KeyType;
import eu.europa.ec.dgc.validation.decorator.entity.KeyUse;
import eu.europa.ec.dgc.validation.decorator.exception.DccException;
import eu.europa.ec.dgc.validation.decorator.exception.NotImplementedException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeyStoreKeyProvider implements KeyProvider {

    private final DgcProperties dgcConfigProperties;

    private final Map<String, Certificate> certificates = new HashMap<>();

    private final Map<String, PrivateKey> privateKeys = new HashMap<>();

    private final Map<String, String> kids = new HashMap<>();

    private final Map<String, String> algs = new HashMap<>();

    private final Map<String, String> kidToName = new HashMap<>();

    /**
     * Initializes the data of the class when the application is started.
     * 
     * @throws NoSuchAlgorithmException if the algorithm cannot be found
     * @throws IOException if the keystore file does not exist
     * @throws CertificateException if certificate could not be loaded
     * @throws KeyStoreException if the keystore has not been initialized
     * @throws UnrecoverableEntryException if the entry does not contain the information needed to recover
     */
    @PostConstruct
    public void createKeys() throws NoSuchAlgorithmException, IOException, CertificateException,
            KeyStoreException, UnrecoverableEntryException {
        Security.addProvider(new BouncyCastleProvider());
        Security.setProperty("crypto.policy", "unlimited");

        final Path filePath = Path.of(dgcConfigProperties.getKeyStoreFile());
        if (!Files.exists(filePath)) {
            log.error("keyfile not found on: {} please adapt the configuration property: issuance.keyStoreFile",
                    filePath);
            throw new DccException("keyfile not found on: " + filePath
                    + " please adapt the configuration property: issuance.keyStoreFile");
        }

        final CertificateUtils certificateUtils = new CertificateUtils();
        final KeyStore keyStore = KeyStore.getInstance("JKS");
        final char[] keyStorePassword = dgcConfigProperties.getKeyStorePassword().toCharArray();
        try (InputStream is = new FileInputStream(dgcConfigProperties.getKeyStoreFile())) {
            final char[] privateKeyPassword = dgcConfigProperties.getPrivateKeyPassword().toCharArray();
            keyStore.load(is, privateKeyPassword);
            KeyStore.PasswordProtection keyPassword = new KeyStore.PasswordProtection(keyStorePassword);

            for (String alias : this.getKeyNames(KeyType.ALL)) {
                final PrivateKeyEntry privateKeyEntry = (PrivateKeyEntry) keyStore.getEntry(alias, keyPassword);

                if (privateKeyEntry != null) {
                    final X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
                    PrivateKey privateKey = privateKeyEntry.getPrivateKey();
                    certificates.put(alias, cert);
                    privateKeys.put(alias, privateKey);

                    final String kid = certificateUtils.getCertKid((X509Certificate) cert);
                    kids.put(alias, kid);
                    kidToName.put(kid, alias);

                    if (cert.getSigAlgOID().contains("1.2.840.113549.1.1.1")) {
                        algs.put(alias, "RS256");
                    } else if (cert.getSigAlgOID().contains("1.2.840.113549.1.1.10")) {
                        algs.put(alias, "PS256");
                    } else if (cert.getSigAlgOID().contains("1.2.840.10045.4.3.2")) {
                        algs.put(alias, "ES256");
                    } else {
                        throw new NotImplementedException(String.format("SigAlg OID '{}'", cert.getSigAlgOID()));
                    }
                }
            }
        }
    }

    @Override
    public Certificate receiveCertificate(String keyName) {
        return certificates.get(keyName);
    }

    @Override
    public PrivateKey receivePrivateKey(String keyName) {
        return privateKeys.get(keyName);
    }

    @Override
    public List<String> getKeyNames(KeyType type) {
        final List<String> keyNames = new ArrayList<>();
        switch (type) {
            case VALIDATION_DECORATOR_ENC_KEY:
                keyNames.addAll(dgcConfigProperties.getEncAliases());
                break;
            case VALIDATION_DECORATOR_SIGN_KEY:
                keyNames.addAll(dgcConfigProperties.getSignAliases());
                break;
            case ALL:
                keyNames.addAll(dgcConfigProperties.getEncAliases());
                keyNames.addAll(dgcConfigProperties.getSignAliases());
                break;
            default:
                throw new NotImplementedException(String.format("Key type '%s'", type));
        }
        return keyNames;
    }

    @Override
    public String getKid(String keyName) {
        return kids.get(keyName);
    }

    @Override
    public String getAlg(String keyName) {
        return algs.get(keyName);
    }

    @Override
    public String getActiveSignKey() {
        return dgcConfigProperties.getActiveSignKey();
    }

    @Override
    public String getKeyName(String kid) {
        return kidToName.get(kid);
    }

    @Override
    public KeyUse getKeyUse(String keyName) {
        return dgcConfigProperties.getEncAliases().contains(keyName) ? KeyUse.ENC : KeyUse.SIG;
    }
}
