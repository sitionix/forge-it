package com.sitionix.forgeit.domain;

/**
 * Domain contract for retrieving JSON content shared across ForgeIT modules.
 */
public interface JsonLoader {

    /**
     * Loads JSON content.
     *
     * @return JSON payload as a string
     */
    String load();
}
