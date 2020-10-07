package com.yejianfengblue.ldplayer;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class Ldplayer {

    String name;

    int index;

    boolean running;

    boolean androidReady;

    public Ldplayer(int index) {
        this.index = index;
        this.running = false;
        this.androidReady = false;
    }
}
