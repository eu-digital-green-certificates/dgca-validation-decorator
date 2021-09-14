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

package eu.europa.ec.dgc.validation.decorator.repository;

import eu.europa.ec.dgc.validation.decorator.config.DgcProperties.ServiceProperties;
import eu.europa.ec.dgc.validation.decorator.dto.DccTokenRequest;
import eu.europa.ec.dgc.validation.decorator.entity.ValidationServiceIdentityResponse;
import eu.europa.ec.dgc.validation.decorator.entity.ValidationServiceInitializeRequest;
import eu.europa.ec.dgc.validation.decorator.entity.ValidationServiceInitializeResponse;
import eu.europa.ec.dgc.validation.decorator.entity.ValidationServiceStatusResponse;
import eu.europa.ec.dgc.validation.decorator.entity.ValidationServiceStatusResponse.Status;
import eu.europa.ec.dgc.validation.decorator.service.AccessTokenService;
import java.util.Base64;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationServiceRepository {

    private static final String PLACEHOLDER_SUBJECT = "{subject}";

    @Value("${validation.urls.identity}")
    private String identityUrl;

    @Value("${validation.urls.initialize}")
    private String initializeUrl;

    @Value("${validation.urls.status}")
    private String statusUrl;

    @Value("${validation.urls.validate}")
    private String validateUrl;

    private final RestTemplate restTpl;

    private final AccessTokenService accessTokenService;

    /**
     * Validation service identity endpoint. Example:
     * https://dgca-validation-service-eu-test.cfapps.eu10.hana.ondemand.com/.
     * 
     * @return {@link ValidationServiceIdentityResponse}
     */
    public ValidationServiceIdentityResponse identity() {
        final String url = this.identityUrl;
        final ResponseEntity<ValidationServiceIdentityResponse> response = restTpl
                .getForEntity(url, ValidationServiceIdentityResponse.class);
        return response.getBody();
    }

    /**
     * Validation service initialize endpoint.
     * 
     * @param service {@link ServiceProperties}
     * @param dccToken {@link DccTokenRequest}
     * @param subject {@link String}
     * @return {@link ValidationServiceInitializeResponse}
     */
    public ValidationServiceInitializeResponse initialize(
            final ServiceProperties service, DccTokenRequest dccToken, String subject) {
        final String url = String.format("%s/%s", service.getServiceEndpoint(), subject);

        final ValidationServiceInitializeRequest body = new ValidationServiceInitializeRequest();
        body.setPubKey(dccToken.getPubKey());
        body.setKeyType("ES256"); // FIXME source?
        body.setNonce(this.buildNonce());
        // TODO add callback

        final HttpHeaders headers = new HttpHeaders();
        headers.add("X-Version", "1.0");
        headers.add("Authorization", accessTokenService.buildHeaderToken(subject));

        final HttpEntity<ValidationServiceInitializeRequest> entity = new HttpEntity<>(body, headers);

        log.debug("REST Call to '{}' starting", url);
        final ResponseEntity<ValidationServiceInitializeResponse> response = restTpl
                .exchange(url, HttpMethod.PUT, entity, ValidationServiceInitializeResponse.class);
        log.debug("REST Call to '{}' done", url);
        return response.getBody();
    }
    
    

    /**
     * Validation service status endpoint.
     * 
     * @param subject {@link String}
     * @return {@link ValidationServiceStatusResponse}
     */
    public ValidationServiceStatusResponse status(String subject) {
        final String url = this.statusUrl.replace(PLACEHOLDER_SUBJECT, subject);

        final HttpHeaders headers = new HttpHeaders();
        headers.add("X-Version", "1.0");
        headers.add("Authorization", accessTokenService.buildHeaderToken(subject));

        final HttpEntity<String> entity = new HttpEntity<>(headers);

        final ResponseEntity<String> response = restTpl.exchange(url, HttpMethod.GET, entity, String.class);
        switch (response.getStatusCode()) {
            case OK:
                return new ValidationServiceStatusResponse(Status.VALID, response.getStatusCodeValue());
            case NO_CONTENT:
                return new ValidationServiceStatusResponse(Status.WAITING, response.getStatusCodeValue());
            default:
                return new ValidationServiceStatusResponse(Status.ERROR, response.getStatusCodeValue());
        }
    }

    /**
     * Validation service validate endpoint.
     * 
     * @param subject {@link String}
     * @return {@link String}
     */
    public String validate(final String subject) {
        final String url = this.initializeUrl.replace(PLACEHOLDER_SUBJECT, subject);

        final HttpHeaders headers = new HttpHeaders();
        headers.add("X-Version", "1.0");
        headers.add("Authorization", accessTokenService.buildHeaderToken(subject));

        // TODO add body DccValidationRequest

        final HttpEntity<String> entity = new HttpEntity<>(headers);

        final ResponseEntity<String> response = restTpl.exchange(url, HttpMethod.POST, entity, String.class);
        return response.getBody();
    }
    
    private String buildNonce() {
        byte[] randomBytes = new byte[16];
        new Random().nextBytes(randomBytes);
        return Base64.getEncoder().encodeToString(randomBytes);
    }
}
