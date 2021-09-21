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
import eu.europa.ec.dgc.validation.decorator.entity.ServiceResultRequest;
import eu.europa.ec.dgc.validation.decorator.entity.ServiceTokenContentResponse;
import eu.europa.ec.dgc.validation.decorator.service.AccessTokenService;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingBackendRepository implements BackendRepository {

    private static final String PLACEHOLDER_SUBJECT = "{subject}";

    @Value("${booking.urls.tokenContent}")
    private String tokenContentUrl;

    @Value("${booking.urls.result}")
    private String resultUrl;

    private final RestTemplate restTpl;

    private final AccessTokenService accessTokenService;

    /**
     * Booking service token content endpoint.
     * 
     * @param subject {@link String}
     * @return {@link BookingServiceTokenContentResponse}
     */
    @Override
    public ServiceTokenContentResponse tokenContent(final String subject) {
        return this.tokenContent(subject, null);
    }

    /**
     * Booking service token content endpoint.
     * 
     * @param subject {@link String}
     * @param service Used service
     * @return {@link BookingServiceTokenContentResponse}
     */
    @Override
    public ServiceTokenContentResponse tokenContent(final String subject, final ServiceProperties service) {
        final UriComponentsBuilder urlBuilder = UriComponentsBuilder
                .fromUriString(this.tokenContentUrl.replace(PLACEHOLDER_SUBJECT, subject));

        String serviceIdBase64 = null;
        if (service != null && service.getId() != null) {
            log.debug("Receive service ID to booking service '{}'", service.getId());
            serviceIdBase64 = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(service.getId().getBytes(StandardCharsets.UTF_8));
            urlBuilder.queryParam("service", serviceIdBase64);
        }
        final String url = urlBuilder.toUriString();

        final HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", accessTokenService.buildHeaderToken(subject));

        final HttpEntity<String> entity = new HttpEntity<>(headers);

        log.debug("Send service ID (encoded) to booking service '{}'", serviceIdBase64);
        log.debug("REST Call to '{}' starting", url);
        final ResponseEntity<ServiceTokenContentResponse> response = this.restTpl.exchange(url, HttpMethod.GET,
                entity, ServiceTokenContentResponse.class);
        return response.getBody();
    }

    /**
     * Booking service result endpoint.
     * 
     * @param subject {@link String}
     * @param body {@link BookingServiceResultRequest}
     */
    @Override
    public void result(final String subject, final ServiceResultRequest body) {
        if (body.getDccStatus() != null) {
            body.getDccStatus().setSub(subject);
        }

        final String url = "http://localhost:8082/result/{subject}".replace(PLACEHOLDER_SUBJECT, subject);
        //final String url = this.resultUrl.replace(PLACEHOLDER_SUBJECT, subject);

        final HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", accessTokenService.buildHeaderToken(subject));

        final HttpEntity<ServiceResultRequest> entity = new HttpEntity<>(body, headers);

        log.debug("REST Call to '{}' starting", url);
        this.restTpl.exchange(url, HttpMethod.PUT, entity, String.class);
    }
}
