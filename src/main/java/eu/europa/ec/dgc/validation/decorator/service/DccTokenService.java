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
import eu.europa.ec.dgc.validation.decorator.entity.BookingServiceTokenContentResponse;
import eu.europa.ec.dgc.validation.decorator.entity.BookingServiceTokenContentResponse.FlightInfoResponse;
import eu.europa.ec.dgc.validation.decorator.entity.BookingServiceTokenContentResponse.PassengerResponse;
import eu.europa.ec.dgc.validation.decorator.entity.ValidationServiceInitializeResponse;
import eu.europa.ec.dgc.validation.decorator.exception.NotFoundException;
import eu.europa.ec.dgc.validation.decorator.exception.NotImplementedException;
import eu.europa.ec.dgc.validation.decorator.repository.BookingServiceRepository;
import eu.europa.ec.dgc.validation.decorator.repository.ValidationServiceRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DccTokenService {

    private static final String TYPE_VALIDATION_SERVICE = "ValidationService";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private static final DateTimeFormatter BIRTH_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final DgcProperties dgcProperties;

    private final ValidationServiceRepository validationServiceRepository;

    private final BookingServiceRepository bookingServiceRepository;

    private final IdentityService identityService;

    /**
     * Request validation- and booking service to create token.
     * 
     * @param dccToken {@link DccTokenRequest}
     * @return {@link AccessTokenPayload}
     */
    public AccessTokenPayload getAccessTockenForValidationService(
            final DccTokenRequest dccToken, final String subject) {
        final ServiceProperties service = identityService.getServicePropertiesById(dccToken.getService());
        if (!TYPE_VALIDATION_SERVICE.equalsIgnoreCase(service.getType())) {
            throw new NotImplementedException(String.format("Service type '%s' not implemented", service.getType()));
        }

        final ValidationServiceInitializeResponse initialize = validationServiceRepository
                .initialize(service, dccToken, subject);

        final BookingServiceTokenContentResponse tokenContent = bookingServiceRepository.tokenContent(subject, service);
        if (tokenContent.getPassengers() == null || tokenContent.getPassengers().isEmpty()) {
            throw new NotFoundException("Passenger not found by subject");
        }
        final PassengerResponse passenger = tokenContent.getPassengers().get(0);
        final FlightInfoResponse flightInfo = tokenContent.getFlightInfo();

        return this.buildAccessToken(subject, initialize, passenger, flightInfo);
    }

    private AccessTokenPayload buildAccessToken(
            final String subject,
            final ValidationServiceInitializeResponse initialize,
            final PassengerResponse passenger,
            final FlightInfoResponse flightInfo) {
        final AccessTokenConditions accessTokenConditions = new AccessTokenConditions();
        //  TODO add hash
        accessTokenConditions.setLang(flightInfo.getLanguage());
        accessTokenConditions.setFnt(passenger.getForename());
        accessTokenConditions.setGnt(passenger.getLastname());
        accessTokenConditions.setDob(passenger.getBirthDate().format(BIRTH_DATE_FORMATTER));
        accessTokenConditions.setCoa(flightInfo.getCountryOfArrival());
        accessTokenConditions.setCod(flightInfo.getCountryOfDeparture());
        accessTokenConditions.setRoa(flightInfo.getRegionOfArrival());
        accessTokenConditions.setRod(flightInfo.getRegionOfDeparture());
        accessTokenConditions.setType(flightInfo.getConditionTypes());
        accessTokenConditions.setCategory(flightInfo.getCategories());

        final OffsetDateTime departureTime = flightInfo.getDepartureTime();
        accessTokenConditions.setValidFrom(departureTime.format(FORMATTER));
        accessTokenConditions.setValidationClock(flightInfo.getArrivalTime().format(FORMATTER));
        accessTokenConditions.setValidTo(departureTime.plusDays(2).format(FORMATTER));

        final AccessTokenPayload accessTokenPayload = new AccessTokenPayload();
        //  TODO add jti
        accessTokenPayload.setIss(this.dgcProperties.getToken().getIssuer());
        accessTokenPayload.setIat(Instant.now().getEpochSecond());
        accessTokenPayload.setExp(initialize.getExp());
        accessTokenPayload.setSub(subject);
        accessTokenPayload.setAud(initialize.getAud());
        accessTokenPayload.setType(flightInfo.getType());
        accessTokenPayload.setConditions(accessTokenConditions);
        accessTokenPayload.setVersion("1.0");
        return accessTokenPayload;
    }
}
