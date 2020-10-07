package com.yejianfengblue.ldplayer.command;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
@Slf4j
public class CommandExecutor {

    /**
     * @return  a wrapper of exit value and output lines
     * @throws  CommandFailureException  if error during read output or process is interrupted
     */
    public static CommandExecutionResult execute(String cmd) throws CommandFailureException {

        log.info("Execute command '{}'", cmd);

        Process process = null;
        try {
            process = Runtime.getRuntime().exec(cmd);
            log.info("Waiting for command '{}' to terminate", cmd);

            List<String> outputLines = new BufferedReader(new InputStreamReader(process.getInputStream(), UTF_8))
                    .lines()
                    .collect(Collectors.toList());
            log.info("Output: {}", String.join("\n", outputLines));

            int exitValue = process.waitFor();
            log.info("Exit value: {}", exitValue);

            return new CommandExecutionResult(process.exitValue(), outputLines);

        } catch (IOException | InterruptedException e) {

            if (null != process) {
                process.destroy();
            }
            log.error("Fail to execute command '{}'", cmd, e);
            throw new CommandFailureException(
                    String.format("Fail to execute command '%s' with error '%s'", cmd, e.getMessage()));
        }
    }
}
