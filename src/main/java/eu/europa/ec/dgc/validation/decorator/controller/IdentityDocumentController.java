package eu.europa.ec.dgc.validation.decorator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class IdentityDocumentController {
	
    @Operation(
    		summary = "The identity document endpoint delivers a json description of public keys and service endpoints",
            description = "The identity document endpoint delivers a json description of public keys and service endpoints"
    )
    @ApiResponses(value = {
    		@ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @GetMapping(value = "/identity/{rootElement}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void identity() {
    	// TODO search identity
    }
}
