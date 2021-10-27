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

import eu.europa.ec.dgc.validation.decorator.service.AccessTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RejectController {

    static final String PATH = "/reject";

    private final AccessTokenService accessTokenService;

    /**
     * Cancels a DCC validation.
     * 
     * @param token Authorization Token
     * @return {@link ResponseEntity}
     */
    @Operation(summary = "Cancels a DCC validation", description = "Cancels a DCC validation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized, if no access token are provided"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error"),
    })
    @GetMapping(value = PATH)
    public ResponseEntity reject(@RequestHeader("Authorization") final String token) {
        log.debug("Incoming GET request to '{}' with token '{}'", PATH, token);

        if (accessTokenService.isValid(token)) {
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache())
                    .build();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .cacheControl(CacheControl.noCache())
                .build();
    }
}
