package org.nicktorwald.platform.dice.service;

import java.util.Random;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

@DisplayName("A numeric dice")
class UniformNumericDiceTest {

    private Dice<Integer> numericDice;
    private Random random;

    @BeforeEach
    void setUp() {
        random = Mockito.mock(Random.class);
        numericDice = new UniformNumericDice(random, 6);
    }

    @Test
    @DisplayName("returns an expected value")
    void testGenerateValidNumber() {
        int lowBound = 1;
        int highBound = 6;
        BDDMockito.given(random.nextInt(Mockito.anyInt())).willReturn(0);
        var generatedNumber = numericDice.roll();
        Assertions.assertThat(generatedNumber).isBetween(lowBound, highBound);
    }

}