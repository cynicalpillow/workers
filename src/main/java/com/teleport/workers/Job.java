package com.teleport.workers;

import java.util.Arrays;
import java.util.List;

/**
 * Job class
 * Represents an arbitrary Linux application or command
 */
public class Job {
    private List<String> command;
    private int status;
    private Result result;

    /**
    * Job constructor
    * Each job object must contain a command or file path to an application
    * This string is split by whitespace so that parameters are properly parsed
    * when creating the Process with ProcessBuilder
    */
    public Job(String command) {
        this.command = Arrays.asList(command.trim().split("\\s+"));
    }

    public String getCommand() {
        return String.join(" ", command);
    }

    public int getStatus() {
        return status;
    }

    /**
    * Synchronized methods to set/get the Result on Process termination
    * Workers will call this from a separate thread to store the result back into
    * the job they have been tasked with. This should only be set once, hence the null check.
    */
    public synchronized void setResult(Result result) {
        if (this.result == null) {
            this.result = result;
        }
    }

    public synchronized Result getResult() {
        return result;
    }
}
