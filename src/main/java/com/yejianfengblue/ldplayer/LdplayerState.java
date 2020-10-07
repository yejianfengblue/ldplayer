package com.yejianfengblue.ldplayer;

import lombok.Value;

/**
 * Store each line of output of {@code ldconsole list2}
 */
@Value
class LdplayerState {

    int index;

    String title;
//
//    int topWindowHandle;
//
//    int bindingWindowHandle;

    boolean androidReady;

//    int pid;
//
//    int vboxPid;

}
