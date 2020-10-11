package com.yejianfengblue.ldplayer;

import lombok.Value;

import java.util.List;

@Value
public class LdplayerCreation {

    String name;

    Integer fromIndex;

    boolean runAfterCreate;

    List<String> installApkPaths;

    String manufacturer;

    String model;
}
