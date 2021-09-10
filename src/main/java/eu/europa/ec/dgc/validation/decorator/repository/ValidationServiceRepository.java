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

package eu.europa.ec.dgc.validation.decorator.repository;

import eu.europa.ec.dgc.validation.decorator.config.KeysProperties.ServiceProperties;
import eu.europa.ec.dgc.validation.decorator.entity.ValidationServiceIdentityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class ValidationServiceRepository {

    private final RestTemplate restTpl;

    /**
     * Validation service identity endpoint.
     * 
     * @param service {@link ServiceProperties}
     * @return {@link ValidationServiceIdentityResponse}
     */
    public ValidationServiceIdentityResponse identity(final ServiceProperties service) {
        final String controller = service.getController();
        final ResponseEntity<ValidationServiceIdentityResponse> response = restTpl
                .getForEntity(controller, ValidationServiceIdentityResponse.class);
        return response.getBody();
    }

    public void initialize() {
        // TODO ValidationServiceInitializeResponse
    }
}
