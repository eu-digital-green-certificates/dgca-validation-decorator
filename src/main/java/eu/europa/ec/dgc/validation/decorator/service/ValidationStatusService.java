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

import eu.europa.ec.dgc.validation.decorator.config.DgcProperties.ServiceProperties;
import eu.europa.ec.dgc.validation.decorator.entity.ServiceTokenContentResponse;
import eu.europa.ec.dgc.validation.decorator.entity.ServiceTokenContentResponse.SubjectResponse;
import eu.europa.ec.dgc.validation.decorator.entity.ValidationServiceStatusResponse;
import eu.europa.ec.dgc.validation.decorator.exception.NotFoundException;
import eu.europa.ec.dgc.validation.decorator.exception.UncheckedUnsupportedEncodingException;
import eu.europa.ec.dgc.validation.decorator.repository.BackendRepository;
import eu.europa.ec.dgc.validation.decorator.repository.ValidationServiceRepository;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ValidationStatusService {

    private final BackendRepository backendRepository;

    private final IdentityService identityService;

    private final ValidationServiceRepository validationServiceRepository;

    /**
     * Determines the status of the validation service.
     * 
     * @param subject Subject ID
     * @return {@link ValidationServiceStatusResponse}
     */
    public ValidationServiceStatusResponse determineStatus(final String subject) {
        final ServiceTokenContentResponse tokenContent = backendRepository.tokenContent(subject);
        if (tokenContent.getSubjects() == null || tokenContent.getSubjects().isEmpty()) {
            throw new NotFoundException("Passenger not found by subject");
        }

        final SubjectResponse subjectResponse = tokenContent.getSubjects().get(0);
        final String serviceId = subjectResponse.getServiceIdUsed();
        if (serviceId == null || serviceId.isBlank()) {
            throw new NotFoundException("Passenger without service ID");
        }

        String decodedServiceId;
        try {
            decodedServiceId = URLDecoder.decode(serviceId, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new UncheckedUnsupportedEncodingException(e);
        }

        final ServiceProperties service = identityService.getServicePropertiesById(decodedServiceId);
        return validationServiceRepository.status(service, subject);
    }
}
