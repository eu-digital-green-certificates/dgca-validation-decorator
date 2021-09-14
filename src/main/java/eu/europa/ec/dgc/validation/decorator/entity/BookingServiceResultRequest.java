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

package eu.europa.ec.dgc.validation.decorator.entity;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
public class BookingServiceResultRequest {

    private String token;

    @Valid
    @NotNull
    private DccStatusRequest dccStatus;

    @Data
    public static final class DccStatusRequest {

        private String issuer;

        private long iat;

        private String sub;

        @Valid
        @NotEmpty
        private List<ResultRequest> results;
    }

    @Data
    public static final class ResultRequest {

        private String identifier;

        private DccStatusResult result;

        private DccStatusType type;

        private String details;
    }
    
    
    public static enum DccStatusResult {

        OPEN,

        FAILED,

        PASSED;
    }
    
    @Getter
    @AllArgsConstructor
    public static enum DccStatusType {

        TECHNICAL_CHECK("Technical Check"),

        ISSUER_INVALIDATION("Issuer Invalidation"),

        DESTINATION("Destination"),

        ACCEPTANCE_TRAVELER("Acceptance Traveler"),

        ACCEPTANCE("Acceptance");

        private String name;
    }
}
