package eu.europa.ec.dgc.validation.decorator.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;

@ResponseStatus(value = HttpStatus.GONE)
public class RepositoryException extends RuntimeException {

    public RepositoryException(final String message, final HttpClientErrorException e) {
        super(message, e);
    }
}
