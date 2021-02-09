package com.teleport.workers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

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
        assertArrayEquals(j.getCommand().toArray(), new String[]{"ls", "-a", "-l"});
        assertEquals(j.getCommandString(), "ls -a -l");
    }

    /**
     * Test empty command is properly parsed
     */
    @Test
    public void emptyCommandShouldParseCorrectly() {
        Job j = new Job("");
        assertArrayEquals(j.getCommand().toArray(), new String[]{""});
        assertEquals(j.getCommandString(), "");
    }

    /**
     * Test job with assigned result should throw exception
     */
    @Test(expected = IllegalStateException.class)
    public void settingFinishedJobShouldThrowException() {
        Job j = new Job("");
        j.setResult(new Result(""));
        j.setResult(new Result("invalid"));
    }

    /**
     * Test job with invalid status should throw exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void settingJobWithInvalidStatusShouldThrowException() {
        Job j = new Job("");
        j.setStatus(2);
    }
}
