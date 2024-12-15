package com.innovasolutions.framework.webclient.util;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;

@Slf4j
public final class Utils {

    /**
     * logs the service execution time.
     *
     * @param start
     */
    public static void logExecutionTime(Instant start) {
        Instant end = Instant.now();
        long executionTimeMillis = Duration.between(start, end).toMillis();
        log.info("Completed service execution in {} ms ", executionTimeMillis);
    }
}
