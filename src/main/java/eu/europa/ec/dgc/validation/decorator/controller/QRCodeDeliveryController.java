package eu.europa.ec.dgc.validation.decorator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import eu.europa.ec.dgc.validation.decorator.config.QRCodeProperties;
import eu.europa.ec.dgc.validation.decorator.dto.QRCodeDto;
import eu.europa.ec.dgc.validation.decorator.dto.SubjectDto;
import eu.europa.ec.dgc.validation.decorator.service.SubjectService;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class QRCodeDeliveryController {
	
	private final QRCodeProperties properties;
	
	private final SubjectService subjectService;
	
    @Operation(
    		summary = "The provision endpoint is the public endpoint for receiving the initialization QR Code",
            description = "The provision endpoint is the public endpoint for receiving the initialization QR Code"
    )
    @ApiResponses(value = {
    		@ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Unauthorized, if no active session is attached")
    })
    // TODO produces image ???
    @GetMapping(value = "/initialize", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public QRCodeDto initialize(
    		@RequestParam(name = "session", required = true) String session) {
    	
    	SubjectDto subject = subjectService.generate();
    	// TODO generate QR code
		return QRCodeDto.builder()
    			.protocol(properties.getProtocol())
    			.protocolVersion(properties.getProtocolVersion())
    			.subject(subject.getSubject())
    			.build();
    	/**
    	 * Example QR Code content:
    	 * {"protocol":"DCCVALIDATION",
    	 * "protocolVersion":"1.0.0",
    	 * "serviceidentity":"http://dccexampleprovider/dcc/identity",
    	 * "validationidentity":"http://validation.dccexampleprovider/dcc/identity",
    	 * "token":"eyJhbGciOiJFUzI1NiIsImtpZCI6IjIzNDMifQ.eyJpc3MiOiJodHRwczovL3NlcnZpY2Vwcm92aWRlciIsInN1YiI6IkFERURERERERERERERERERERCIsImlhdCI6MTUxNjIzOTAyMiwiZXhwIjoxNTE2MjM5MDIyfQ.Gdnw1LF1BccpYKcTAuyNL_KeY1Z0juz9WU9660BedgRzrZplxUZRjr09JIlZNtgZtqgAZ9Pma3kgPhUln9Gufw",
    	 * "consent":"I want to check your DCC to confirm your booking!:)",
    	 * “subject”:”Booking Nr. …”,
    	 * “serviceprovider” :”Service Provider.com”}
    	 */
    }
}
