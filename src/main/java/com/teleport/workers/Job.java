package com.teleport.workers;

import java.util.Arrays;
import java.util.List;

/**
 * Job class
 * Represents an arbitrary Linux application or command
 *
 * Status values:
 * 0 - Not executing (finished or hasn't been queued)
 * 1 - Executing
 * -1 - Error (job couldn't be ran or another error occured while creating the process)
 */
public class Job {
    private List<String> command;
    private JobStatus status;
    private Result result;

    public enum JobStatus {
        STOPPED,
        FINISHED,
        RUNNING,
        ERROR
    }

    /**
    * Job constructor
    * Each job object must contain a command or file path to an application
    * This string is split by whitespace so that parameters are properly parsed
    * when creating the Process with ProcessBuilder
    */
    public Job(String command) {
        this.command = Arrays.asList(command.trim().split("\\s+"));
        this.status = JobStatus.STOPPED;
    }

    public List<String> getCommand() {
        return command;
    }

    public String getCommandString() {
        return String.join(" ", command);
    }

    public synchronized void setStatus(JobStatus status) {
        this.status = status;
    }

    public synchronized JobStatus getStatus() {
        return status;
    }

    /**
    * Synchronized methods to set/get the Result on Process termination
    * Workers will call this from a separate thread to store the result back into
    * the job they have been tasked with. This should only be set once, hence the null check.
    */
    public synchronized void setResult(Result result) throws IllegalStateException {
        if (this.result != null) {
            throw new IllegalStateException("Result already assigned");
        }
        this.result = result;
    }

    public synchronized Result getResult() {
        return result;
    }
}
