package com.apnastorage.common;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FieldErrorsException extends RuntimeException {

    private final Map<String, List<String>> errors;

    public FieldErrorsException(Map<String, List<String>> errors) {
        this.errors = Collections.unmodifiableMap(new LinkedHashMap<>(errors));
    }

    public Map<String, List<String>> errors() {
        return errors;
    }
}
