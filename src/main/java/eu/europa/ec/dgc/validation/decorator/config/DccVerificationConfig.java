/*-
 * ---license-start
 * European Digital COVID Certificate Validation Decorator / dgca-validation-decorator
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

package eu.europa.ec.dgc.validation.decorator.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dgca.verifier.app.decoder.JsonSchemaKt;
import dgca.verifier.app.engine.AffectedFieldsDataRetriever;
import dgca.verifier.app.engine.CertLogicEngine;
import dgca.verifier.app.engine.DefaultAffectedFieldsDataRetriever;
import dgca.verifier.app.engine.DefaultCertLogicEngine;
import dgca.verifier.app.engine.DefaultJsonLogicValidator;
import dgca.verifier.app.engine.JsonLogicValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DccVerificationConfig {

    @Bean
    ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    @Bean
    AffectedFieldsDataRetriever affectedFieldsDataRetriever(ObjectMapper objectMapper) throws JsonProcessingException {
        JsonNode jsonNode = objectMapper.readTree(JsonSchemaKt.JSON_SCHEMA_V1);
        return new DefaultAffectedFieldsDataRetriever(jsonNode, objectMapper);
    }

    @Bean
    JsonLogicValidator jsonLogicValidator() {
        return new DefaultJsonLogicValidator();
    }

    @Bean
    CertLogicEngine certLogicEngine(AffectedFieldsDataRetriever affectedFieldsDataRetriever,
            JsonLogicValidator jsonLogicValidator) {
        return new DefaultCertLogicEngine(affectedFieldsDataRetriever, jsonLogicValidator);
    }
}
