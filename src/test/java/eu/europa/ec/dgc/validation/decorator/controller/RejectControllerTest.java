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

package eu.europa.ec.dgc.validation.decorator.controller;

import static org.assertj.core.api.Assertions.assertThat;
import eu.europa.ec.dgc.validation.decorator.dto.DccTokenRequest;
import eu.europa.ec.dgc.validation.decorator.service.AccessTokenService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class RejectControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTpl;

    @Autowired
    private AccessTokenService accessTokenService;

    @Test
    void reject_withValidToken_successResponse() {
        // GIVEN
        final String subject = UUID.randomUUID().toString();
        final String token = this.accessTokenService.buildHeaderToken(subject);
        // AND
        final String url = UriComponentsBuilder.fromUriString("http://localhost")
                .port(this.port)
                .path(RejectController.PATH)
                .toUriString();
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", token);
        final HttpEntity<DccTokenRequest> entity = new HttpEntity<>(headers);
        // WHEN
        final ResponseEntity<Void> result = this.restTpl.exchange(url, HttpMethod.GET, entity, Void.class);
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNull();
        // AND header
        assertThat(result.getHeaders()).containsKeys("Cache-Control");
        assertThat(result.getHeaders().get("Cache-Control")).contains("no-cache");        
    }
}
