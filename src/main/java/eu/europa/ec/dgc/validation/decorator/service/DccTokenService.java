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
import eu.europa.ec.dgc.validation.decorator.config.DgcProperties.ServiceProperties;
import eu.europa.ec.dgc.validation.decorator.dto.AccessTokenPayload;
import eu.europa.ec.dgc.validation.decorator.dto.AccessTokenPayload.AccessTokenConditions;
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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxxxx");

    private final DgcProperties dgcProperties;

    private final ValidationServiceRepository validationServiceRepository;

    private final BackendRepository backendRepository;

    private final IdentityService identityService;

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

        final AccessTokenPayload accessToken = this.buildAccessToken(
                subject, initialize, subjectResponse, occurrenceInfo);
        accessToken.setNonce(nonce);
        return accessToken;
    }

    private String buildNonce() {
        byte[] randomBytes = new byte[16];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getEncoder().encodeToString(randomBytes);
    }

    private AccessTokenPayload buildAccessToken(
            final String subject,
            final ValidationServiceInitializeResponse initialize,
            final SubjectResponse subjectResponse,
            final OccurrenceInfoResponse occurrenceInfo) {
        final AccessTokenConditions accessTokenConditions = new AccessTokenConditions();
        accessTokenConditions.setLang(occurrenceInfo.getLanguage());
        accessTokenConditions.setFnt(subjectResponse.getForename());
        accessTokenConditions.setGnt(subjectResponse.getLastname());
        accessTokenConditions.setCoa(occurrenceInfo.getCountryOfArrival());
        accessTokenConditions.setCod(occurrenceInfo.getCountryOfDeparture());
        accessTokenConditions.setRoa(occurrenceInfo.getRegionOfArrival());
        accessTokenConditions.setRod(occurrenceInfo.getRegionOfDeparture());
        accessTokenConditions.setType(occurrenceInfo.getConditionTypes());
        accessTokenConditions.setCategory(occurrenceInfo.getCategories());
        if (subjectResponse.getBirthDate() != null && !subjectResponse.getBirthDate().isBlank()) {
            accessTokenConditions.setDob(subjectResponse.getBirthDate());
        }

        final OffsetDateTime departureTime = occurrenceInfo.getDepartureTime();
        accessTokenConditions.setValidFrom(departureTime.format(FORMATTER));
        accessTokenConditions.setValidationClock(occurrenceInfo.getArrivalTime().format(FORMATTER));
        accessTokenConditions.setValidTo(departureTime.plusDays(2).format(FORMATTER));

        final AccessTokenPayload accessTokenPayload = new AccessTokenPayload();
        accessTokenPayload.setJti(subjectResponse.getJti());
        accessTokenPayload.setIss(this.dgcProperties.getToken().getIssuer());
        accessTokenPayload.setIat(Instant.now().getEpochSecond());
        accessTokenPayload.setExp(initialize.getExp());
        accessTokenPayload.setSub(subject);
        accessTokenPayload.setAud(initialize.getAud());
        accessTokenPayload.setType(occurrenceInfo.getType());
        accessTokenPayload.setConditions(accessTokenConditions);
        accessTokenPayload.setVersion("1.0");
        return accessTokenPayload;
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
