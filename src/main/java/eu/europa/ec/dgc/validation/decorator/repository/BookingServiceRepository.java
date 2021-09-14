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

import eu.europa.ec.dgc.validation.decorator.entity.BookingServiceBoardingPassResponse;
import eu.europa.ec.dgc.validation.decorator.entity.BookingServiceResultRequest;
import eu.europa.ec.dgc.validation.decorator.entity.BookingServiceTokenContentResponse;
import eu.europa.ec.dgc.validation.decorator.service.AccessTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class BookingServiceRepository {

    private static final String PLACEHOLDER_SUBJECT = "{subject}";

    @Value("${booking.urls.boardingPass}")
    private String boardingPassUrl;

    @Value("${booking.urls.tokenContent}")
    private String tokenContentUrl;

    @Value("${booking.urls.result}")
    private String resultUrl;

    private final RestTemplate restTpl;

    private final AccessTokenService accessTokenService;

    /**
     * Booking service boarding pass endpoint.
     * 
     * @param subject {@link String}
     * @return {@link BookingServiceBoardingPassResponse}
     */
    public BookingServiceBoardingPassResponse boardingPass(final String subject) {
        final String url = this.boardingPassUrl.replace(PLACEHOLDER_SUBJECT, subject);

        final HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", accessTokenService.buildHeaderToken(subject));

        final HttpEntity<String> entity = new HttpEntity<>(headers);

        final ResponseEntity<BookingServiceBoardingPassResponse> response = this.restTpl.exchange(url, HttpMethod.GET,
                entity, BookingServiceBoardingPassResponse.class);
        return response.getBody();
    }

    /**
     * Booking service token content endpoint.
     * 
     * @param subject {@link String}
     * @return {@link BookingServiceTokenContentResponse}
     */
    public BookingServiceTokenContentResponse tokenContent(final String subject) {
        final String url = this.tokenContentUrl.replace(PLACEHOLDER_SUBJECT, subject);

        final HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", accessTokenService.buildHeaderToken(subject));

        final HttpEntity<String> entity = new HttpEntity<>(headers);

        final ResponseEntity<BookingServiceTokenContentResponse> response = this.restTpl.exchange(url, HttpMethod.GET,
                entity, BookingServiceTokenContentResponse.class);
        return response.getBody();
    }

    /**
     * Booking service result endpoint.
     * 
     * @param subject {@link String}
     * @param request {@link BookingServiceResultRequest}
     */
    public void result(final String subject, final BookingServiceResultRequest request) {
        final String url = this.resultUrl.replace(PLACEHOLDER_SUBJECT, subject);

        final HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", accessTokenService.buildHeaderToken(subject));

        final HttpEntity<BookingServiceResultRequest> entity = new HttpEntity<>(request, headers);

        this.restTpl.exchange(url, HttpMethod.PUT, entity, String.class);
    }
}
