package eu.europa.ec.dgc.validation.decorator.service;

import org.springframework.stereotype.Service;

import eu.europa.ec.dgc.validation.decorator.dto.SubjectDto;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubjectService {

	public SubjectDto generate() {
		// TODO impl
		return SubjectDto.builder()
				.subject("TODO add subject")
				.build();
	}
}
