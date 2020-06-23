package org.nicktorwald.platform.dice.config;

import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converts a number of dice sides to a textual representation
 * according to a template provided.
 */
@Component
class DiceSideConverter implements Converter<List<Integer>, String> {

    private final DiceProperties diceProperties;

    public DiceSideConverter(DiceProperties diceProperties) {
        this.diceProperties = diceProperties;
    }

    @Override
    public String convert(List<Integer> source) {
        var sides = source.stream()
                .map(value -> diceProperties.getSides().get(value - 1))
                .collect(Collectors.toList());
        return flatSides(sides);
    }

    private String flatSides(List<String> dices) {
        var slicedDices = dices.stream()
                .map(dice -> dice.split("\n"))
                .toArray(String[][]::new);

        StringJoiner lineJoiner = new StringJoiner("\n");
        int height = slicedDices[0].length;
        for (int i = 0; i < height; i++) {
            StringJoiner sliceJoiner = new StringJoiner(" ");
            for (var slicedDice : slicedDices) {
                sliceJoiner.add(slicedDice[i]);
            }
            lineJoiner.merge(sliceJoiner);
        }
        return lineJoiner.toString();
    }
}
