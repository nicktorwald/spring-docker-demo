package org.nicktorwald.platform.dice.config;

import java.util.Random;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * Configures the dice services.
 */
@Configuration
@EnableWebFlux
@EnableConfigurationProperties(DiceProperties.class)
class DiceServiceConfig implements WebFluxConfigurer {

    private final DiceProperties diceProperties;

    DiceServiceConfig(DiceProperties diceProperties) {
        this.diceProperties = diceProperties;
    }

    @Bean
    Random defaultNumberGenerator() {
        return new Random();
    }

    @Bean
    Integer defaultDiceSidesNumber() {
        return diceProperties.getSidesNumber();
    }

}
