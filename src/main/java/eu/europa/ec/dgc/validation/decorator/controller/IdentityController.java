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

import eu.europa.ec.dgc.validation.decorator.dto.IdentityResponse;
import eu.europa.ec.dgc.validation.decorator.service.IdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class IdentityController {

    public static final String PATH_ALL = "/identity";
    
    private static final String PATH_ELEMENT = "/identity/{element}";
    
    private static final String PATH_ELEMENT_ID = "/identity/{element}/{id}";

    private final IdentityService identityService;

    /**
     * Delivers a JSON description of public keys and endpoints.
     * 
     * @return {@link IdentityResponse}
     */
    @Operation(summary = "The identity document endpoint delivers a JSON description of public keys and endpoints", 
            description = "The identity document endpoint delivers a JSON description of public keys and endpoints")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Bad Request / Validation errors"),
        @ApiResponse(responseCode = "401", description = "Unauthorized, if no active session is attached"),
        @ApiResponse(responseCode = "404", description = "Not Found"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error"),
    })
    @GetMapping(value = PATH_ALL, produces = MediaType.APPLICATION_JSON_VALUE)
    public IdentityResponse identityAll() {
        log.debug("Incoming GET request to '{}' with element '{}' and id '{}'", PATH_ALL);

        return identityService.getIdentity(null, null);
    }
    
    /**
     * Delivers a JSON description of public keys and endpoints.
     * 
     * @param element Name of element
     * @return {@link IdentityResponse}
     */
    @Operation(summary = "The identity document endpoint delivers a JSON description of public keys and endpoints", 
            description = "The identity document endpoint delivers a JSON description of public keys and endpoints")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Bad Request / Validation errors"),
        @ApiResponse(responseCode = "401", description = "Unauthorized, if no active session is attached"),
        @ApiResponse(responseCode = "404", description = "Not Found"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error"),
    })
    @GetMapping(value = PATH_ELEMENT, produces = MediaType.APPLICATION_JSON_VALUE)
    public IdentityResponse identity(@PathVariable(name = "element", required = true) final String element) {
        log.debug("Incoming GET request to '{}' with element '{}'", PATH_ELEMENT, element);

        return identityService.getIdentity(element, null);
    }

    /**
     * Delivers a JSON description of public keys and endpoints.
     * 
     * @param element Name of element
     * @param id ID of element
     * @return {@link IdentityResponse}
     */
    @Operation(summary = "The identity document endpoint delivers a JSON description of public keys and endpoints", 
            description = "The identity document endpoint delivers a JSON description of public keys and endpoints")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Bad Request / Validation errors"),
        @ApiResponse(responseCode = "401", description = "Unauthorized, if no active session is attached"),
        @ApiResponse(responseCode = "404", description = "Not Found"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error"),
    })
    @GetMapping(value = PATH_ELEMENT_ID, produces = MediaType.APPLICATION_JSON_VALUE)
    public IdentityResponse identity(
            @PathVariable(name = "element", required = true) final String element,
            @PathVariable(name = "id", required = true) final String id) {
        log.debug("Incoming GET request to '{}' with element '{}' and id '{}'", PATH_ELEMENT_ID, element, id);

        return identityService.getIdentity(element, id);
    }
}
