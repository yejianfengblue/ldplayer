package com.yejianfengblue.ldplayer.command;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR, reason = "command failure")
public class CommandFailureException extends RuntimeException {

    public CommandFailureException(String message) {
        super(message);
    }
}
