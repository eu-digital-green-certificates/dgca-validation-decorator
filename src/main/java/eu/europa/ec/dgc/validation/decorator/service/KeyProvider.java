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
import eu.europa.ec.dgc.validation.decorator.exception.DccException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeyProvider {

    private final DgcProperties dgcProperties;

    private final Map<KeyType, Certificate> certificates = new HashMap<>();

    private final Map<KeyType, PrivateKey> privateKeys = new HashMap<>();

    private final Map<KeyType, String> kids = new HashMap<>();

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
        final char[] keyStorePassword = dgcProperties.getKeyStorePassword().toCharArray();

        Security.addProvider(new BouncyCastleProvider());
        Security.setProperty("crypto.policy", "unlimited");

        KeyStore keyStore = KeyStore.getInstance("JKS");
        File keyFile = new File(dgcProperties.getKeyStoreFile());
        if (!keyFile.isFile()) {
            String msg = String.format(
                    "keyfile not found on: %s please adapt the configuration property: issuance.keyStoreFile", keyFile);
            log.error(msg);
            throw new DccException(msg);
        }

        CertificateUtils certificateUtils = new CertificateUtils();
        try (InputStream is = new FileInputStream(dgcProperties.getKeyStoreFile())) {
            keyStore.load(is, dgcProperties.getPrivateKeyPassword().toCharArray());
            KeyStore.PasswordProtection keyPassword = new KeyStore.PasswordProtection(keyStorePassword);

            for (KeyType keyType : KeyType.values()) {
                String keyName = keyType.name().toLowerCase();
                PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(keyName, keyPassword);
                Certificate cert = keyStore.getCertificate(keyName);
                PrivateKey privateKey = privateKeyEntry.getPrivateKey();
                
                certificates.put(keyType, cert);
                privateKeys.put(keyType, privateKey);
                kids.put(keyType, certificateUtils.getCertKid((X509Certificate) cert));
            }
        }
    }

    public Certificate receiveCertificate(KeyType keyType) {
        return certificates.get(keyType);
    }

    public PrivateKey receivePrivateKey(KeyType keyType) {
        return privateKeys.get(keyType);
    }

    public String getKid(KeyType keyType) {
        return kids.get(keyType);
    }
}
