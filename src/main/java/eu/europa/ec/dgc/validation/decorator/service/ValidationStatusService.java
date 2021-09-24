/*-
 * ---license-start
 * European Digital COVID Certificate Validation Decorator Service / dgca-validation-decorator
 * ---
 * Copyright (C) 2021 T-Systems International GmbH and all other contributors
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package eu.europa.ec.dgc.validation.decorator.service;

import eu.europa.ec.dgc.validation.decorator.config.DgcProperties.ServiceProperties;
import eu.europa.ec.dgc.validation.decorator.dto.ResultToken;
import eu.europa.ec.dgc.validation.decorator.dto.ResultToken.Result;
import eu.europa.ec.dgc.validation.decorator.entity.KeyUse;
import eu.europa.ec.dgc.validation.decorator.entity.ServiceResultRequest;
import eu.europa.ec.dgc.validation.decorator.entity.ServiceTokenContentResponse;
import eu.europa.ec.dgc.validation.decorator.entity.ServiceTokenContentResponse.SubjectResponse;
import eu.europa.ec.dgc.validation.decorator.entity.ValidationServiceIdentityResponse;
import eu.europa.ec.dgc.validation.decorator.entity.ValidationServiceIdentityResponse.PublicKeyJwk;
import eu.europa.ec.dgc.validation.decorator.entity.ValidationServiceStatusResponse;
import eu.europa.ec.dgc.validation.decorator.exception.DccException;
import eu.europa.ec.dgc.validation.decorator.exception.NotFoundException;
import eu.europa.ec.dgc.validation.decorator.exception.RepositoryException;
import eu.europa.ec.dgc.validation.decorator.exception.UncheckedCertificateException;
import eu.europa.ec.dgc.validation.decorator.repository.BackendRepository;
import eu.europa.ec.dgc.validation.decorator.repository.ValidationServiceRepository;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwt;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationStatusService {

    private final BackendRepository backendRepository;

    private final IdentityService identityService;

    private final ValidationServiceRepository validationServiceRepository;

    private final AccessTokenService accessTokenService;

    private final ConversionService converter;

    /**
     * Determines the status of the validation service.
     * 
     * @param subject Subject ID
     * @return {@link ValidationServiceStatusResponse}
     */
    public ValidationServiceStatusResponse determineStatus(final String subject) {
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

        final ValidationServiceStatusResponse status = this.getValidationServiceStatus(subject, service);
        log.debug("Receive validation service response (status code): {}", status.getHttpStatusCode());

        if (status.getHttpStatusCode() == HttpStatus.OK.value() && StringUtils.hasText(status.getJwt())) {
            final Map<String, Object> jwtContent = this.getJwtContent(service, status);
            final ResultToken resultToken = this.buildResultToken(jwtContent);
            status.setResultToken(resultToken);

            // Send result to backend service
            try {
                final ServiceResultRequest request = this.converter.convert(resultToken, ServiceResultRequest.class);
                this.backendRepository.result(subject, request);
            } catch (HttpClientErrorException e) {
                log.error(e.getMessage(), e);
                throw new RepositoryException("Backend service http client error", e);
            }
        }
        return status;
    }

    @SuppressWarnings("unchecked")
    private ResultToken buildResultToken(final Map<String, Object> jwtContent) {
        final ResultToken resultToken = new ResultToken();
        if (jwtContent.containsKey("result")) {
            resultToken.setResult((String) jwtContent.get("result"));
        }
        if (jwtContent.containsKey("confirmation")) {
            resultToken.setConfirmation((String) jwtContent.get("confirmation"));
        }
        if (jwtContent.containsKey("iss")) {
            resultToken.setIssuer((String) jwtContent.get("iss"));
        }
        if (jwtContent.containsKey("iat")) {
            resultToken.setIat(((Integer) jwtContent.get("iat")).longValue());
        }
        if (jwtContent.containsKey("results")) {
            final List<Map<String, Object>> jwtResults = (List<Map<String, Object>>) jwtContent.get("results");
            final List<Result> results = jwtResults.stream().map(jwtRes -> {
                final Result res = new Result();
                if (jwtRes.containsKey("identifier")) {
                    res.setIdentifier((String) jwtRes.get("identifier"));
                }
                if (jwtRes.containsKey("result")) {
                    res.setResult((String) jwtRes.get("result"));
                }
                if (jwtRes.containsKey("type")) {
                    res.setType((String) jwtRes.get("type"));
                }
                if (jwtRes.containsKey("details")) {
                    res.setDetails((String) jwtRes.get("details"));
                }
                return res;
            }).collect(Collectors.toList());
            resultToken.setResults(results);
        }
        return resultToken;
    }

    private Map<String, Object> getJwtContent(final ServiceProperties service,
            final ValidationServiceStatusResponse status) {
        if (status.getHttpStatusCode() == HttpStatus.OK.value() && StringUtils.hasText(status.getJwt())) {
            final Jwt jwt = this.accessTokenService.parseUnsecure(status.getJwt());
            final Header jwtHeaders = jwt.getHeader();
            if (jwtHeaders.containsKey("kid") && jwtHeaders.get("kid") instanceof String) {
                final String keyId = (String) jwtHeaders.get("kid");

                final PublicKey vsPublicKey = this.getSignPublicKey(service, keyId);
                return this.accessTokenService.parseAccessToken(status.getJwt(), vsPublicKey);
            } else {
                throw new NotFoundException("Status JWT has no key ID");
            }
        } else {
            throw new NotFoundException("Status response has no JWT");
        }
    }

    private PublicKey getSignPublicKey(final ServiceProperties service, final String keyId) {
        final ValidationServiceIdentityResponse identity = this.validationServiceRepository.identity(service);
        return identity.getVerificationMethod().stream()
                .filter(vm -> vm.getPublicKeyJwk() != null)
                .filter(vm -> KeyUse.SIG.name().equalsIgnoreCase(vm.getPublicKeyJwk().getUse()))
                .filter(vm -> keyId.equalsIgnoreCase(vm.getPublicKeyJwk().getKid()))
                .map(vm -> this.toPublicKey(vm.getPublicKeyJwk()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        String.format("Validation service method with ID '%s' not found", keyId)));
    }

    private PublicKey toPublicKey(final PublicKeyJwk publicKeyJwk) {
        final byte[] encoded = Base64.getDecoder().decode(publicKeyJwk.getX5c());
        try (ByteArrayInputStream encStream = new ByteArrayInputStream(encoded)) {
            return CertificateFactory.getInstance("X.509").generateCertificate(encStream).getPublicKey();
        } catch (CertificateException e) {
            throw new UncheckedCertificateException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ServiceTokenContentResponse getBackendTokenContent(final String subject) {
        try {
            return this.backendRepository.tokenContent(subject);
        } catch (HttpClientErrorException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryException("Backend service http client error", e);
        }
    }

    private ValidationServiceStatusResponse getValidationServiceStatus(final String subject,
            final ServiceProperties service) {
        try {
            return this.validationServiceRepository.status(service, subject);
        } catch (HttpClientErrorException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryException("Validation service http client error", e);
        }
    }
}
