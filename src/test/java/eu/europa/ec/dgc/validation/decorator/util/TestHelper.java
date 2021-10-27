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

package eu.europa.ec.dgc.validation.decorator.util;

import eu.europa.ec.dgc.validation.decorator.dto.IdentityResponse.ServiceIdentityResponse;
import eu.europa.ec.dgc.validation.decorator.entity.ServiceTokenContentResponse;
import eu.europa.ec.dgc.validation.decorator.entity.ServiceTokenContentResponse.OccurrenceInfoResponse;
import eu.europa.ec.dgc.validation.decorator.entity.ServiceTokenContentResponse.SubjectResponse;
import eu.europa.ec.dgc.validation.decorator.entity.ValidationServiceIdentityResponse;
import eu.europa.ec.dgc.validation.decorator.entity.ValidationServiceIdentityResponse.PublicKeyJwk;
import eu.europa.ec.dgc.validation.decorator.entity.ValidationServiceIdentityResponse.VerificationMethod;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

public class TestHelper {

    public static ServiceTokenContentResponse buildServiceTokenContent(final String subject,
            ServiceIdentityResponse service) {
        return buildServiceTokenContent(subject, service.getId());
    }

    public static ServiceTokenContentResponse buildServiceTokenContent(final String subject, final String serviceId) {
        final String serviceIdBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(serviceId.getBytes());
        final SubjectResponse subjectResponse = new SubjectResponse();
        subjectResponse.setId(UUID.fromString(subject));
        subjectResponse.setForename("Lionel");
        subjectResponse.setLastname("Kuhic");
        subjectResponse.setBirthDate("1994-05-25");
        subjectResponse.setServiceIdUsed(serviceIdBase64);
        subjectResponse.setJti(UUID.randomUUID().toString());

        final OffsetDateTime departureTime = OffsetDateTime.now().plusDays(1);
        final OccurrenceInfoResponse occurrenceInfo = new OccurrenceInfoResponse();
        occurrenceInfo.setFrom("East Kizzieshire");
        occurrenceInfo.setTo("South Duncanhaven");
        occurrenceInfo.setTime(departureTime);
        occurrenceInfo.setType(2);
        occurrenceInfo.setCategories(Arrays.asList("Standard"));
        occurrenceInfo.setConditionTypes(Arrays.asList("r", "v", "t"));
        occurrenceInfo.setCountryOfArrival("TT");
        occurrenceInfo.setRegionOfArrival("TT");
        occurrenceInfo.setCountryOfDeparture("TD");
        occurrenceInfo.setRegionOfDeparture("TD");
        occurrenceInfo.setDepartureTime(departureTime);
        occurrenceInfo.setArrivalTime(departureTime.plusHours(8).plusMinutes(24));
        occurrenceInfo.setLanguage("en-en");

        final ServiceTokenContentResponse tokenContent = new ServiceTokenContentResponse();
        tokenContent.setReference("TestBookingReference");
        tokenContent.setTime(OffsetDateTime.now());
        tokenContent.getSubjects().add(subjectResponse);
        tokenContent.setFlightInfo(occurrenceInfo);
        return tokenContent;
    }

    /*
     * Algorithm: ES256
     * 
     * Test public key:
     * MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEEVs/o5+uQbTjL3chynL4wXgUg2R9q9UU8I5mEovUf86QZ7kOBIjJwqnzD1omageEHWwHdBO6B+
     * dFabmdT9POxg==
     * 
     * Test private key:
     * MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgevZzL1gdAFr88hb2OF/2NxApJCzGCEDdfSp6VQO30hyhRANCAAQRWz+
     * jn65BtOMvdyHKcvjBeBSDZH2r1RTwjmYSi9R/zpBnuQ4EiMnCqfMPWiZqB4QdbAd0E7oH50VpuZ1P087G
     */
    public static ValidationServiceIdentityResponse buildValidationServiceIdentity() {
        final PublicKeyJwk publicKeyJwk = new PublicKeyJwk();
        publicKeyJwk.setUse("sig");
        publicKeyJwk.setAlg("ES256");
        publicKeyJwk.setKid("MFkwEwYHKu+=");
        publicKeyJwk.setX5c("MIIB4DCCAYegAwIBAgIUVuls/1X3r1LY9+KcbRnX1ixbl8YwCgYIKoZIzj0EAwIw"
                + "RTELMAkGA1UEBhMCREUxEzARBgNVBAgMClNvbWUtU3RhdGUxITAfBgNVBAoMGElu"
                + "dGVybmV0IFdpZGdpdHMgUHR5IEx0ZDAgFw0yMTA5MjMwODMxMDRaGA8yMTIwMDQx"
                + "NzA4MzEwNFowRTELMAkGA1UEBhMCREUxEzARBgNVBAgMClNvbWUtU3RhdGUxITAf"
                + "BgNVBAoMGEludGVybmV0IFdpZGdpdHMgUHR5IEx0ZDBZMBMGByqGSM49AgEGCCqG"
                + "SM49AwEHA0IABBFbP6OfrkG04y93Icpy+MF4FINkfavVFPCOZhKL1H/OkGe5DgSI"
                + "ycKp8w9aJmoHhB1sB3QTugfnRWm5nU/TzsajUzBRMB0GA1UdDgQWBBSaqFjzps1q"
                + "G+x2DPISjaXTWsTOdDAfBgNVHSMEGDAWgBSaqFjzps1qG+x2DPISjaXTWsTOdDAP"
                + "BgNVHRMBAf8EBTADAQH/MAoGCCqGSM49BAMCA0cAMEQCIC/VAxhYH0HGDgcHIJwJ"
                + "QXgThit8ZVqAxwzcK2/CUZPRAiASv2PY68vYaHSUZSICg80zO3puKPfum9126fmU"
                + "4LlytA==");

        final VerificationMethod sig = new VerificationMethod();
        sig.setType("JsonWebKey2020");
        sig.setId("http://localhost:8082/identity/verificationMethod/JsonWebKey2020#validationservicesignkey");
        sig.setController("http://localhost:8082/identity");
        sig.setPublicKeyJwk(publicKeyJwk);

        final ValidationServiceIdentityResponse response = new ValidationServiceIdentityResponse();
        response.setId("http://localhost:8082/identity");
        response.setVerificationMethod(Arrays.asList(sig));
        return response;
    }

}
