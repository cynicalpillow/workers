package com.teleport.workers;

/**
 * Result class
 * Immutable object that contains the corresponding output/error for an associated job
 */
public class Result {

    private String output;

    /**
    * Result constructor
    * Each Result must have an output/error string
    * Note: Error output is merged with standard output
    */
    public Result(String output) {
        this.output = output;
    }

    public String getOutput() {
        return output;
    }
}
