package com.yejianfengblue.ldplayer.command;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class CommandFailureException extends RuntimeException {

    public CommandFailureException(String message) {
        super(message);
    }
}
