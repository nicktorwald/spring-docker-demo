package org.nicktorwald.platform.dice.api;

import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.BDDMockito;
import org.nicktorwald.platform.dice.config.DiceMediaType;
import org.nicktorwald.platform.dice.service.Dice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@DisplayName("The dice endpoint")
@WebFluxTest(DiceEndpoint.class)
class DiceEndpointTest {

    private static final String DICE_ROLL_PATH = "/dice/roll";

    @Autowired
    private WebTestClient webClient;

    @MockBean
    private Dice<Integer> dice;

    @ParameterizedTest(name = "returns an expected dice #{0}")
    @MethodSource("provideDiceResults")
    void testRollAllDices(Integer diceNumber, String expectedResult) {
        BDDMockito.given(dice.roll()).willReturn(diceNumber);
        webClient.get().uri(DICE_ROLL_PATH)
                .accept(DiceMediaType.APPLICATION_DICE)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo(expectedResult);
    }

    @Test
    @DisplayName("returns three dices at once")
    void testRollDiceThreeTimes() {
        var expectedDices =
                "+-------+ +-------+ +-------+\n" +
                "| o   o | | o   o | | o     |\n" +
                "|       | |   o   | |   o   |\n" +
                "| o   o | | o   o | |     o |\n" +
                "+-------+ +-------+ +-------+";
        BDDMockito.given(dice.roll()).willReturn(4, 5, 3);
        webClient.get().uri(uriBuilder -> uriBuilder.path(DICE_ROLL_PATH).queryParam("times", 3).build())
                .accept(DiceMediaType.APPLICATION_DICE)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo(expectedDices);
    }

    @ParameterizedTest(name = "returns an error when {0} times is requested")
    @ValueSource(ints = {Integer.MIN_VALUE, 2031, -10, 0, 6, 10, 1502, Integer.MAX_VALUE})
    void testRollAllDices(Integer wrongTimes) {
        webClient.get().uri(uriBuilder -> uriBuilder.path(DICE_ROLL_PATH).queryParam("times", wrongTimes).build())
                .accept(DiceMediaType.APPLICATION_DICE)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class)
                .value(Matchers.containsString("We cannot roll a dice chosen times!"));
    }

    private static Stream<Arguments> provideDiceResults() {
        return Stream.of(
                Arguments.of(
                        1,
                        "+-------+\n" +
                        "|       |\n" +
                        "|   o   |\n" +
                        "|       |\n" +
                        "+-------+"
                ),
                Arguments.of(
                        2,
                        "+-------+\n" +
                        "| o     |\n" +
                        "|       |\n" +
                        "|     o |\n" +
                        "+-------+"
                ),
                Arguments.of(
                        3,
                        "+-------+\n" +
                        "| o     |\n" +
                        "|   o   |\n" +
                        "|     o |\n" +
                        "+-------+"
                ),
                Arguments.of(
                        4,
                        "+-------+\n" +
                        "| o   o |\n" +
                        "|       |\n" +
                        "| o   o |\n" +
                        "+-------+"
                ),
                Arguments.of(
                        5,
                        "+-------+\n" +
                        "| o   o |\n" +
                        "|   o   |\n" +
                        "| o   o |\n" +
                        "+-------+"
                ),
                Arguments.of(
                        6,
                        "+-------+\n" +
                        "| o   o |\n" +
                        "| o   o |\n" +
                        "| o   o |\n" +
                        "+-------+"
                )
        );
    }

}