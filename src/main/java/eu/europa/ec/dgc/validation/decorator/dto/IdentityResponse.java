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

package eu.europa.ec.dgc.validation.decorator.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class IdentityResponse {

    // Service provider URL with "/identity"
    private String id;

    // Verification material for signatures
    private List<VerificationIdentityResponse> verificationMethod = new ArrayList<>();

    // Service endpoints of the service provider
    private List<ServiceIdentityResponse> service = new ArrayList<>();

    @Data
    public static final class VerificationIdentityResponse {

        // Id of the Verification Material
        private String id;

        // Type of the Material
        private String type;

        private String controller;

        private PublicKeyJwkIdentityResponse publicKeyJwk;
    }
    
    @Data
    public static final class PublicKeyJwkIdentityResponse {
        
        private String x5c;
        
        private String kid;
        
        private String alg;
        
        private String use;
    }
    
    @Data
    public static final class ServiceIdentityResponse {
        
        private String id;
        
        private String type;
        
        private String serviceEndpoint;
        
        private String name;
    }
}
