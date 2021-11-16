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

import eu.europa.ec.dgc.validation.decorator.entity.KeyType;
import eu.europa.ec.dgc.validation.decorator.entity.KeyUse;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.List;

public interface KeyProvider {

    Certificate[] receiveCertificate(String keyName);

    PrivateKey receivePrivateKey(String keyName);

    String getKeyName(String kid);

    List<String> getKeyNames(KeyType type);

    String getKid(String keyName);

    String getAlg(String keyName);

    String getActiveSignKey();

    KeyUse getKeyUse(String keyName);
}
