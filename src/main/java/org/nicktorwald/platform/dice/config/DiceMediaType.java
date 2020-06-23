package org.nicktorwald.platform.dice.config;

import org.springframework.http.MediaType;

/**
 * Specifies custom media types for dices.
 */
public class DiceMediaType {

    public static final MediaType APPLICATION_DICE;
    public static final String APPLICATION_DICE_VALUE = "application/vnd.dice+text";

    static {
        APPLICATION_DICE = new MediaType("application", "vnd.dice+text");
    }

}
