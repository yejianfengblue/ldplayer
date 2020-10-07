package com.yejianfengblue.ldplayer;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR, reason = "ldplayer failure")
public class LdplayerFailureException extends RuntimeException {

    public LdplayerFailureException(String message) {
        super(message);
    }
}
