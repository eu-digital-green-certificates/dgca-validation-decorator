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

package eu.europa.ec.dgc.validation.decorator.config;

import io.jsonwebtoken.SignatureAlgorithm;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("dgc")
public class DgcProperties {

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration validationExpire = Duration.ofMinutes(60);

    private String serviceUrl;

    private String keyStoreFile;

    private String keyStorePassword;

    private String privateKeyPassword;

    private String activeSignKey;

    private List<String> encAliases;

    private List<String> signAliases;

    private TokenProperties token;
    
    private List<ServiceProperties> services = new ArrayList<>();
    
    private List<ServiceProperties> endpoints = new ArrayList<>();

    @Data
    public static final class GatewayDownload {

        private Integer timeInterval;

        private Integer lockLimit;
    }

    @Data
    public static final class TokenProperties {

        public static final String ALGORITHM_ELLIPTIC_CURVE = "ES";

        // use "ks" (keyStore) or "config" 
        private String provider;
        
        private String issuer;

        private String type;

        private String algorithm;

        private int keysize;

        private int validity;
        
        private String publicKey;
        
        private String privateKey;
        
        private String keyAlgorithm;

        public SignatureAlgorithm getSignatureAlgorithm() {
            final String name = String.format("%s%d", this.algorithm.toUpperCase(), this.keysize);
            return SignatureAlgorithm.forName(name);
        }
    }
    
    @Data
    public static final class ServiceProperties {

        private String id;

        private String type;

        private String serviceEndpoint;

        private String name;
    }
}
