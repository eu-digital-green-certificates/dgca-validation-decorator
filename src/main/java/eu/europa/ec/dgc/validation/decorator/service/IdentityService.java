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

import eu.europa.ec.dgc.validation.decorator.config.DgcProperties;
import eu.europa.ec.dgc.validation.decorator.config.DgcProperties.ServiceProperties;
import eu.europa.ec.dgc.validation.decorator.controller.IdentityController;
import eu.europa.ec.dgc.validation.decorator.dto.IdentityResponse;
import eu.europa.ec.dgc.validation.decorator.dto.IdentityResponse.PublicKeyJwkIdentityResponse;
import eu.europa.ec.dgc.validation.decorator.dto.IdentityResponse.ServiceIdentityResponse;
import eu.europa.ec.dgc.validation.decorator.dto.IdentityResponse.VerificationIdentityResponse;
import eu.europa.ec.dgc.validation.decorator.entity.KeyType;
import eu.europa.ec.dgc.validation.decorator.exception.DccException;
import eu.europa.ec.dgc.validation.decorator.exception.NotFoundException;
import io.vavr.collection.Stream;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdentityService {

    private static final String VERIFICATION_TYPE = "JsonWebKey2020";
    
    private static final String IDENTITY_PATH = "/identity/verificationMethod" + VERIFICATION_TYPE;

    private final DgcProperties dgcProperties;

    private final KeyProvider keyProvider;

    /**
     * Create identity Object with given informations.
     * 
     * @param element Element
     * @param id ID
     * @return {@link IdentityResponse}
     */
    public IdentityResponse getIdentity(final String element, final String id) {
        // TODO impl filter for id

        final String identityId = String.format("%s%s", dgcProperties.getServiceUrl(), IDENTITY_PATH);

        final List<VerificationIdentityResponse> verificationMethods = keyProvider.getKeyNames(KeyType.ALL).stream()
                .filter(keyName -> element == null || element.equalsIgnoreCase(keyName))
                .map(keyName -> {
                    final VerificationIdentityResponse verificationMethod = new VerificationIdentityResponse();
                    verificationMethod.setId(String.format("%s/%s", identityId, keyName));
                    verificationMethod.setController(identityId);
                    verificationMethod.setType(VERIFICATION_TYPE);
                    verificationMethod.setPublicKeyJwk(buildPublicKey(keyName));
                    return verificationMethod;
                }).collect(Collectors.toList());

        final IdentityResponse identityResponse = new IdentityResponse();
        identityResponse.setId(identityId);
        identityResponse.setVerificationMethod(verificationMethods);
        identityResponse.setService(getServices(element, id));
        return identityResponse;
    }

    /**
     * Delivers the service based on the id.
     * 
     * @param serviceId Service ID
     * @return {@link ServiceProperties}
     */
    public ServiceProperties getServicePropertiesById(final String serviceId) {
        if (serviceId != null && dgcProperties.getServices() != null) {
            return dgcProperties.getServices().stream()
                    .filter(service -> serviceId.equals(service.getId()))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException(String.format("Service not found by ID '%s'", serviceId)));
        }
        throw new NotFoundException("Verification method not found. No ID available.");
    }

    private List<ServiceIdentityResponse> getServices(final String element, final String id) {
        // TODO impl filter for id
        return Stream.concat(dgcProperties.getServices(), dgcProperties.getEndpoints())
                .filter(service -> element == null || element.equalsIgnoreCase(service.getType()))
                .map(service -> {
                    final ServiceIdentityResponse response = new ServiceIdentityResponse();
                    response.setId(service.getId());
                    response.setType(service.getType());
                    response.setServiceEndpoint(service.getServiceEndpoint());
                    response.setName(service.getName());
                    return response;
                }).collect(Collectors.toList());
    }

    private PublicKeyJwkIdentityResponse buildPublicKey(String keyName) {
        final Certificate certificate = keyProvider.receiveCertificate(keyName);        
        try {
            final PublicKeyJwkIdentityResponse publicKeyJwk = new PublicKeyJwkIdentityResponse();
            publicKeyJwk.setX5c(Base64.getEncoder().encodeToString(certificate.getEncoded()));
            publicKeyJwk.setKid(keyProvider.getKid(keyName));
            publicKeyJwk.setAlg(keyProvider.getAlg(keyName));
            publicKeyJwk.setUse(keyProvider.getKeyUse(keyName).name().toLowerCase());
            return publicKeyJwk;
        } catch (CertificateEncodingException e) {
            throw new DccException("Can not encode certificate", e);
        }
    }
}
