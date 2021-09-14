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
import eu.europa.ec.dgc.validation.decorator.entity.BookingServiceBoardingPassResponse;
import eu.europa.ec.dgc.validation.decorator.entity.BookingServiceTokenContentResponse;
import eu.europa.ec.dgc.validation.decorator.entity.BookingServiceTokenContentResponse.PassengerResponse;
import eu.europa.ec.dgc.validation.decorator.entity.ValidationServiceInitializeResponse;
import eu.europa.ec.dgc.validation.decorator.exception.NotFoundException;
import eu.europa.ec.dgc.validation.decorator.repository.BookingServiceRepository;
import eu.europa.ec.dgc.validation.decorator.repository.ValidationServiceRepository;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DccTokenService {

    private final DgcProperties dgcProperties;

    private final ValidationServiceRepository validationServiceRepository;

    private final BookingServiceRepository bookingServiceRepository;

    /**
     * Request validation- and booking service to create token.
     * 
     * @param dccToken {@link DccTokenRequest}
     * @return {@link AccessTokenPayload}
     */
    public AccessTokenPayload getAccessTockenForValidationService(
            final DccTokenRequest dccToken, final String subject) {
        final ServiceProperties service = getServicePropertiesById(dccToken.getService());

        final ValidationServiceInitializeResponse initialize = validationServiceRepository
                .initialize(service, dccToken, subject);

        final BookingServiceTokenContentResponse tokenContent = bookingServiceRepository.tokenContent(subject);
        if (tokenContent.getPassengers() == null || tokenContent.getPassengers().isEmpty()) {
            throw new NotFoundException("Passenger not found by subject");
        }
        final PassengerResponse passenger = tokenContent.getPassengers().get(0);

        final BookingServiceBoardingPassResponse boardingPass = bookingServiceRepository.boardingPass(subject);

        AccessTokenConditions accessTokenConditions = new AccessTokenConditions();
        accessTokenConditions.setLang("en-en"); // TODO Selected language
        accessTokenConditions.setFnt(passenger.getForename());
        accessTokenConditions.setGnt(passenger.getLastname());
        accessTokenConditions.setDob(passenger.getBirthDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        accessTokenConditions.setCoa("NL"); // TODO Country of Arrival
        accessTokenConditions.setCod("DE"); // TODO Country of Departure
        accessTokenConditions.setRoa("AW"); // TODO Region of Arrival ISO 3166-2 without Country
        accessTokenConditions.setRod("BW"); // TODO Region of Departure ISO 3166-2 without Country
        accessTokenConditions.setType(Arrays.asList("r", "v", "t")); // TODO dynamic
        accessTokenConditions.setCategory(Arrays.asList("Standard")); // TODO dynamic 
        accessTokenConditions.setValidFrom(boardingPass.getFlightInfo()
                .getTime().format(DateTimeFormatter.ISO_DATE_TIME));
        accessTokenConditions.setValidationClock(boardingPass.getFlightInfo()
                .getTime().plusDays(1).format(DateTimeFormatter.ISO_DATE_TIME)); // TODO dynamic
        accessTokenConditions.setValidTo(boardingPass.getFlightInfo()
                .getTime().plusDays(2).format(DateTimeFormatter.ISO_DATE_TIME)); // TODO dynamic

        AccessTokenPayload accessTokenPayload = new AccessTokenPayload();
        accessTokenPayload.setIss(dgcProperties.getToken().getIssuer());
        accessTokenPayload.setIat(Instant.now().getEpochSecond());
        accessTokenPayload.setExp(initialize.getExp());
        accessTokenPayload.setSub(subject);
        accessTokenPayload.setAud(initialize.getAud());
        accessTokenPayload.setType(2);
        accessTokenPayload.setConditions(accessTokenConditions);
        accessTokenPayload.setVersion("1.0");

        return accessTokenPayload;
    }

    private ServiceProperties getServicePropertiesById(final String serviceId) {
        if (serviceId != null && dgcProperties.getServices() != null) {
            return dgcProperties.getServices().stream()
                    .filter(service -> serviceId.equals(service.getId()))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException(String.format("Service not found by ID '%s'", serviceId)));
        }
        throw new NotFoundException("Verification method not found. No ID available.");
    }
}
