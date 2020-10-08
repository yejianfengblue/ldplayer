package com.yejianfengblue.ldplayer;

import com.yejianfengblue.ldplayer.command.CommandExecutionFailureException;
import com.yejianfengblue.ldplayer.command.CommandExecutionResult;
import com.yejianfengblue.ldplayer.command.CommandExecutor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
class Ldconsole {

    private static final String LDCONSOLE = "ldconsole";

    /**
     * Because ldconsole erases the exit value of adb command, it's unsafe to detect failure based on exit value.
     *
     * @return output lines
     *
     * @throws InterruptedException  command execution process is interrupted
     * @throws CommandExecutionFailureException  command execution failed due to interruption or output reading failure
     */
    List<String> adb(int index, String command) throws InterruptedException, CommandExecutionFailureException {

        String cmd = LDCONSOLE + " adb" +
                " --index " + index +
                " --command \"" + command + "\"";
        CommandExecutionResult commandExecutionResult = CommandExecutor.execute(cmd);
        return commandExecutionResult.getOutputLines();
    }

    /**
     * @return newly created index
     *
     * @throws InterruptedException  command execution process is interrupted
     * @throws CommandExecutionFailureException  command execution failed due to interruption or output reading failure
     * @throws LdplayerFailureException  command is executed but considered as failure according to exit value or
     *                                   output.
     */
    int copy(String name, int fromIndex)
            throws InterruptedException, CommandExecutionFailureException, LdplayerFailureException {

        // copy command exit value is the new index
        // copy command doesn't have output if succeeds
        CommandExecutionResult commandExecutionResult = CommandExecutor.execute(
                LDCONSOLE + " copy" +
                        " --name " + name +
                        " --from " + fromIndex);
        int exitValue = commandExecutionResult.getExitValue();
        List<String> outputLines = commandExecutionResult.getOutputLines();
        String output = String.join("\n", outputLines);
        if (exitValue < 0 || !outputLines.isEmpty()) {
            String errMsg = String.format("Fail to copy from index %d. %s", fromIndex, output);
            log.error(errMsg);
            throw new LdplayerFailureException(errMsg);
        } else {
            return exitValue;
        }
    }

    /**
     * @throws InterruptedException  command execution process is interrupted
     * @throws CommandExecutionFailureException  command execution failed due to interruption or output reading failure
     * @throws LdplayerFailureException  command is executed but considered as failure according to exit value or
     *                                   output.
     */
    void installApp(int index, String apkPath)
            throws InterruptedException, CommandExecutionFailureException, LdplayerFailureException {

        String cmd = LDCONSOLE + " installapp" +
                " --index " + index +
                " --filename " + "\"" + apkPath + "\"";
        CommandExecutionResult commandExecutionResult = CommandExecutor.execute(cmd);
        if (commandExecutionResult.getExitValue() != 0 || !commandExecutionResult.getOutputLines().isEmpty()) {
            String errMsg = String.format("Fail to install %s to index %d. %s", apkPath, index,
                    String.join("\n", commandExecutionResult.getOutputLines()));
            log.error(errMsg);
            throw new LdplayerFailureException(errMsg);
        }
    }

    /**
     * @throws InterruptedException  command execution process is interrupted
     * @throws CommandExecutionFailureException  command execution failed due to interruption or output reading failure
     * @throws LdplayerFailureException  command is executed but considered as failure according to exit value or
     *                                   output.
     */
    boolean isRunning(int index)
            throws InterruptedException, CommandExecutionFailureException, LdplayerFailureException {

        String cmd = LDCONSOLE + " isrunning --index " + index;
        CommandExecutionResult commandExecutionResult = CommandExecutor.execute(cmd);
        List<String> isRunningOutput = commandExecutionResult.getOutputLines();
        if (commandExecutionResult.getExitValue() == 0) {

            if (isRunningOutput.size() == 1) {
                return isRunningOutput.get(0).equals("running");
            } else {
                String errMsg = String.format("Command '%s' output more than one line: \n%s",
                        cmd, isRunningOutput);
                log.error(errMsg);
                throw new LdplayerFailureException(errMsg);
            }
        } else {
            String errMsg = String.format("Fail to check run status for index %d. %s",
                    index, String.join("\n", isRunningOutput));
            log.error(errMsg);
            throw new LdplayerFailureException(errMsg);
        }
    }

