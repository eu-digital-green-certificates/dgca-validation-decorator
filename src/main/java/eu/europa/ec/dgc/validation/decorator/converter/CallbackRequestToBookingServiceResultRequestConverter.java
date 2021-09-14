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

package eu.europa.ec.dgc.validation.decorator.converter;

import eu.europa.ec.dgc.validation.decorator.dto.CallbackRequest;
import eu.europa.ec.dgc.validation.decorator.dto.CallbackRequest.Result;
import eu.europa.ec.dgc.validation.decorator.entity.BookingServiceResultRequest;
import eu.europa.ec.dgc.validation.decorator.entity.BookingServiceResultRequest.DccStatusRequest;
import eu.europa.ec.dgc.validation.decorator.entity.BookingServiceResultRequest.DccStatusResult;
import eu.europa.ec.dgc.validation.decorator.entity.BookingServiceResultRequest.DccStatusType;
import eu.europa.ec.dgc.validation.decorator.entity.BookingServiceResultRequest.ResultRequest;
import eu.europa.ec.dgc.validation.decorator.exception.NotFoundException;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

@Service
public class CallbackRequestToBookingServiceResultRequestConverter
        implements Converter<CallbackRequest, BookingServiceResultRequest> {

    @Override
    public BookingServiceResultRequest convert(CallbackRequest callback) {
        BookingServiceResultRequest result = new BookingServiceResultRequest();
        result.setDccStatus(new DccStatusRequest());
        result.getDccStatus().setIssuer(callback.getIssuer());
        result.getDccStatus().setIat(callback.getIat());
        result.getDccStatus().setSub(callback.getSub());

        if (callback.getResults() != null) {
            result.getDccStatus().setResults(callback.getResults().stream()
                    .map(this::convert)
                    .collect(Collectors.toList()));
        }
        return result;
    }

    private ResultRequest convert(Result callbackResult) {
        final ResultRequest resultRequest = new ResultRequest();
        resultRequest.setIdentifier(callbackResult.getIdentifier());
        resultRequest.setResult(DccStatusResult.valueOf(callbackResult.getResult()));
        resultRequest.setDetails(callbackResult.getDetails());

        for (DccStatusType dccStatusType : DccStatusType.values()) {
            if (dccStatusType.getName().equalsIgnoreCase(callbackResult.getType())
                    || dccStatusType.name().equalsIgnoreCase(callbackResult.getType())) {
                resultRequest.setType(dccStatusType);
            }
        }

        if (callbackResult.getType() != null
                && !callbackResult.getType().isBlank()
                && resultRequest.getType() == null) {
            throw new NotFoundException(String.format("Enum for type '%s' not found", callbackResult.getType()));
        }
        return resultRequest;
    }
}
