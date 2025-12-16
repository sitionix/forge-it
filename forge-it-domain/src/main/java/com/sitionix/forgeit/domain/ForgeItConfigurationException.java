package com.sitionix.forgeit.domain;

/**
 * Thrown when ForgeIT is misconfigured for the current runtime scenario.
 */
public class ForgeItConfigurationException extends RuntimeException {

    public ForgeItConfigurationException(final String message) {
        super(message);
    }
}
