package com.yejianfengblue.ldplayer;

import lombok.SneakyThrows;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
class LdplayerModelAssembler implements RepresentationModelAssembler<Ldplayer, EntityModel<Ldplayer>> {

    @SneakyThrows
    @Override
    public EntityModel<Ldplayer> toModel(Ldplayer ldplayer) {

        EntityModel<Ldplayer> model = EntityModel.of(ldplayer,
                linkTo(methodOn(LdplayerController.class).getOne(ldplayer.getIndex())).withSelfRel());

        if (ldplayer.isRunning()) {
            model.add(LdplayerLinks.stopLink(ldplayer));
        } else {
            model.add(LdplayerLinks.launchLink(ldplayer));
        }

        return model;
    }

}
