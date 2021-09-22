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
import eu.europa.ec.dgc.validation.decorator.dto.IdentityResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class IdentityControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTpl;

    @Test
    void identityAll_withoutVariales_successWithAllIdentities() {
        // GIVEN
        final String url = UriComponentsBuilder.fromUriString("http://localhost")
                .port(port)
                .path(IdentityController.PATH_ALL)
                .toUriString();
        // WHEN
        final IdentityResponse result = restTpl.getForObject(url, IdentityResponse.class);
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getVerificationMethod()).hasSizeGreaterThanOrEqualTo(4);
        assertThat(result.getService()).hasSizeGreaterThanOrEqualTo(4);
    }

    @ParameterizedTest
    @ValueSource(strings = { "verificationMethod", "service" })
    void identityElement_withElement_successWithElement(final String element) {
        // GIVEN 
        final String url = UriComponentsBuilder.fromUriString("http://localhost")
                .port(port)
                .path(IdentityController.PATH_ELEMENT.replace("{element}", element))
                .toUriString();
        // WHEN 
        final IdentityResponse result = restTpl.getForObject(url, IdentityResponse.class);
        // THEN
        assertThat(result).isNotNull();
        // AND
        if ("verificationMethod".equals(element)) {
            assertThat(result.getVerificationMethod()).hasSizeGreaterThanOrEqualTo(4);
            assertThat(result.getService()).hasSize(0);
        } else {
            assertThat(result.getVerificationMethod()).hasSize(0);
            assertThat(result.getService()).hasSizeGreaterThanOrEqualTo(4);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "ValidationService", "AccessTokenService", "ServiceProvider", "CancellationService",
        "StatusService" })
    void identityType_withServiceAndType_successWithServiceAndType(final String type) {
        // GIVEN
        String element = "service";
        final String url = UriComponentsBuilder.fromUriString("http://localhost")
                .port(port)
                .path(IdentityController.PATH_ELEMENT_TYPE.replace("{element}", element).replace("{type}", type))
                .toUriString();
        // WHEN
        final IdentityResponse result = restTpl.getForObject(url, IdentityResponse.class);
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getVerificationMethod()).hasSize(0);
        assertThat(result.getService()).hasSize(1);
    }
}