    /**
     * @throws InterruptedException  command execution process is interrupted
     * @throws CommandExecutionFailureException  command execution failed due to interruption or output reading failure
     * @throws LdplayerFailureException  command is executed but considered as failure according to exit value or
     *                                   output.
     */
    void launch(int index)
            throws InterruptedException, CommandExecutionFailureException, LdplayerFailureException {

        CommandExecutionResult commandExecutionResult = CommandExecutor.execute(LDCONSOLE + " launch --index " + index);

        if (commandExecutionResult.getExitValue() == 0) {
            List<String> outputLines = commandExecutionResult.getOutputLines();
            if (!outputLines.isEmpty()) {
                String errMsg = String.format("Fail to launch index %d. %s", index, String.join("\n", outputLines));
                log.error(errMsg);
                throw new LdplayerFailureException(errMsg);
            }
        }
    }

    /**
     * @throws InterruptedException  command execution process is interrupted
     * @throws CommandExecutionFailureException  command execution failed due to interruption or output reading failure
     * @throws LdplayerFailureException  command is executed but considered as failure according to exit value or
     *                                   output.
     */
    List<LdplayerState> list()
            throws InterruptedException, CommandExecutionFailureException, LdplayerFailureException {

        String cmd = LDCONSOLE + " list2";
        CommandExecutionResult commandExecutionResult = CommandExecutor.execute(cmd);
        if (commandExecutionResult.getExitValue() == 0) {
            List<String> list2Output = commandExecutionResult.getOutputLines();
            return list2Output.stream()
                    .map(line -> line.split(","))
                    .filter(columns -> columns.length == 7)
                    .map(columns -> new LdplayerState(
                            Integer.parseInt(columns[0]),
                            columns[1],
                            "1".equals(columns[4])))
                    .collect(Collectors.toList());
        } else {
            String errMsg = String.format("Fail to list");
            log.error(errMsg);
            throw new LdplayerFailureException(errMsg);
        }
    }

    Modify.ModifyBuilder modify(int index) {
        return Modify.builder(index);
    }

    @Builder(builderMethodName = "internalBuilder")
    static class Modify {

        private final int index;

        private String manufacturer;

        private String model;

        static ModifyBuilder builder(int index) {
            return internalBuilder().index(index);
        }

        /**
         * @throws InterruptedException  command execution process is interrupted
         * @throws CommandExecutionFailureException  command execution failed due to interruption or output reading failure
         * @throws LdplayerFailureException  command is executed but considered as failure according to exit value or
         *                                   output.
         */
        void run() throws InterruptedException, CommandExecutionFailureException, LdplayerFailureException {

            StringBuilder cmdBuilder = new StringBuilder(LDCONSOLE + " modify --index " + index);
            if (StringUtils.isNotBlank(manufacturer))
                cmdBuilder.append(" --manufacturer " + manufacturer);
            if (StringUtils.isNotBlank(model))
                cmdBuilder.append(" --model " + model);

            String cmd = cmdBuilder.toString();
            CommandExecutionResult commandExecutionResult = CommandExecutor.execute(cmd);
            if (commandExecutionResult.getExitValue() != 0 || !commandExecutionResult.getOutputLines().isEmpty()) {
                String errMsg = String.format("Fail to modify index %d. %s",
                        index, String.join("\n", commandExecutionResult.getOutputLines()));
                log.error(errMsg);
                throw new LdplayerFailureException(errMsg);
            }
        }
    }

