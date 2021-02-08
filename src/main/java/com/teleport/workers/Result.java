package com.teleport.workers;

/**
 * Result class
 * Immutable object that contains the corresponding output/error for an associated job
 */
public class Result {

    private String output;
    private String error;

    /**
    * Result constructor
    * Each Result must have an output string and an error string
    */
    public Result(String output, String error) {
        this.output = output;
        this.error = error;
    }

    public String getOutput() {
        return output;
    }

    public String getError() {
        return error;
    }
}
