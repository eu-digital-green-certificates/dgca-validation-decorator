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

import eu.europa.ec.dgc.validation.decorator.dto.AccessTokenPayload;
import eu.europa.ec.dgc.validation.decorator.dto.DccTokenRequest;
import eu.europa.ec.dgc.validation.decorator.service.AccessTokenService;
import eu.europa.ec.dgc.validation.decorator.service.DccTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.Map;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DccTokenController {

    static final String PATH = "/token";

    private final AccessTokenService accessTokenService;

    private final DccTokenService dccTokenService;

    /**
     * Returns an access token for the validation service which contains the information of the booking session.
     * 
     * @param token JWT
     * @param dccToken {@link DccTokenRequest}
     * @return {@link AccessTokenPayload}
     */
    @Operation(summary = "Access token for the validation service", 
            description = "Access token for the validation service")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Bad Request / Validation errors"),
        @ApiResponse(responseCode = "401", description = "Unauthorized, if no access token was provided"),
        @ApiResponse(responseCode = "404", description = "Not Found"),
        @ApiResponse(responseCode = "410", description = "Gone. Repository service reports errors"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error"),
    })
    @PostMapping(value = { "/api" + PATH, PATH }, 
        consumes = MediaType.APPLICATION_JSON_VALUE, produces = "application/jwt")
    public ResponseEntity<String> token(
            @RequestHeader("Authorization") final String token,
            @Valid @RequestBody final DccTokenRequest dccToken) {
        log.debug("Incoming POST request to '{}' with content '{}' and token '{}'", PATH, dccToken, token);

        if (this.accessTokenService.isValid(token)) {
            final Map<String, Object> tokenContent = this.accessTokenService.parseAccessToken(token);
            if (tokenContent.containsKey("sub") && tokenContent.get("sub") instanceof String) {
                final String subject = (String) tokenContent.get("sub");
                final AccessTokenPayload accessTockenPayload = dccTokenService
                        .getAccessTockenForValidationService(dccToken, subject);
                final String accessToken = this.accessTokenService.buildAccessToken(accessTockenPayload);

                final HttpHeaders headers = new HttpHeaders();
                headers.set("X-Nonce", accessTockenPayload.getNonce());
                return ResponseEntity.ok()
                        .headers(headers)
                        .cacheControl(CacheControl.noCache())
                        .body(accessToken);
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .cacheControl(CacheControl.noCache())
                .build();
    }
}
