package eu.europa.ec.dgc.validation.decorator.exception;

import java.io.UnsupportedEncodingException;

public class UncheckedUnsupportedEncodingException extends RuntimeException {

    public UncheckedUnsupportedEncodingException(UnsupportedEncodingException e) {
        super(e);
    }
}
