/*-
 * ---license-start
 * European Digital COVID Certificate Booking Demo / dgca-booking-demo-backend
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
import eu.europa.ec.dgc.validation.decorator.entity.BookingServiceTokenContentResponse;
import eu.europa.ec.dgc.validation.decorator.entity.BookingServiceTokenContentResponse.PassengerResponse;
import eu.europa.ec.dgc.validation.decorator.entity.ValidationServiceStatusResponse;
import eu.europa.ec.dgc.validation.decorator.exception.NotFoundException;
import eu.europa.ec.dgc.validation.decorator.exception.UncheckedUnsupportedEncodingException;
import eu.europa.ec.dgc.validation.decorator.repository.BookingServiceRepository;
import eu.europa.ec.dgc.validation.decorator.repository.ValidationServiceRepository;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ValidationStatusService {

    private final BookingServiceRepository bookingServiceRepository;

    private final IdentityService identityService;

    private final ValidationServiceRepository validationServiceRepository;

    /**
     * Determines the status of the validation service.
     * 
     * @param subject Subject ID
     * @return {@link ValidationServiceStatusResponse}
     */
    public ValidationServiceStatusResponse determineStatus(final String subject) {
        final BookingServiceTokenContentResponse tokenContent = bookingServiceRepository.tokenContent(subject);
        if (tokenContent.getPassengers() == null || tokenContent.getPassengers().isEmpty()) {
            throw new NotFoundException("Passenger not found by subject");
        }

        final PassengerResponse passenger = tokenContent.getPassengers().get(0);
        String serviceId;
        try {
            serviceId = URLDecoder.decode(passenger.getServiceIdUsed(), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new UncheckedUnsupportedEncodingException(e);
        }
        if (serviceId == null || serviceId.isBlank()) {
            throw new NotFoundException("Passenger without service ID");
        }

        final ServiceProperties service = identityService.getServicePropertiesById(serviceId);
        return validationServiceRepository.status(service, subject);
    }
}
