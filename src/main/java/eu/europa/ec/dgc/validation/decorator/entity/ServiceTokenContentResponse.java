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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class ServiceTokenContentResponse {

    private String reference;

    private OffsetDateTime time;

    private List<SubjectResponse> passengers = new ArrayList<>();

    private OccurrenceInfoResponse flightInfo;

    public List<SubjectResponse> getSubjects() {
        return this.getPassengers();
    }

    public OccurrenceInfoResponse getOccurrenceInfo() {
        return this.getFlightInfo();
    }

    @Data
    public static final class SubjectResponse {

        private UUID id;

        private String forename;

        private String lastname;

        private String birthDate;

        private DccStatusResponse dccStatus;

        private String serviceIdUsed;

        private String jti;
    }

    @Data
    public static final class DccStatusResponse {

        private String issuer;

        private long iat;

        private String sub;

        private List<ResultResponse> results;

        private String confirmation;
    }

    @Data
    public static final class ResultResponse {

        private String identifier;

        private String result;

        private String type;

        private String details;
    }

    @Data
    public static final class OccurrenceInfoResponse {

        private String from;

        private String to;

        private OffsetDateTime time;

        @JsonProperty("coa")
        private String countryOfArrival;

        @JsonProperty("cod")
        private String countryOfDeparture;

        @JsonProperty("roa")
        private String regionOfArrival;

        @JsonProperty("rod")
        private String regionOfDeparture;

        @JsonProperty("departureTime")
        private OffsetDateTime departureTime;

        @JsonProperty("arrivalTime")
        private OffsetDateTime arrivalTime;

        private int type;

        private List<String> categories;

        @JsonProperty("lang")
        private String language;

        private List<String> conditionTypes;
    }
}
