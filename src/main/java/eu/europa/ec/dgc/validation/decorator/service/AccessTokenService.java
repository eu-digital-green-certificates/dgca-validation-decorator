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

import eu.europa.ec.dgc.validation.decorator.config.DgcProperties;
import eu.europa.ec.dgc.validation.decorator.exception.DccException;
import eu.europa.ec.dgc.validation.decorator.exception.NotImplementedException;
import eu.europa.ec.dgc.validation.decorator.exception.UncheckedInvalidKeySpecException;
import eu.europa.ec.dgc.validation.decorator.exception.UncheckedNoSuchAlgorithmException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessTokenService {

    public static final String TOKEN_PREFIX = "Bearer ";

    private static final String KEY_PROVIDER = "ks";

    private static final String CONFIG_PROVIDER = "config";

    private final DgcProperties properties;

    private final KeyProvider keyProvider;

    /**
     * This token is generated an default access token without claims.
     */
    public String buildHeaderToken() {
        return String.format("%s%s", TOKEN_PREFIX, buildAccessToken());
    }

    /**
     * This token is generated to access the Validation Decorator endpoints.
     * 
     * @param subject Subject
     * @return {@link String} JWT token
     */
    public String buildHeaderToken(String subject) {
        return String.format("%s%s", TOKEN_PREFIX, buildAccessToken(subject));
    }

    /**
     * This token is generated an default access token without claims.
     */
    public String buildAccessToken() {
        return getAccessTokenBuilder()
                .compact();
    }

    /**
     * This token is generated to access the Validation Decorator endpoints.
     * 
     * @param subject Subject
     * @return {@link String} JWT token
     */
    public String buildAccessToken(final String subject) {
        return getAccessTokenBuilder()
                .addClaims(Collections.singletonMap("sub", subject))
                .compact();
    }

    /**
     * Read content from token, if token is valid.
     * 
     * @param token with or without prefix
     * @return {@link Map} with {@link String} as key and {@link String} as value
     */
    public Map<String, String> parseAccessToken(String token) {
        final String tokenContent = token.startsWith(TOKEN_PREFIX) ? token.replace(TOKEN_PREFIX, "") : token;
        final Jws<Claims> parsedToken = Jwts.parser()
                .setSigningKey(this.getPublicKey())
                .requireIssuer(properties.getToken().getIssuer())
                .parseClaimsJws(tokenContent);
        final Claims body = parsedToken.getBody();
        if (!body.containsKey("sub")) {
            throw new DccException("Token invalid: subjet not found");
        }

        final String subject = body.get("sub", String.class);
        if (subject == null || subject.isBlank()) {
            throw new DccException("Token invalid: subjet is blank");
        }
        return Collections.singletonMap("sub", subject);
    }

    /**
     * Validate token.
     * 
     * @param token with or without prefix
     * @return validation result
     */
    public boolean isValid(final String token) {
        try {
            parseAccessToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private JwtBuilder getAccessTokenBuilder() {
        return Jwts.builder()
                .signWith(properties.getToken().getSignatureAlgorithm(), this.getPrivateKey())
                .setHeaderParam("typ", properties.getToken().getType())
                .setHeaderParam("kid", this.getKeyId())
                .setIssuer(properties.getToken().getIssuer())
                .setExpiration(new Date(Instant.now().plusSeconds(properties.getToken().getValidity()).toEpochMilli()));
    }

    private PublicKey parsePublicKey(String privateKeyBase64) {
        try {
            final byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
            final X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            final KeyFactory kf = KeyFactory.getInstance(this.getKeyAlgorithm());
            return kf.generatePublic(spec);
        } catch (NoSuchAlgorithmException e) {
            throw new UncheckedNoSuchAlgorithmException(e);
        } catch (InvalidKeySpecException e) {
            throw new UncheckedInvalidKeySpecException(e);
        }
    }

    private PrivateKey parsePrivateKey(String privateKeyBase64) {
        try {
            final byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
            final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            final KeyFactory kf = KeyFactory.getInstance(this.getKeyAlgorithm());
            return kf.generatePrivate(spec);
        } catch (NoSuchAlgorithmException e) {
            throw new UncheckedNoSuchAlgorithmException(e);
        } catch (InvalidKeySpecException e) {
            throw new UncheckedInvalidKeySpecException(e);
        }
    }

    private String getKeyAlgorithm() {
        return properties.getToken().getKeyAlgorithm();
    }

    private String getKeyId() {
        if (CONFIG_PROVIDER.equalsIgnoreCase(this.properties.getToken().getProvider())) {
            return "config";
        } else if (KEY_PROVIDER.equalsIgnoreCase(this.properties.getToken().getProvider())) {
            return keyProvider.getKid(keyProvider.getActiveSignKey());
        }
        throw new NotImplementedException(String
                .format("Token provider '%s' not implemented", this.properties.getToken().getProvider()));
    }

    private PublicKey getPublicKey() {
        if (CONFIG_PROVIDER.equalsIgnoreCase(this.properties.getToken().getProvider())) {
            return this.parsePublicKey(this.properties.getToken().getPublicKey());
        } else if (KEY_PROVIDER.equalsIgnoreCase(this.properties.getToken().getProvider())) {
            return keyProvider.receiveCertificate(keyProvider.getActiveSignKey()).getPublicKey();
        }
        throw new NotImplementedException(String
                .format("Token provider '%s' not implemented", this.properties.getToken().getProvider()));
    }

    private PrivateKey getPrivateKey() {
        if (CONFIG_PROVIDER.equalsIgnoreCase(this.properties.getToken().getProvider())) {
            return this.parsePrivateKey(this.properties.getToken().getPrivateKey());
        } else if (KEY_PROVIDER.equalsIgnoreCase(this.properties.getToken().getProvider())) {
            return keyProvider.receivePrivateKey(keyProvider.getActiveSignKey());
        }
        throw new NotImplementedException(String
                .format("Token provider '%s' not implemented", this.properties.getToken().getProvider()));
    }
}
