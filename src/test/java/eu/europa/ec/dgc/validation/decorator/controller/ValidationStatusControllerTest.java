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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import eu.europa.ec.dgc.validation.decorator.dto.DccTokenRequest;
import eu.europa.ec.dgc.validation.decorator.dto.IdentityResponse.ServiceIdentityResponse;
import eu.europa.ec.dgc.validation.decorator.dto.ResultToken;
import eu.europa.ec.dgc.validation.decorator.entity.ServiceTokenContentResponse;
import eu.europa.ec.dgc.validation.decorator.entity.ValidationServiceStatusResponse;
import eu.europa.ec.dgc.validation.decorator.repository.BackendRepository;
import eu.europa.ec.dgc.validation.decorator.repository.ValidationServiceRepository;
import eu.europa.ec.dgc.validation.decorator.service.AccessTokenService;
import eu.europa.ec.dgc.validation.decorator.service.IdentityService;
import eu.europa.ec.dgc.validation.decorator.util.TestHelper;
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
public class ValidationStatusControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTpl;

    @Autowired
    private AccessTokenService accessTokenService;

    @Autowired
    private IdentityService identityService;

    @MockBean
    private BackendRepository backendRepositoryMock;

    @MockBean
    private ValidationServiceRepository validationServiceRepositoryMock;

    private String subject;

    private ServiceIdentityResponse service;

    @BeforeEach
    public void before() {
        this.subject = UUID.randomUUID().toString();
        this.service = this.identityService.getIdentity("service", "ValidationService").getService().get(0);

        final ServiceTokenContentResponse tokenContent = TestHelper.buildServiceTokenContent(
                this.subject, this.service);
        when(this.backendRepositoryMock.tokenContent(any())).thenReturn(tokenContent);
        when(this.backendRepositoryMock.tokenContent(any(), any())).thenReturn(tokenContent);

        when(this.validationServiceRepositoryMock.identity(any())).thenReturn(TestHelper.buildValidationServiceIdentity());
    }

    @Test
    void status_withValidTokenAndSubject_noContentResponse() {
        // GIVEN 
        final String subject = UUID.randomUUID().toString();
        final String token = this.accessTokenService.buildHeaderToken(subject);
        // AND 
        final String url = UriComponentsBuilder.fromUriString("http://localhost")
                .port(this.port)
                .path(ValidationStatusController.PATH)
                .toUriString();
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", token);
        final HttpEntity<DccTokenRequest> entity = new HttpEntity<>(headers);
        // AND
        final int reponseHttpStatusCode = HttpStatus.NO_CONTENT.value();
        when(this.validationServiceRepositoryMock.status(any(), any()))
                .thenReturn(this.buildValidationServiceStatus(reponseHttpStatusCode));
        // WHEN
        final ResponseEntity<ResultToken> result = this.restTpl.exchange(url, HttpMethod.GET, entity,
                ResultToken.class);
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.getBody()).isNull();
        // AND header 
        assertThat(result.getHeaders()).containsKeys("Cache-Control");
        assertThat(result.getHeaders().get("Cache-Control")).contains("no-cache");
    }

    @Test
    void status_withValidTokenAndSubject_successResponseWithResultNok() {
        // GIVEN 
        final String subject = "d0cebabe-3e23-4e54-8b28-5d557168aa1b"; // Subject from JWT
        final String token = this.accessTokenService.buildHeaderToken(subject);
        // AND 
        final String url = UriComponentsBuilder.fromUriString("http://localhost")
                .port(this.port)
                .path(ValidationStatusController.PATH)
                .toUriString();
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", token);
        final HttpEntity<DccTokenRequest> entity = new HttpEntity<>(headers);
        // AND
        final String jwt = getJwtSuccessResponseWithResultNok();
        final int reponseHttpStatusCode = HttpStatus.OK.value();
        when(this.validationServiceRepositoryMock.status(any(), any()))
                .thenReturn(this.buildValidationServiceStatus(reponseHttpStatusCode, jwt));
        // WHEN
        final ResponseEntity<ResultToken> result = this.restTpl.exchange(url, HttpMethod.GET, entity,
                ResultToken.class);
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        // AND header 
        assertThat(result.getHeaders()).containsKeys("Cache-Control");
        assertThat(result.getHeaders().get("Cache-Control")).contains("no-cache");
        // AND body / ResultToken
        final ResultToken resultToken = result.getBody();
        assertThat(resultToken).isNotNull();
        assertThat(resultToken.getResult()).isEqualTo("NOK");
        assertThat(resultToken.getConfirmation()).isNotBlank();        
        // AND ResultToken (JsonIgnore)
        assertThat(resultToken.getIssuer()).isNull();
        assertThat(resultToken.getIat()).isNull();
        // AND ResultToken Results
        assertThat(resultToken.getResults()).hasSize(2);
        resultToken.getResults().forEach(res -> {
            assertThat(res).isNotNull();
            assertThat(res.getIdentifier()).isNotNull();
            assertThat(res.getResult()).isEqualTo("NOK");
            assertThat(res.getType()).isEqualTo("TechnicalVerification");
            assertThat(res.getDetails()).contains("name does not match");
        });
    }

    private ValidationServiceStatusResponse buildValidationServiceStatus(final int httpStatusCode) {
        return this.buildValidationServiceStatus(httpStatusCode, null);
    }

    private ValidationServiceStatusResponse buildValidationServiceStatus(final int httpStatusCode, final String jwt) {
        return new ValidationServiceStatusResponse(httpStatusCode, jwt);
    }

    private String getJwtSuccessResponseWithResultNok() {
        return "eyJ0eXAiOiJKV1QiLCJraWQiOiJNRmt3RXdZSEt1Kz0iLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiIyMWIwZj"
                + "U0YS1jNDU1LTQ2YjctOGUwYy05YTgxZTkyOWM4ZDUiLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwOD"
                + "IiLCJpYXQiOjE2MzIzODI2NTQsImNvbmZpcm1hdGlvbiI6ImV5SnJhV1FpT2lKTlJtdDNSWGRaU0V0MU"
                + "t6MGlMQ0poYkdjaU9pSkZVekkxTmlKOS5leUpxZEdraU9pSTVZek5rWW1JNE55MDBZek16TFRSbFkyVX"
                + "RPV0ppTVMxa1lXRmlNRGhrT1dFeE1EQWlMQ0p6ZFdJaU9pSXlNV0l3WmpVMFlTMWpORFUxTFRRMllqY3"
                + "RPR1V3WXkwNVlUZ3haVGt5T1dNNFpEVWlMQ0pwWVhRaU9qRTJNekl6T0RJMk5UUXNJbkpsYzNWc2RDST"
                + "ZJazVQU3lKOS5CaE95bnZJNmFHTllWUmEzYlJHU0dZNkdMQjFnWmVJdDRQeXV4OFlUTENrTWRfc0VsaT"
                + "RUbmFiRExoTFVieGs0ZXVpQS1GSkFKbWg4bkVMRTZsV0hNUSIsInJlc3VsdHMiOlt7ImlkZW50aWZpZX"
                + "IiOiJGTlROT01BVENIIiwicmVzdWx0IjoiTk9LIiwidHlwZSI6IlRlY2huaWNhbFZlcmlmaWNhdGlvbi"
                + "IsImRldGFpbHMiOiJmYW1pbHkgbmFtZSBkb2VzIG5vdCBtYXRjaCJ9LHsiaWRlbnRpZmllciI6IkdOVE"
                + "5PVE1BVENIIiwicmVzdWx0IjoiTk9LIiwidHlwZSI6IlRlY2huaWNhbFZlcmlmaWNhdGlvbiIsImRldG"
                + "FpbHMiOiJnaXZlbiBuYW1lIGRvZXMgbm90IG1hdGNoIn1dLCJyZXN1bHQiOiJOT0sifQ.uomqmF12Ezq"
                + "OCLcfApE6AhCIF_zQ9iw_O0UdHGMfX6vIE77-28eisOm2T2ujQrPcLBTfRvoJJFznqg5JcCR2qw";
    }
}
