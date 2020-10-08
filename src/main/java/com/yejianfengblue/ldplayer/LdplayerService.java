package com.yejianfengblue.ldplayer;

import com.yejianfengblue.ldplayer.command.CommandExecutionFailureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class LdplayerService {

    private final Ldconsole ldconsole;

    /**
     * Create a ldplayer by copying from the one with given index.
     * Modify manufacturer and model, install apks, install certificate, set global http proxy, reboot on demand.
     *
     * @throws InterruptedException  underlying command is interrupted
     *                               or interrupted when wait for android ready after reboot
     * @throws CommandExecutionFailureException  underlying command execution failed due to output reading failure
     * @throws LdplayerFailureException         underlying command is executed but considered as failure
     *                                          according to exit value or output
     */
    public Ldplayer create(LdplayerCreation ldplayerCreation)
            throws InterruptedException, LdplayerFailureException, CommandExecutionFailureException {

        int newLdplayerIndex = ldconsole.copy(ldplayerCreation.getName(), ldplayerCreation.getFromIndex());

        Ldplayer newLdplayer = new Ldplayer(newLdplayerIndex);
        newLdplayer.setName(ldplayerCreation.getName());

        // manufacturer and model
        if (StringUtils.isNotBlank(ldplayerCreation.getManufacturer())
                && StringUtils.isNotBlank(ldplayerCreation.getModel())) {
            ldconsole.modify(newLdplayerIndex)
                    .manufacturer(ldplayerCreation.getManufacturer())
                    .model(ldplayerCreation.getModel())
                    .build()
                    .run();
        }

        // apk
        if (ldplayerCreation.getInstallApkPaths() != null) {
            for (String apk : ldplayerCreation.getInstallApkPaths()) {
                installApk(newLdplayerIndex, apk);
            }
        }

        // certificate
        if (ldplayerCreation.getInstallCertPaths() != null) {
            for (String cert : ldplayerCreation.getInstallCertPaths()) {
                installCert(newLdplayerIndex, cert);
            }
        }

        boolean rebootRequired = false;
        // http proxy, need restart, so put last
        if (StringUtils.isNotBlank(ldplayerCreation.getHttpProxyHost())
                && null != ldplayerCreation.getHttpProxyPort()) {
            setHttpProxy(newLdplayerIndex,
                    ldplayerCreation.getHttpProxyHost(), ldplayerCreation.getHttpProxyPort(),
                    ldplayerCreation.getHttpProxyExclusionList());
            rebootRequired = true;
        }

        // reboot or quit
        if (ldplayerCreation.isRunAfterCreate()) {

            if (rebootRequired) {
                ldconsole.reboot(newLdplayerIndex);
                while (!isAndroidReady(newLdplayerIndex)) {
                    TimeUnit.SECONDS.sleep(10);
                }
            }
            newLdplayer.setRunning(true);
            newLdplayer.setAndroidReady(true);

        } else {
            ldconsole.quit(newLdplayerIndex);
        }

        return newLdplayer;
    }

    /**
     * @throws InterruptedException  underlying command execution is interrupted
     * @throws CommandExecutionFailureException underlying command execution failed due to output reading failure
     * @throws LdplayerFailureException         underlying command is executed but considered as failure
     *                                          according to exit value or output
     */
    public Optional<Ldplayer> get(int index)
            throws InterruptedException, LdplayerFailureException, CommandExecutionFailureException {

        for (LdplayerState state : ldconsole.list()) {

            if (state.getIndex() == index) {
                return Optional.of(
                        new Ldplayer(
                                state.getTitle(),
                                state.getIndex(),
                                ldconsole.isRunning(index),
                                state.isAndroidReady()));
            }
        }

        return Optional.empty();
    }

    /**
     * @throws InterruptedException  underlying command execution is interrupted
     * @throws CommandExecutionFailureException underlying command execution failed due to output reading failure
     * @throws LdplayerFailureException  underlying command is executed but considered as failure
     *                                   according to exit value or output
     */
    public List<Ldplayer> getAll()
            throws InterruptedException, LdplayerFailureException, CommandExecutionFailureException {

        List<Ldplayer> ldplayers = new ArrayList<>();

        for (LdplayerState state : ldconsole.list()) {
            ldplayers.add(
                    new Ldplayer(
                            state.getTitle(),
                            state.getIndex(),
                            ldconsole.isRunning(state.getIndex()),
                            state.isAndroidReady()));
        }

        return ldplayers;
    }

    /**
     * @throws InterruptedException  underlying command execution is interrupted
     * @throws CommandExecutionFailureException underlying command execution failed due to output reading failure
     * @throws LdplayerFailureException  underlying command is executed but considered as failure
     *                                   according to exit value or output
     */
    public boolean isAndroidReady(int index)
            throws InterruptedException, LdplayerFailureException, CommandExecutionFailureException {

        return ldconsole.list().stream()
                .filter(ldplayerState -> ldplayerState.getIndex() == index)
                .map(LdplayerState::isAndroidReady)
                .findFirst()
                .orElse(false);
    }

    /**
     * Install apk. If the ldplayer is not running, launch it.
     *
     * @throws InterruptedException  underlying command is interrupted
     *                               or interrupted when wait for android ready after launch
     * @throws CommandExecutionFailureException  underlying command execution failed due to output reading failure
     * @throws LdplayerFailureException  underlying command is executed but considered as failure
     *                                   according to exit value or output
     */
    public void installApk(int index, String apkPath)
            throws InterruptedException, LdplayerFailureException, CommandExecutionFailureException {

        if (ldconsole.isRunning(index)) {
            launchAndWaitAndroidReady(index);
        }

        while (!isAndroidReady(index)) {
            TimeUnit.SECONDS.sleep(10);
        }

        ldconsole.installApp(index, apkPath);
    }

    /**
     * Install certificate to Android by pushing to {@code /system/etc/security/cacerts/} and chmod all read permission.
     * If the ldplayer is not running, launch it first.
     *
     * @throws InterruptedException  underlying command is interrupted
     *                               or interrupted when wait for android ready after launch
     * @throws CommandExecutionFailureException  underlying command execution failed due to output reading failure
     * @throws LdplayerFailureException  underlying command is executed but considered as failure
     *                                   according to exit value or output
     */
    public void installCert(int index, String certPathStr)
            throws InterruptedException, LdplayerFailureException, CommandExecutionFailureException {

        if (ldconsole.isRunning(index)) {
            launchAndWaitAndroidReady(index);
        }

        while (!isAndroidReady(index)) {
            TimeUnit.SECONDS.sleep(10);
        }

        Path certFilenamePath = Paths.get(certPathStr).getFileName();
        Path remoteCertPath = Paths.get("/system/etc/security/cacerts/").resolve(certFilenamePath);
        String remoteCertPathStr = remoteCertPath.toString();
        ldconsole.push(index, certPathStr, remoteCertPathStr);
        ldconsole.adb(index, "shell chmod 644 " + remoteCertPathStr);
    }

    /**
     *
     * @throws InterruptedException  underlying command is interrupted
     *                               or interrupted when wait for android ready after launch
     * @throws CommandExecutionFailureException  underlying command execution failed due to output reading failure
     * @throws LdplayerFailureException  underlying command is executed but considered as failure
     *                                   according to exit value or output
     */
    public void launchAndWaitAndroidReady(int index)
            throws InterruptedException, LdplayerFailureException, CommandExecutionFailureException {

        ldconsole.launch(index);
        do {
            TimeUnit.SECONDS.sleep(10);
        } while (!isAndroidReady(index));
    }

    /**
     *
     * @throws InterruptedException  underlying command is interrupted
     *                               or interrupted when wait for quit
     * @throws CommandExecutionFailureException  underlying command execution failed due to output reading failure
     * @throws LdplayerFailureException  underlying command is executed but considered as failure
     *                                   according to exit value or output
     */
    public void quit(int index)
            throws InterruptedException, LdplayerFailureException, CommandExecutionFailureException {

        ldconsole.quit(index);
        do {
            TimeUnit.SECONDS.sleep(5);
        } while (ldconsole.isRunning(index));
    }

    /**
     * Set global http proxy.
     * If the ldplayer is not running, launch it first.
     * The http proxy setting take effects after reboot.
     *
     * @throws InterruptedException  underlying command is interrupted
     *                               or interrupted when wait for android ready after launch
     * @throws CommandExecutionFailureException  underlying command execution failed due to output reading failure
     * @throws LdplayerFailureException  underlying command is executed but considered as failure
     *                                   according to exit value or output
     */
    public void setHttpProxy(int index, String host, int port, String exclusion)
            throws InterruptedException, LdplayerFailureException, CommandExecutionFailureException {

        if (ldconsole.isRunning(index)) {
            launchAndWaitAndroidReady(index);
        }

        while (!isAndroidReady(index)) {
            TimeUnit.SECONDS.sleep(10);
        }

        ldconsole.putSetting(index, "global", "global_http_proxy_host", host);
        ldconsole.putSetting(index, "global", "global_http_proxy_port", String.valueOf(port));
        if (StringUtils.isNotBlank(exclusion)) {
            ldconsole.putSetting(index, "global", "global_http_proxy_exclusion_list", exclusion);
        }
    }
}
