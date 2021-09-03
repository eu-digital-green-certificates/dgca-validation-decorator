package eu.europa.ec.dgc.validation.decorator.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/")
public class ValidationStatusController {

	@Operation(
            summary = "The validation status endpoint provides the validation result of a subject",
            description = "The validation status endpoint provides the validation result of a subject"
    )
	@ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "204", description = "No content, wait for status"),
            @ApiResponse(responseCode = "401", description = "Unauthorized, if no access token are provided"),
            @ApiResponse(responseCode = "410", description = "Gone. Subject does not exist anymore")
    })
	@GetMapping(value = "/status", consumes = MediaType.APPLICATION_JSON_VALUE)
	public void reject(
			@RequestHeader("Authorization") String accessToken) {
		// TODO impl
	}
}
