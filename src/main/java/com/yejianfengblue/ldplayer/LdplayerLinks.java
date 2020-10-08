package com.yejianfengblue.ldplayer;

import lombok.SneakyThrows;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

public class LdplayerLinks {

    public static final String LDPLAYERS = "ldplayers";

    static final String LDPLAYER = "ldplayer";

    static final String LAUNCH = "launch";

    static final String QUIT = "quit";

    public static final LinkRelation LDPLAYERS_REL = LinkRelation.of(LDPLAYERS);

    static final LinkRelation LDPLAYER_REL = LinkRelation.of(LDPLAYER);

    static final LinkRelation LAUNCH_REL = LinkRelation.of(LAUNCH);

    static final LinkRelation QUIT_REL = LinkRelation.of(QUIT);

    @SneakyThrows
    static Link launchLink(Ldplayer ldplayer) {

        return linkTo(methodOn(LdplayerController.class).launch(ldplayer.getIndex()))
                .withRel(LAUNCH_REL);
    }

    @SneakyThrows
    static Link stopLink(Ldplayer ldplayer) {

        return linkTo(methodOn(LdplayerController.class).quit(ldplayer.getIndex()))
                .withRel(QUIT_REL);
    }
}
