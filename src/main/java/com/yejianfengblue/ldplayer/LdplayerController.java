package com.yejianfengblue.ldplayer;

import com.yejianfengblue.ldplayer.command.CommandExecutionFailureException;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
@RequestMapping(LdplayerLinks.LDPLAYERS)
@RequiredArgsConstructor
public class LdplayerController {

    private final LdplayerService ldplayerService;

    private final LdplayerModelAssembler ldplayerModelAssembler;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    ResponseEntity<CollectionModel<EntityModel<Ldplayer>>> getAll()
            throws InterruptedException, CommandExecutionFailureException, LdplayerFailureException {
        return ResponseEntity.ok(ldplayerModelAssembler.toCollectionModel(ldplayerService.getAll()));
    }

    @PostMapping(produces = MediaTypes.HAL_JSON_VALUE)
    ResponseEntity<EntityModel<Ldplayer>> create(@RequestBody LdplayerCreation ldplayerCreation)
            throws InterruptedException, CommandExecutionFailureException, LdplayerFailureException {

        Ldplayer ldplayer = ldplayerService.create(ldplayerCreation);
        EntityModel<Ldplayer> ldplayerModel = ldplayerModelAssembler.toModel(ldplayer);
        return ResponseEntity
                .created(ldplayerModel.getRequiredLink(IanaLinkRelations.SELF).toUri())
                .body(ldplayerModel);
    }

    @GetMapping(path = "/{index}", produces = MediaTypes.HAL_JSON_VALUE)
    ResponseEntity<EntityModel<Ldplayer>> getOne(@PathVariable int index)
            throws InterruptedException, CommandExecutionFailureException, LdplayerFailureException {

        Optional<Ldplayer> ldplayer = ldplayerService.get(index);
        return ldplayer.map(value -> ResponseEntity.ok(ldplayerModelAssembler.toModel(value)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping(path = "/{index}/" + LdplayerLinks.LAUNCH, produces = MediaTypes.HAL_JSON_VALUE)
    ResponseEntity<EntityModel<Ldplayer>> launch(@PathVariable int index)
            throws InterruptedException, CommandExecutionFailureException, LdplayerFailureException {

        Optional<Ldplayer> ldplayer = ldplayerService.get(index);
        if (ldplayer.isPresent()) {

            ldplayerService.launchAndWaitAndroidReady(index);
            ldplayer = ldplayerService.get(index);
            return ResponseEntity.ok(ldplayerModelAssembler.toModel(ldplayer.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping(path = "/{index}/" + LdplayerLinks.QUIT, produces = MediaTypes.HAL_JSON_VALUE)
    ResponseEntity<EntityModel<Ldplayer>> quit(@PathVariable int index)
            throws InterruptedException, CommandExecutionFailureException, LdplayerFailureException {

        Optional<Ldplayer> ldplayer = ldplayerService.get(index);
        if (ldplayer.isPresent()) {
            try {
                ldplayerService.quit(index);
                ldplayer = ldplayerService.get(index);
                return ResponseEntity.ok(ldplayerModelAssembler.toModel(ldplayer.get()));

            } catch (InterruptedException interruptedException) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Quit is interrupted");
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

}
