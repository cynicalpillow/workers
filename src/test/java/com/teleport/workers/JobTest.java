package com.teleport.workers;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for Jobs
 */
public class JobTest {

    /**
     * Test command is properly parsed
     */
    @Test
    public void commandShouldParseCorrectly() {
        Job j = new Job("  ls   -a  -l  ");
        assertEquals(j.getCommand(), "ls -a -l");
    }

    /**
     * Test empty command is properly parsed
     */
    @Test
    public void emptyCommandShouldParseCorrectly() {
        Job j = new Job("");
        assertEquals(j.getCommand(), "");
    }
}
