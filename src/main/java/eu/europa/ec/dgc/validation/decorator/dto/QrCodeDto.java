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

package eu.europa.ec.dgc.validation.decorator.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QrCodeDto {

    // Type of the requested protocol
    // used from application.yml
    private String protocol;

    // SemVer version number
    // used from application.yml
    private String protocolVersion;

    // URL to the service provider identity document
    // used from application.yml
    private String serviceIdentity;

    // Service Validation Document
    // used from application.yml
    // currently disabled // private String validationIdentity;

    // JWT for access Validation Decorator from booking service 
    private String token;

    // Consent text which is shown to the user by the wallet app
    // used from application.yml
    private String consent;

    // Subject of the Request
    private String subject;

    // Company Name
    // used from application.yml
    private String serviceProvider;
}
