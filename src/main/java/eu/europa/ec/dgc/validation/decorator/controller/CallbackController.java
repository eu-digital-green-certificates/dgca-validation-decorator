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

import eu.europa.ec.dgc.validation.decorator.dto.CallbackRequest;
import eu.europa.ec.dgc.validation.decorator.service.BackendService;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CallbackController {

    private static final String PATH = "/callback/{subject}";

    private final BackendService backendService;

    /**
     * Callback endpoint receives the validation result to a subject.
     * 
     * @param subject Subject
     */
    @Operation(summary = "The optional callback endpoint receives the validation result to a subject", description = "The optional callback endpoint receives the validation result to a subject")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized, if Result Token was not correctly signed"),
        @ApiResponse(responseCode = "410", description = "Gone. Subject does not exist anymore"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @PutMapping(value = PATH, consumes = { "application/jwt", MediaType.TEXT_PLAIN_VALUE })
    public ResponseEntity callback(
            @PathVariable(value = "subject", required = true) final String subject,
            @RequestHeader("X-Version") final String version,
            @Valid @RequestBody final String body) {
        log.debug("Incoming PUT request to '{}' with subject '{}' and bldy '{}'", PATH, subject, body);
        try {
            final CallbackRequest request = this.backendService.parseRequest(subject, body);
            this.backendService.saveResult(subject, request);    
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache())
                    .build();
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .cacheControl(CacheControl.noCache())
                    .build();
        }
    }
}
