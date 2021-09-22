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
import eu.europa.ec.dgc.validation.decorator.config.IdentityProperties;
import eu.europa.ec.dgc.validation.decorator.dto.QrCodeDto;
import eu.europa.ec.dgc.validation.decorator.service.AccessTokenService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class InitializeControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTpl;
    
    @Autowired
    private IdentityProperties properties;
    
    @Autowired
    private AccessTokenService accessTokenService;
    
    @Test
    void initialize_withRandomSubject_successQrCode() {
        // GIVEN
        final String subject = UUID.randomUUID().toString();
        final String url = UriComponentsBuilder.fromUriString("http://localhost")
                .port(port)
                .path(InitializeController.PATH.replace("{subject}", subject))
                .toUriString();
        // WHEN
        final QrCodeDto result = restTpl.getForObject(url, QrCodeDto.class);
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getProtocol()).isEqualTo(this.properties.getProtocol());
        assertThat(result.getProtocolVersion()).isEqualTo(this.properties.getProtocolVersion());
        assertThat(result.getServiceIdentity()).isEqualTo(this.properties.getServiceIdentityUrl());
        assertThat(result.getPrivacyUrl()).isEqualTo(this.properties.getPrivacyUrl());
        assertThat(result.getConsent()).isEqualTo(this.properties.getConsent());
        assertThat(result.getSubject()).isEqualTo(subject);
        assertThat(result.getServiceProvider()).isEqualTo(this.properties.getServiceProvider());
        // AND token
        assertThat(result.getToken()).isNotBlank();
        assertThat(accessTokenService.isValid(result.getToken())).isTrue();
    }
}
