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

import eu.europa.ec.dgc.validation.decorator.entity.ValidationServiceStatusResponse;
import eu.europa.ec.dgc.validation.decorator.service.AccessTokenService;
import eu.europa.ec.dgc.validation.decorator.service.ValidationStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ValidationStatusController {

    private static final String PATH = "/status";
    
    private final ValidationStatusService validationStatusService;

    private final AccessTokenService accessTokenService;

    /**
     * Provides information on the current status.
     * 
     * @param token Authorization token from header
     * @return The response is via the HTTP status code, the content is empty
     */
    @Operation(summary = "The validation status endpoint provides the validation result of a subject", 
            description = "The validation status endpoint provides the validation result of a subject")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "204", description = "No content, wait for status"),
        @ApiResponse(responseCode = "401", description = "Unauthorized, if no access token are provided"),
        @ApiResponse(responseCode = "410", description = "Gone. Subject does not exist anymore"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @GetMapping(value = PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity status(@RequestHeader("Authorization") final String token) {
        log.debug("Incoming GET request to '{}' with token '{}'", PATH, token);
        
        if (accessTokenService.isValid(token)) {
            final Map<String, String> tokenContent = accessTokenService.parseAccessToken(token);
            if (tokenContent.containsKey("sub") && tokenContent.get("sub") != null) {
                final String subject = tokenContent.get("sub");
                final ValidationServiceStatusResponse status = validationStatusService.determineStatus(subject);
                return ResponseEntity.status(status.getHttpStatusCode()).build();
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
