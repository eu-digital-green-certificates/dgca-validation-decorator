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

package eu.europa.ec.dgc.validation.decorator.controller;

import eu.europa.ec.dgc.validation.decorator.config.QrCodeProperties;
import eu.europa.ec.dgc.validation.decorator.dto.QRCodeDto;
import eu.europa.ec.dgc.validation.decorator.dto.SubjectDto;
import eu.europa.ec.dgc.validation.decorator.service.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class QrCodeDeliveryController {

    private final QrCodeProperties properties;

    private final SubjectService subjectService;

    /**
     * Delivers data for a QR in JSON format.
     * 
     * @param subjectId Subject ID
     * @return {@link QRCodeDto}
     */
    @Operation(summary = "The provision endpoint is the public endpoint for receiving the initialization QR Code", 
            description = "The provision endpoint is the public endpoint for receiving the initialization QR Code")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized, if no active session is attached")
    })
    @GetMapping(value = "/initialize", 
        consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public QRCodeDto initialize(@RequestParam(name = "subject", required = true) String subjectId) {

        SubjectDto subject = subjectService.generate();
        // TODO generate QR code
        return QRCodeDto.builder()
                .protocol(properties.getProtocol())
                .protocolVersion(properties.getProtocolVersion())
                .subject(subject.getSubject())
                .build();
        /*
         * Example QR Code content: {"protocol":"DCCVALIDATION", "protocolVersion":"1.0.0",
         * "serviceidentity":"http://dccexampleprovider/dcc/identity",
         * "validationidentity":"http://validation.dccexampleprovider/dcc/identity",
         * "token":"eyJhbGciOiJFUzI1Ni...9Gufw",
         * "consent":"I want to check your DCC to confirm your booking!:)", “subject”:”Booking Nr. …”, “serviceprovider”
         * :”Service Provider.com”}
         */
    }
}
