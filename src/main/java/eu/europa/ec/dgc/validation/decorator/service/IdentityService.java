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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdentityService {

    private static final String VERIFICATION_TYPE = "JsonWebKey2020";

    private static final String IDENTITY_ROOT = "/identity";

    private static final String IDENTITY_PATH = IDENTITY_ROOT + "/verificationMethod/" + VERIFICATION_TYPE;

    private static final String ELEMENT_VERIFICATION_METHOD = "verificationMethod";

    private static final String ELEMENT_SERVICE = "service";

    private final DgcProperties dgcProperties;

    private final KeyProvider keyProvider;

    /**
     * Create identity Object with given informations.
     * 
     * @param element Element
     * @param type Type
     * @return {@link IdentityResponse}
     */
    public IdentityResponse getIdentity(final String element, final String type) {
        final String identityId = String.format("%s%s", dgcProperties.getServiceUrl(), IDENTITY_ROOT);

        final IdentityResponse identityResponse = new IdentityResponse();
        identityResponse.setId(identityId);
        identityResponse.setVerificationMethod(getVerificationMethods(element, type));
        identityResponse.setService(getServices(element, type));
        return identityResponse;
    }

    private List<VerificationIdentityResponse> getVerificationMethods(final String element, final String type) {
        final String identityRoot = String.format("%s%s", dgcProperties.getServiceUrl(), IDENTITY_ROOT);
        final String identityPath = String.format("%s%s", dgcProperties.getServiceUrl(), IDENTITY_PATH);

        return keyProvider.getKeyNames(KeyType.ALL).stream()
                .filter(keyName -> element == null || ELEMENT_VERIFICATION_METHOD.equalsIgnoreCase(element))
                .map(keyName -> {
                    final VerificationIdentityResponse verificationMethod = new VerificationIdentityResponse();
                    verificationMethod.setId(String.format("%s/%s", identityPath, keyName));
                    verificationMethod.setController(identityRoot);
                    verificationMethod.setType(VERIFICATION_TYPE);
                    verificationMethod.setPublicKeyJwk(buildPublicKey(keyName));
                    return verificationMethod;
                })
                .filter(method -> type == null || type.equalsIgnoreCase(method.getType()))
                .collect(Collectors.toList());
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

    private List<ServiceIdentityResponse> getServices(final String element, final String type) {
        return Stream.concat(dgcProperties.getServices(), dgcProperties.getEndpoints())
                .filter(service -> element == null || ELEMENT_SERVICE.equalsIgnoreCase(element))
                .map(service -> {
                    final ServiceIdentityResponse response = new ServiceIdentityResponse();
                    response.setId(service.getId());
                    response.setType(service.getType());
                    response.setServiceEndpoint(service.getServiceEndpoint());
                    response.setName(service.getName());
                    return response;
                })
                .filter(method -> type == null || type.equalsIgnoreCase(method.getType()))
                .collect(Collectors.toList());
    }

    private PublicKeyJwkIdentityResponse buildPublicKey(String keyName) {
        final Certificate[] certificate = keyProvider.receiveCertificate(keyName);
        if (certificate == null) {
            return null;
        }
        try {
            final PublicKeyJwkIdentityResponse publicKeyJwk = new PublicKeyJwkIdentityResponse();
            List<String> x5c = new ArrayList<String>();
            for (Certificate cert : certificate) {
                x5c.add(Base64.getEncoder().encodeToString(cert.getEncoded()));
            }

            publicKeyJwk.setX5c(x5c.toArray(new String[0]));                                       
            publicKeyJwk.setKid(keyProvider.getKid(keyName));
            publicKeyJwk.setAlg(keyProvider.getAlg(keyName));
            publicKeyJwk.setUse(keyProvider.getKeyUse(keyName).name().toLowerCase());
            return publicKeyJwk;
        } catch (CertificateEncodingException e) {
            throw new DccException("Can not encode certificate", e);
        }
    }
}
