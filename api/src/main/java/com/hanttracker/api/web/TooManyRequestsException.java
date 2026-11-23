package com.hanttracker.api.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Answered with 429 when a rate limit is reached (see RateLimits). */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException() {
        super("Too many attempts. Wait a few minutes and try again.");
    }
}
