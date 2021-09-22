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
import eu.europa.ec.dgc.validation.decorator.dto.AccessTokenPayload;
import eu.europa.ec.dgc.validation.decorator.exception.DccException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultJwtParser;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AccessTokenService {

    public static final String TOKEN_PREFIX = "Bearer ";

    private final DgcProperties properties;

    private final KeyProvider keyProvider;

    /**
     * This token is generated an default header token without 'Bearer' prefix.
     */
    public String buildHeaderToken() {
        return String.format("%s%s", TOKEN_PREFIX, this.buildAccessToken());
    }

    /**
     * This token is generated an default header token without 'Bearer' prefix.
     * 
     * @param subject Subject
     * @return {@link String} JWT token
     */
    public String buildHeaderToken(final String subject) {
        return String.format("%s%s", TOKEN_PREFIX, this.buildAccessToken(subject));
    }

    /**
     * This token is generated an default access token without claims.
     */
    public String buildAccessToken() {
        return this.getAccessTokenBuilder()
                .compact();
    }

    /**
     * This token is generated to access the Validation Decorator endpoints.
     * 
     * @param subject Subject
     * @return {@link String} JWT token
     */
    public String buildAccessToken(final String subject) {
        return this.getAccessTokenBuilder()
                .addClaims(Collections.singletonMap("sub", subject))
                .compact();
    }

    /**
     * Generates access token from from {@link AccessTokenPayload}.
     * 
     * @param payload {@link AccessTokenPayload}
     * @return {@link String} JWT token
     */
    public String buildAccessToken(final AccessTokenPayload payload) {
        final Map<String, Object> claims = new HashMap<>();
        claims.put("jti", payload.getJti());
        claims.put("sub", payload.getSub());
        claims.put("aud", payload.getAud());
        claims.put("iat", payload.getIat());
        claims.put("t", payload.getType());
        claims.put("v", payload.getVersion());
        claims.put("vc", payload.getConditions());

        final JwtBuilder builder = this.getAccessTokenBuilder()
                .setExpiration(new Date(payload.getExp()))
                .addClaims(claims);
        return builder.compact();
    }

    /**
     * Read content from token, if token is valid.
     * 
     * @param token with or without prefix
     * @return {@link Map} with {@link String} as key and {@link String} as value
     */
    public Map<String, Object> parseAccessToken(final String token) {
        final String activeSignKey = this.keyProvider.getActiveSignKey();
        final PublicKey publicKey = this.keyProvider.receiveCertificate(activeSignKey).getPublicKey();
        final String issuer = this.properties.getToken().getIssuer();

        final Map<String, Object> body = this.parseAccessToken(token, publicKey, issuer);
        if (!body.containsKey("sub")) {
            throw new DccException("Token invalid: subjet not found");
        }

        final Object subject = body.get("sub");
        if (!(subject instanceof String && !((String) subject).isBlank())) {
            throw new DccException("Token invalid: subjet is blank");
        }
        return body;
    }

    /**
     * Read content from token, if token is valid.
     * 
     * @param token with or without prefix
     * @return {@link Map} with {@link String} as key and {@link String} as value
     */
    public Map<String, Object> parseAccessToken(final String token, final PublicKey publicKey) {
        return this.parseAccessToken(token, publicKey, null);
    }

    /**
     * Read content from token, if token is valid.
     * 
     * @param token with or without prefix
     * @return {@link Map} with {@link String} as key and {@link String} as value
     */
    public Map<String, Object> parseAccessToken(final String token, final PublicKey publicKey, final String issuer) {
        final String tokenContent = token.startsWith(TOKEN_PREFIX) ? token.replace(TOKEN_PREFIX, "") : token;

        final JwtParser parser = Jwts.parser().setSigningKey(publicKey);
        if (StringUtils.hasText(issuer)) {
            parser.requireIssuer(this.properties.getToken().getIssuer());
        }

        final Jws<Claims> parsedToken = parser.parseClaimsJws(tokenContent);
        return new HashMap<>(parsedToken.getBody());
    }

    /**
     * Parses the token without any security checks.
     * 
     * @param token JWT
     * @return parsed JWT
     */
    public Jwt parseUnsecure(final String token) {
        final String[] splitToken = token.split("\\.");
        final String unsignedToken = splitToken[0] + "." + splitToken[1] + ".";
        final DefaultJwtParser parser = new DefaultJwtParser();
        return parser.parse(unsignedToken);
    }

    /**
     * Validate token.
     * 
     * @param token with or without prefix
     * @return validation result
     */
    public boolean isValid(final String token) {
        try {
            this.parseAccessToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private JwtBuilder getAccessTokenBuilder() {
        final String activeSignKey = this.keyProvider.getActiveSignKey();
        final PrivateKey privateKey = this.keyProvider.receivePrivateKey(activeSignKey);
        final String algorithm = this.keyProvider.getAlg(activeSignKey);
        final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.valueOf(algorithm);
        final String keyId = this.keyProvider.getKid(activeSignKey);
        final int validity = this.properties.getToken().getInitialize().getValidity();

        return Jwts.builder()
                .signWith(signatureAlgorithm, privateKey)
                .setHeaderParam("typ", this.properties.getToken().getType())
                .setHeaderParam("kid", keyId)
                .setIssuer(this.properties.getToken().getIssuer())
                .setExpiration(new Date(Instant.now().plusSeconds(validity).toEpochMilli()));
    }
}
