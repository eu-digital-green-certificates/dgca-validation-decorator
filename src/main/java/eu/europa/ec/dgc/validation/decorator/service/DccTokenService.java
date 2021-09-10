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

import eu.europa.ec.dgc.validation.decorator.config.KeysProperties;
import eu.europa.ec.dgc.validation.decorator.config.KeysProperties.ServiceProperties;
import eu.europa.ec.dgc.validation.decorator.dto.AccessTokenPayload;
import eu.europa.ec.dgc.validation.decorator.dto.DccTokenRequest;
import eu.europa.ec.dgc.validation.decorator.entity.ValidationServiceIdentityResponse;
import eu.europa.ec.dgc.validation.decorator.exception.NotFoundException;
import eu.europa.ec.dgc.validation.decorator.repository.ValidationServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DccTokenService {

    private final KeysProperties keysProperties;

    private final ValidationServiceRepository validationServiceRepository;

    /**
     * Request validation- and booking service to create token.
     * 
     * @param dccToken {@link DccTokenRequest}
     * @return {@link AccessTokenPayload}
     */
    public AccessTokenPayload getAccessTockenForValidationService(DccTokenRequest dccToken) {
        final ServiceProperties service = getServicePropertiesById(dccToken.getService());
        ValidationServiceIdentityResponse identity = validationServiceRepository.identity(service);
        // TODO impl
        return null;
    }

    private ServiceProperties getServicePropertiesById(final String serviceId) {
        if (serviceId != null) {
            return keysProperties.getVerificationMethods().stream()
                    .filter(ver -> serviceId.equalsIgnoreCase(ver.getId()))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException(
                            String.format("Verification method not found by ID '%s'", serviceId)));
        }
        throw new NotFoundException("Verification method not found. No ID available.");
    }
}
