package eu.europa.ec.dgc.validation.decorator.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/")
public class CallbackStatusController {

	@Operation(
            summary = "The optional callback endpoint receives the validation result to a subject",
            description = "The optional callback endpoint receives the validation result to a subject"
    )
	@ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Unauthorized, if Result Token was not correctly signed by validation service"),
            @ApiResponse(responseCode = "410", description = "Gone. Subject does not exist anymore")
    })
	@PutMapping(value = "/callback/{subject}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public void callback() {
		// TODO impl
	}
}
