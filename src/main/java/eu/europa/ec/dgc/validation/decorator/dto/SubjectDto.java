package eu.europa.ec.dgc.validation.decorator.dto;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class SubjectDto {

	@NonNull
	private String subject;
}
