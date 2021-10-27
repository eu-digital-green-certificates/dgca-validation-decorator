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
import eu.europa.ec.dgc.validation.decorator.dto.AccessTokenPayload;
import eu.europa.ec.dgc.validation.decorator.dto.DccTokenRequest;
import eu.europa.ec.dgc.validation.decorator.entity.ServiceTokenContentResponse;
import eu.europa.ec.dgc.validation.decorator.entity.ServiceTokenContentResponse.OccurrenceInfoResponse;
import eu.europa.ec.dgc.validation.decorator.entity.ServiceTokenContentResponse.SubjectResponse;
import eu.europa.ec.dgc.validation.decorator.entity.ValidationServiceInitializeResponse;
import eu.europa.ec.dgc.validation.decorator.exception.NotFoundException;
import eu.europa.ec.dgc.validation.decorator.exception.NotImplementedException;
import eu.europa.ec.dgc.validation.decorator.exception.RepositoryException;
import eu.europa.ec.dgc.validation.decorator.repository.BackendRepository;
import eu.europa.ec.dgc.validation.decorator.repository.ValidationServiceRepository;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Service
@RequiredArgsConstructor
public class DccTokenService {

    private static final String TYPE_VALIDATION_SERVICE = "ValidationService";

    private final ValidationServiceRepository validationServiceRepository;

    private final BackendRepository backendRepository;

    private final IdentityService identityService;

    private final AccessTokenPayloadBuilder accessTokenPayloadBuilder;

    /**
     * Request validation- and backend service to create token.
     * 
     * @param dccToken {@link DccTokenRequest}
     * @return {@link AccessTokenPayload}
     */
    public AccessTokenPayload getAccessTockenForValidationService(
            final DccTokenRequest dccToken, final String subject) {
        final ServiceProperties service = this.identityService.getServicePropertiesById(dccToken.getService());
        if (!TYPE_VALIDATION_SERVICE.equalsIgnoreCase(service.getType())) {
            throw new NotImplementedException(String.format("Service type '%s' not implemented", service.getType()));
        }

        final String nonce = buildNonce();
        final ValidationServiceInitializeResponse initialize = this.getValidationServiceInitialize(
                dccToken, subject, service, nonce);

        final ServiceTokenContentResponse tokenContent = this.getBackendServiceTokenContent(subject, service);
        if (tokenContent.getSubjects() == null || tokenContent.getSubjects().isEmpty()) {
            throw new NotFoundException("Subject not found in token");
        }
        final SubjectResponse subjectResponse = tokenContent.getSubjects().get(0);
        final OccurrenceInfoResponse occurrenceInfo = tokenContent.getOccurrenceInfo();

        final AccessTokenPayload accessToken = this.accessTokenPayloadBuilder.build(
                subject, initialize, subjectResponse, occurrenceInfo);
        accessToken.setNonce(nonce);
        return accessToken;
    }

    private String buildNonce() {
        byte[] randomBytes = new byte[16];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getEncoder().encodeToString(randomBytes);
    }

    private ValidationServiceInitializeResponse getValidationServiceInitialize(final DccTokenRequest dccToken,
            final String subject, final ServiceProperties service, final String nonce) {
        try {
            return this.validationServiceRepository.initialize(service, dccToken, subject, nonce);
        } catch (HttpClientErrorException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryException("Validation service http client error", e);
        }
    }

    private ServiceTokenContentResponse getBackendServiceTokenContent(final String subject,
            final ServiceProperties service) {
        try {
            return this.backendRepository.tokenContent(subject, service);
        } catch (HttpClientErrorException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryException("Backend service http client error", e);
        }
    }
}
