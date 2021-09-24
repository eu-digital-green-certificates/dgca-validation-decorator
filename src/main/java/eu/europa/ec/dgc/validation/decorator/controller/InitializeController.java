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

import eu.europa.ec.dgc.validation.decorator.dto.QrCodeDto;
import eu.europa.ec.dgc.validation.decorator.service.InitializeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class InitializeController {

    static final String PATH = "/initialize/{subject}";

    private final InitializeService initializeService;

    /**
     * Delivers data for a QR in JSON format.
     *
     * @param subject Subject
     * @return {@link QrCodeDto} content for QR code
     */
    @Operation(summary = "The provision endpoint is the public endpoint for receiving the initialization QR Code",
            description = "The provision endpoint is the public endpoint for receiving the initialization QR Code")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Bad Request / Validation errors"),
        @ApiResponse(responseCode = "401", description = "Unauthorized, if no active session is attached"),
        @ApiResponse(responseCode = "404", description = "Not Found"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error"),
    })
    @GetMapping(value = { "/api" + PATH, PATH }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QrCodeDto> initialize(
            @PathVariable(value = "subject", required = true) final String subject) {
        log.debug("Incoming GET request to '{}' with subject '{}'", PATH, subject);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(initializeService.getBySubject(subject));
    }
}
