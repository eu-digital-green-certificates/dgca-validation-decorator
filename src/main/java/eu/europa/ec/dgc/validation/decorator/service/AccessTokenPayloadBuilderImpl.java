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
import eu.europa.ec.dgc.validation.decorator.dto.AccessTokenPayload.AccessTokenConditions;
import eu.europa.ec.dgc.validation.decorator.entity.ServiceTokenContentResponse.OccurrenceInfoResponse;
import eu.europa.ec.dgc.validation.decorator.entity.ServiceTokenContentResponse.SubjectResponse;
import eu.europa.ec.dgc.validation.decorator.entity.ValidationServiceInitializeResponse;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessTokenPayloadBuilderImpl implements AccessTokenPayloadBuilder {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxxxx");
    
    private final DgcProperties dgcProperties;
    
    @Override
    public AccessTokenPayload build(
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
}
