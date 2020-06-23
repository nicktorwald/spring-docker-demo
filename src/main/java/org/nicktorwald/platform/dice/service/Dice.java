package org.nicktorwald.platform.dice.service;

/**
 * Throw it if you are unsure about something.
 *
 * @param <V> target value of dice side
 */
public interface Dice<V> {

    /**
     * Rolls this dice to get an answer.
     *
     * @return thrown dice side
     */
    V roll();

}
