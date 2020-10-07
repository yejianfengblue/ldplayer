package com.yejianfengblue.ldplayer;

import lombok.Value;

import java.util.List;

@Value
public class LdplayerCreation {

    String name;

    Integer fromIndex;

    boolean runAfterCreate;

    List<String> installApkPaths;

    List<String> installCertPaths;

    String httpProxyHost;

    Integer httpProxyPort;

    /**
     * comma separated list like ".baidu.com,.google.com"
     */
    String httpProxyExclusionList;

    String manufacturer;

    String model;
}
