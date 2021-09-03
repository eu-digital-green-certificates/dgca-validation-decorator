package eu.europa.ec.dgc.validation.decorator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import javax.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eu.europa.ec.dgc.validation.decorator.dto.DccTokenRequest;

@RestController
@RequestMapping("/")
public class AccessTokenController {
	
    @Operation(
    		summary = "TODO",
            description = "TODO"
    )
    @ApiResponses(value = {
    		@ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Unauthorized, if no access token was provided")
    })
    @PostMapping(value = "/token", consumes = "application/jwt")
    public void token(
    		@RequestHeader("Authorization") String accessToken, 
    		@Valid @RequestBody DccTokenRequest request) {
    	// TODO impl
    }
}
