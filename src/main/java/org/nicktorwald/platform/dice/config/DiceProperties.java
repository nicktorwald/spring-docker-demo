package org.nicktorwald.platform.dice.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * Configuration properties for dices.
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "dice")
class DiceProperties {

    /**
     * Number of dice sides.
     */
    private final Integer sidesNumber;

    /**
     * Multiline textual representation of each dice side.
     */
    private final List<String> sides;

    public DiceProperties(Integer sidesNumber, List<String> sides) {
        this.sidesNumber = sidesNumber;
        this.sides = sides;
    }

    public Integer getSidesNumber() {
        return sidesNumber;
    }

    public List<String> getSides() {
        return sides;
    }

}
