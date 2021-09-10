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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class AccessTokenPayload {

    @JsonProperty("Jti")
    private String jti;

    private String iss;

    private long iat;

    private String sub;

    private long exp;

    private int type;

    private String version;

    private AccessTokenConditions conditions;

    @Data
    public static final class AccessTokenConditions {

        // Hash of the dcc. Not applicable for Type 1,2.
        private String hash;

        // Selected language.
        private String lang;

        // ICOA 930 transliterated surname (Familienname).
        private String fnt;

        // ICOA 930 transliterated given name.
        private String gnt;

        // Date of birth.
        private String dob;

        // Contry of Arrival.
        private String coa;

        // Country of Departure.
        private String cod;

        // Region of Arrival ISO 3166-2 without Country.
        private String roa;

        // Region of Departure ISO 3166-2 without Country.
        private String rod;

        // Acceptable Type of DCC.
        private List<String> type;

        // Optional category which shall be reflected in the validation by additional rules/logic. 
        // If null, Standard Business Rule Check will apply.
        private List<String> category;

        // Date where te DCC must be validateable.
        private String validationClock;

        // DCC must be valid from this date (ISO8601 with offset).
        private String validFrom;

        // DCC must be valid minimum to this date (ISO8601 with offset).
        private String validTo;
    }
}
