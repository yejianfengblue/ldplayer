package com.yejianfengblue.ldplayer;

import com.yejianfengblue.ldplayer.command.CommandFailureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LdplayerService {

    private final Ldconsole ldconsole;

    public Ldplayer create(LdplayerCreation ldplayerCreation) throws InterruptedException {

        Set<Integer> oldLdplayerIndexes = ldconsole.list().stream()
                .map(LdplayerState::getIndex)
                .collect(Collectors.toSet());

        ldconsole.copy(ldplayerCreation.getName(), ldplayerCreation.getFromIndex());

        Set<Integer> newLdplayerIndexes = ldconsole.list().stream()
                .map(LdplayerState::getIndex)
                .collect(Collectors.toCollection(HashSet::new));

        newLdplayerIndexes.removeAll(oldLdplayerIndexes);

        if (newLdplayerIndexes.size() == 1) {
            int newLdplayerIndex = newLdplayerIndexes.iterator().next();
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

            // http proxy, need restart, so put last
            if (StringUtils.isNotBlank(ldplayerCreation.getHttpProxyHost())
                    && null != ldplayerCreation.getHttpProxyPort()) {
                setHttpProxy(newLdplayerIndex,
                        ldplayerCreation.getHttpProxyHost(), ldplayerCreation.getHttpProxyPort(),
                        ldplayerCreation.getHttpProxyExclusionList());
            }

            // reboot or quit
            if (ldplayerCreation.isRunAfterCreate()) {

                ldconsole.reboot(newLdplayerIndex);
                while (!isAndroidReady(newLdplayerIndex)) {
                    TimeUnit.SECONDS.sleep(10);
                }
                newLdplayer.setRunning(true);
                newLdplayer.setAndroidReady(true);

            } else {
                ldconsole.quit(newLdplayerIndex);
            }

            return newLdplayer;

        } else {
            throw new CommandFailureException("New ldplayer index not found");
        }
    }

    public Optional<Ldplayer> get(int index) {

        return ldconsole.list().stream()
                .filter(ldplayerState -> ldplayerState.getIndex() == index)
                .map(ldplayerState -> new Ldplayer(
                        ldplayerState.getTitle(),
                        ldplayerState.getIndex(),
                        ldconsole.isRunning(index),
                        ldplayerState.isAndroidReady()))
                .findFirst();
    }

    public List<Ldplayer> getAll() {

        return ldconsole.list().stream()
                .map(ldplayerState -> new Ldplayer(
                        ldplayerState.getTitle(),
                        ldplayerState.getIndex(),
                        ldconsole.isRunning(ldplayerState.getIndex()),
                        ldplayerState.isAndroidReady()))
                .collect(Collectors.toList());
    }

    public boolean isAndroidReady(int index) {

        return ldconsole.list().stream()
                .filter(ldplayerState -> ldplayerState.getIndex() == index)
                .map(LdplayerState::isAndroidReady)
                .findFirst()
                .orElse(false);
    }

    public void installApk(int index, String apkPath) throws InterruptedException {

        if (ldconsole.isRunning(index)) {
            launchAndWaitAndroidReady(index);
        }

        while (!isAndroidReady(index)) {
            TimeUnit.SECONDS.sleep(10);
        }

        ldconsole.installApp(index, apkPath);
    }

    /**
     * Install certificate to Android by pushing to {@code /system/etc/security/cacerts/}
     *
     * @throws InterruptedException
     */
    public void installCert(int index, String certPath) throws InterruptedException {

        if (ldconsole.isRunning(index)) {
            launchAndWaitAndroidReady(index);
        }

        while (!isAndroidReady(index)) {
            TimeUnit.SECONDS.sleep(10);
        }

        ldconsole.push(index, certPath, "/system/etc/security/cacerts/");
    }

    public void launchAndWaitAndroidReady(int index) throws InterruptedException {

        ldconsole.launch(index);
        do {
            TimeUnit.SECONDS.sleep(10);
        } while (!isAndroidReady(index));
    }

    public void quit(int index) throws  InterruptedException {

        ldconsole.quit(index);
        do {
            TimeUnit.SECONDS.sleep(5);
        } while (ldconsole.isRunning(index));
    }

    public void setHttpProxy(int index, String host, int port, String exclusion) {

        ldconsole.putSetting(index, "global", "global_http_proxy_host", host);
        ldconsole.putSetting(index, "global", "global_http_proxy_port", String.valueOf(port));
        if (StringUtils.isNotBlank(exclusion)) {
            ldconsole.putSetting(index, "global", "global_http_proxy_exclusion_list", exclusion);
        }
    }


}
