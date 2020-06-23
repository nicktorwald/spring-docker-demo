package org.nicktorwald.platform.dice.api;

import java.util.List;

import org.nicktorwald.platform.dice.config.DiceMediaType;
import org.nicktorwald.platform.dice.service.Dice;
import org.springframework.core.convert.converter.Converter;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * API entry point to deal with the service.
 */
@Validated
@RestController
@RequestMapping("/dice/roll")
class DiceEndpoint {
    private final Dice<Integer> dice;
    private final Converter<List<Integer>, String> diceSideConverter;

    DiceEndpoint(Dice<Integer> dice, Converter<List<Integer>, String> diceSideConverter) {
        this.dice = dice;
        this.diceSideConverter = diceSideConverter;
    }

    @GetMapping(produces = DiceMediaType.APPLICATION_DICE_VALUE)
    Mono<String> rollDice(
            @ValidRollTimes @RequestParam(required = false, defaultValue = "1") Integer times
    ) {
        return Flux
                .range(1, times)
                .map(i -> dice.roll())
                .collectList()
                .map(diceSideConverter::convert);
    }

}
