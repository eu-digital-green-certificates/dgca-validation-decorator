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

import eu.europa.ec.dgc.validation.decorator.dto.CallbackRequest;
import eu.europa.ec.dgc.validation.decorator.entity.BookingServiceResultRequest;
import eu.europa.ec.dgc.validation.decorator.repository.BookingServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingServiceRepository bookingServiceRepository;

    private final ConversionService converter;

    /**
     * Save result in booking service.
     * 
     * @param subject {@link String}
     * @param request {@link CallbackRequest}
     */
    public void saveResult(String subject, CallbackRequest request) {
        final BookingServiceResultRequest resultRequest = this.converter
                .convert(request, BookingServiceResultRequest.class);
        bookingServiceRepository.result(subject, resultRequest);
    }
}