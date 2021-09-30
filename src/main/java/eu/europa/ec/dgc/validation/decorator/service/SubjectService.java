package eu.europa.ec.dgc.validation.decorator.service;

import eu.europa.ec.dgc.validation.decorator.config.DgcProperties.ServiceProperties;
import eu.europa.ec.dgc.validation.decorator.entity.ServiceTokenContentResponse;
import eu.europa.ec.dgc.validation.decorator.entity.ServiceTokenContentResponse.SubjectResponse;
import eu.europa.ec.dgc.validation.decorator.exception.DccException;
import eu.europa.ec.dgc.validation.decorator.exception.RepositoryException;
import eu.europa.ec.dgc.validation.decorator.repository.BackendRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubjectService {

    private final IdentityService identityService;
    
    private final BackendRepository backendRepository;
    
    public ServiceProperties getServiceBySubject(String subject) {
        final ServiceTokenContentResponse tokenContent = this.getBackendTokenContent(subject);
        if (tokenContent != null && tokenContent.getSubjects() == null || tokenContent.getSubjects().isEmpty()) {
            throw new DccException("Subject not found in token", HttpStatus.NO_CONTENT.value());
        }

        final SubjectResponse subjectResponse = tokenContent.getSubjects().get(0);
        final String serviceId = subjectResponse.getServiceIdUsed();
        log.debug("Receive service ID (encoded) from booking service '{}'", serviceId);
        if (serviceId == null || serviceId.isBlank()) {
            throw new DccException(String.format("Subject without service ID '%s'", serviceId),
                    HttpStatus.NO_CONTENT.value());
        }
        
        final String decodedServiceId = new String(Base64.getUrlDecoder().decode(serviceId), StandardCharsets.UTF_8);
        log.debug("Receive service ID (decoded) from booking service '{}'", decodedServiceId);

        final ServiceProperties service = this.identityService.getServicePropertiesById(decodedServiceId);
        log.debug("Receive service: {}", service);
        return service;
    }
    
    private ServiceTokenContentResponse getBackendTokenContent(final String subject) {
        try {
            return this.backendRepository.tokenContent(subject);
        } catch (HttpClientErrorException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryException("Backend service http client error", e);
        }
    }
}
