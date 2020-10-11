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
     * @return  a wrapper of exit value and output lines.
     *          If no output, the output lines is a empty list.
     *
     * @throws InterruptedException  process is interrupted
     * @throws CommandExecutionFailureException  if fail to read command output
     */
    public static CommandExecutionResult execute(String cmd)
            throws InterruptedException, CommandExecutionFailureException {

        log.info("Execute command '{}'", cmd);

        Process process = null;
        try {
            process = Runtime.getRuntime().exec(cmd);
            log.info("Waiting for command '{}' to terminate", cmd);

            List<String> outputLines = new BufferedReader(new InputStreamReader(process.getInputStream(), UTF_8))
                    .lines()
                    .collect(Collectors.toList());
            log.debug("outputLines: {}", outputLines);
            if (!outputLines.isEmpty()) {
                log.info("Output:\n{}", String.join("\n", outputLines));
            } else {
                log.info("No output");
            }

            int exitValue = process.waitFor();
            log.info("Exit value: {}", exitValue);

            return new CommandExecutionResult(process.exitValue(), outputLines);

        } catch (IOException ioException) {

            process.destroy();
            log.error("Fail to read output of command '{}'", cmd, ioException);
            throw new CommandExecutionFailureException(
                    String.format("Fail to read output of command '%s'. %s", cmd, ioException.getMessage()));
        } catch (InterruptedException interruptedException) {
            throw new InterruptedException(String.format("Command '%s' interrupted", cmd));
        }
    }
}