    /**
     * Because ldconsole erases the exit value of adb command, it's unsafe to detect failure based on exit value.
     *
     * @param index
     * @param localPath  file path in PC
     * @param remotePath file path in ldplayer, either file path or directory path
     *
     * @throws InterruptedException  command execution process is interrupted
     * @throws CommandExecutionFailureException  command execution failed due to interruption or output reading failure
     * @throws LdplayerFailureException  command is executed but considered as failure according to exit value or
     *                                   output.
     */
    void push(int index, String localPath, String remotePath)
            throws InterruptedException, CommandExecutionFailureException, LdplayerFailureException {

        String cmd = LDCONSOLE + " push --index " + index +
                " --remote " + "\"" + remotePath + "\"" +
                " --local " + "\"" + localPath + "\"";
        CommandExecutionResult commandExecutionResult = CommandExecutor.execute(cmd);
        if (commandExecutionResult.getExitValue() != 0 || !commandExecutionResult.getOutputLines().isEmpty()) {
            String errMsg = String.format("Fail to push index %d from local %s to remote %s. %s",
                    index, localPath, remotePath, String.join("\n", commandExecutionResult.getOutputLines()));
            log.error(errMsg);
            throw new LdplayerFailureException(errMsg);
        }
    }

    /**
     * Because ldconsole erases the exit value of adb command, it's unsafe to detect failure based on exit value.
     *
     * @param index
     * @param namespace  global, secure, system
     * @param key
     * @param value
     *
     * @throws InterruptedException  command execution process is interrupted
     * @throws CommandExecutionFailureException  command execution failed due to interruption or output reading failure
     * @throws LdplayerFailureException  command is executed but considered as failure according to exit value or
     *                                   output.
     */
    void putSetting(int index, String namespace, String key, String value)
            throws InterruptedException, CommandExecutionFailureException, LdplayerFailureException {

        String cmd = String.format("%s adb --index %d --command \"shell settings put %s %s %s\"",
                LDCONSOLE, index, namespace, key, value);
        CommandExecutionResult commandExecutionResult = CommandExecutor.execute(cmd);
        if (commandExecutionResult.getExitValue() != 0 || !commandExecutionResult.getOutputLines().isEmpty()) {
            String errMsg = String.format("Fail to push setting for index=%d, namespace=%s, key=%s, value=%s. %s",
                    index, namespace, key, value, String.join("\n", commandExecutionResult.getOutputLines()));
            log.error(errMsg);
            throw new LdplayerFailureException(errMsg);
        }
    }

    /**
     * @throws InterruptedException  command execution process is interrupted
     * @throws CommandExecutionFailureException  command execution failed due to interruption or output reading failure
     * @throws LdplayerFailureException  command is executed but considered as failure according to exit value or
     *                                   output.
     */
    void reboot(int index) throws InterruptedException, CommandExecutionFailureException, LdplayerFailureException {

        String cmd = LDCONSOLE + " reboot --index " + index;
        CommandExecutionResult commandExecutionResult = CommandExecutor.execute(cmd);
        if (commandExecutionResult.getExitValue() != 0 || !commandExecutionResult.getOutputLines().isEmpty()) {
            String errMsg = String.format("Fail to reboot index %d. %s",
                    index, String.join("\n", commandExecutionResult.getOutputLines()));
            log.error(errMsg);
            throw new LdplayerFailureException(errMsg);
        }
    }

    /**
     * @throws InterruptedException  command execution process is interrupted
     * @throws CommandExecutionFailureException  command execution failed due to interruption or output reading failure
     * @throws LdplayerFailureException  command is executed but considered as failure according to exit value or
     *                                   output.
     */
    void quit(int index) throws InterruptedException, CommandExecutionFailureException, LdplayerFailureException {

        String cmd = LDCONSOLE + " quit --index " + index;
        CommandExecutionResult commandExecutionResult = CommandExecutor.execute(cmd);
        if (commandExecutionResult.getExitValue() != 0 || !commandExecutionResult.getOutputLines().isEmpty()) {
            String errMsg = String.format("Fail to quit index %d. %s",
                    index, String.join("\n", commandExecutionResult.getOutputLines()));
            throw new LdplayerFailureException(String.format("Fail to execute command '%s'", cmd));
        }
    }
}
