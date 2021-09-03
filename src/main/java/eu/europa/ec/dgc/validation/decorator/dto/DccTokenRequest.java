package eu.europa.ec.dgc.validation.decorator.dto;

import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class DccTokenRequest {

	@NotBlank
	private String service;
	
	@NotBlank
	private String pubKey; 
}
