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
public class RejectCallbackController {

	@Operation(
            summary = "TODO",
            description = "TODO"
    )
	@ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Unauthorized, if no access token are provided")
    })
	@GetMapping(value = "/reject", consumes = MediaType.APPLICATION_JSON_VALUE)
	public void reject(
			@RequestHeader("Authorization") String accessToken) {
		// TODO impl
	}
}
