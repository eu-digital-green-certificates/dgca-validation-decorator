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
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import eu.europa.ec.dgc.validation.decorator.dto.DccTokenRequest;
import eu.europa.ec.dgc.validation.decorator.dto.IdentityResponse.ServiceIdentityResponse;
import eu.europa.ec.dgc.validation.decorator.entity.ServiceTokenContentResponse;
import eu.europa.ec.dgc.validation.decorator.entity.ValidationServiceInitializeResponse;
import eu.europa.ec.dgc.validation.decorator.repository.BackendRepository;
import eu.europa.ec.dgc.validation.decorator.repository.ValidationServiceRepository;
import eu.europa.ec.dgc.validation.decorator.service.AccessTokenService;
import eu.europa.ec.dgc.validation.decorator.service.IdentityService;
import eu.europa.ec.dgc.validation.decorator.util.TestHelper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class DccTokenControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTpl;

    @Autowired
    private AccessTokenService accessTokenService;

    @Autowired
    private IdentityService identityService;

    @MockBean
    private ValidationServiceRepository validationServiceRepositoryMock;

    @MockBean
    private BackendRepository backendRepository;

    private String subject;

    private ServiceIdentityResponse service;

    @BeforeEach
    public void before() {
        this.subject = UUID.randomUUID().toString();
        this.service = this.identityService.getIdentity("service", "ValidationService").getService().get(0);

        final ValidationServiceInitializeResponse initialize = this.buildValidationServiceInitialize();
        when(this.validationServiceRepositoryMock.initialize(any(), any(), any(), any())).thenReturn(initialize);

        final ServiceTokenContentResponse tokenContent = TestHelper.buildServiceTokenContent(
                this.subject, this.service);
        when(this.backendRepository.tokenContent(any())).thenReturn(tokenContent);
        when(this.backendRepository.tokenContent(any(), any())).thenReturn(tokenContent);
    }

    @SuppressWarnings("unchecked")
    @Test
    void token_withValidTokenAndService_successResponse() {
        // GIVEN
        final String token = this.accessTokenService.buildHeaderToken(this.subject);
        final DccTokenRequest body = new DccTokenRequest();
        body.setService(this.service.getId());
        body.setPubKey(UUID.randomUUID().toString());
        // AND 
        final String url = UriComponentsBuilder.fromUriString("http://localhost")
                .port(port)
                .path(DccTokenController.PATH.replace("{subject}", this.subject))
                .toUriString();
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", token);
        final HttpEntity<DccTokenRequest> entity = new HttpEntity<>(body, headers);
        // WHEN       
        ResponseEntity<String> result = this.restTpl.exchange(url, HttpMethod.POST, entity, String.class);
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotBlank();
        assertThat(this.accessTokenService.isValid(result.getBody())).isTrue();
        // AND header 
        assertThat(result.getHeaders()).containsKeys("X-Nonce", "Cache-Control");
        assertThat(result.getHeaders().get("X-Nonce")).isNotNull().isNotEmpty();
        assertThat(result.getHeaders().get("Cache-Control")).contains("no-cache");
        // AND tokenContent
        final Map<String, Object> tokenContent = accessTokenService.parseAccessToken(result.getBody());
        assertThat(tokenContent)
                .containsKeys("sub", "aud", "t", "v", "iss", "exp", "iat", "vc", "jti")
                .contains(entry("sub", this.subject));
        // AND occurrenceInfo
        assertThat(tokenContent.get("vc")).isNotNull().isInstanceOf(Map.class);
        final Map<String, Object> occurrenceInfo = (Map<String, Object>) tokenContent.get("vc");
        assertThat(occurrenceInfo).containsKeys("lang", "fnt", "gnt", "dob", "coa", "cod", "roa", "type", "category",
                "validationClock", "validFrom", "validTo");
    }

    private ValidationServiceInitializeResponse buildValidationServiceInitialize() {
        final ValidationServiceInitializeResponse initialize = new ValidationServiceInitializeResponse();
        initialize.setSubject(this.subject);
        initialize.setExp(Instant.now().plusSeconds(60).toEpochMilli());
        initialize.setAud("ValidationServiceInitializeResponse");
        return initialize;
    }
}
