package com.yejianfengblue.ldplayer.command;

import lombok.Value;

import java.util.List;

@Value
public class CommandExecutionResult {

    int exitValue;

    List<String> outputLines;
}
