package org.nicktorwald.platform.dice.service;

import java.util.Random;

import org.springframework.stereotype.Component;

/**
 * A standard Nth sided dice.
 */
@Component
class UniformNumericDice implements Dice<Integer> {

    private final Random numberGenerator;
    private final Integer sides;

    /**
     * Constructs a new numeric dice specifiedd by
     * a number of sides as well as a number generator.
     *
     * @param numberGenerator number provider
     * @param sides number of sides
     */
    UniformNumericDice(Random numberGenerator, Integer sides) {
        this.numberGenerator = numberGenerator;
        this.sides = sides;
    }

    @Override
    public Integer roll() {
        return numberGenerator.nextInt(sides) + 1;
    }

}

