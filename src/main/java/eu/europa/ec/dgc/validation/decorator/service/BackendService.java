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

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.dgc.validation.decorator.config.DgcProperties.ServiceProperties;
import eu.europa.ec.dgc.validation.decorator.dto.CallbackRequest;
import eu.europa.ec.dgc.validation.decorator.entity.KeyUse;
import eu.europa.ec.dgc.validation.decorator.entity.ServiceResultRequest;
import eu.europa.ec.dgc.validation.decorator.entity.ValidationServiceIdentityResponse;
import eu.europa.ec.dgc.validation.decorator.entity.ValidationServiceIdentityResponse.PublicKeyJwk;
import eu.europa.ec.dgc.validation.decorator.exception.NotFoundException;
import eu.europa.ec.dgc.validation.decorator.exception.UncheckedCertificateException;
import eu.europa.ec.dgc.validation.decorator.repository.BackendRepository;
import eu.europa.ec.dgc.validation.decorator.repository.ValidationServiceRepository;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwt;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BackendService {

    private final BackendRepository backendRepository;

    private final ConversionService converter;

    private final AccessTokenService accessTokenService;

    private final ValidationServiceRepository validationServiceRepository;

    private final ObjectMapper mapper;

    private final SubjectService subjectService;

    /**
     * Reads the content from the JWT and converts it into {@link CallbackRequest}.
     * 
     * @param subject Subject ID
     * @param body JWT
     * @return {@link CallbackRequest}
     */
    public CallbackRequest parseRequest(String subject, String body) {
        final ServiceProperties service = this.subjectService.getServiceBySubject(subject);
        final Map<String, Object> jwtContent = this.getJwtContent(service, body);
        return this.mapper.convertValue(jwtContent, CallbackRequest.class);
    }

    /**
     * Save result in booking service.
     * 
     * @param subject {@link String}
     * @param request {@link CallbackRequest}
     */
    public void saveResult(final String subject, final CallbackRequest request) {
        final ServiceResultRequest resultRequest = this.converter.convert(request, ServiceResultRequest.class);
        this.backendRepository.result(subject, resultRequest);
    }

    private Map<String, Object> getJwtContent(final ServiceProperties service, final String token) {
        final Jwt jwt = this.accessTokenService.parseUnsecure(token);
        final Header jwtHeaders = jwt.getHeader();
        if (jwtHeaders.containsKey("kid") && jwtHeaders.get("kid") instanceof String) {
            final String keyId = (String) jwtHeaders.get("kid");

            final PublicKey vsPublicKey = this.getSignPublicKey(service, keyId);
            return this.accessTokenService.parseAccessToken(token, vsPublicKey);
        } else {
            throw new NotFoundException("Status JWT has no key ID");
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
}
