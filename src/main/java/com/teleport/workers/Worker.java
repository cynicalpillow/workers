package com.teleport.workers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.StringJoiner;

/**
 * Worker class
 * Abstraction for a worker process
 * Workers will create a new Process with a specified command and
 * read the output/error streams using a separate thread. Once finished, the
 * Result is stored into the associated Job for the Worker.
 */
public class Worker implements Runnable {

    private final static Logger LOGGER = Logger.getLogger(Worker.class.getName());
    private Process process;
    private Job job;

    private String readStream(BufferedReader stream) throws IOException {
        StringJoiner joiner = new StringJoiner("\n");
        String line = null;
        while ((line = stream.readLine()) != null) {
            joiner.add(line);
        }
        return joiner.toString();
    }

    @Override
    public void run() {
        BufferedReader outputStream = new BufferedReader(new InputStreamReader(process.getInputStream()));
        try {
            String output = readStream(outputStream);
            job.setResult(new Result(output.toString()));
        } catch (IOException e) {
            // This could happen when we stop the process
            LOGGER.warning(String.format("Exception while reading stream: %s", e.getMessage()));
        } catch (IllegalArgumentException e) {
            LOGGER.severe(String.format("Exception while setting status: %s", e.getMessage()));
        } catch (IllegalStateException e) {
            LOGGER.warning(String.format("Exception while assigning result: %s", e.getMessage()));
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                LOGGER.severe(String.format("Could not close streams: %s", e.getMessage()));
            }
            // clean up zombies
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                LOGGER.warning(String.format("Thread interrupted while waiting for process termination: %s", e.getMessage()));
                stopProcess();
            }
            job.setStatus(0);
        }
    }

    /**
     * Start new process and redirect error stream to standard output
     * Redirection lets us avoid creating a new thread and also makes it easier to implement
     * the API server later on
     */
    public long execute(Job job) {
        this.job = job;

        ProcessBuilder builder = new ProcessBuilder(job.getCommand());
        builder.redirectErrorStream(true);
        try {
            process = builder.start();
        } catch (IOException e) {
            LOGGER.severe(String.format("Could not start process: %s", e.getMessage()));
            this.job.setStatus(-1);
            return -1;
        }

        job.setStatus(1);
        new Thread(this).start();
        return process.toHandle().pid();
    }

    /**
     * To make sure the process is stopped completely, we set a timeout of 2s on destroy.
     * If the process is still alive then we forcibly destroy it.
     */
    public void stopProcess() {
        process.destroy();
        try {
            if (!process.waitFor(2000, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            LOGGER.warning(String.format("Thread interrupted while destroying process: %s", e.getMessage()));
            process.destroyForcibly();
        } finally {
            job.setStatus(0);
        }
    }

    public Job getJob() {
        return job;
    }

    public long getPID() {
        if (process == null) {
            return -1;
        }
        return process.toHandle().pid();
    }

    public Process getProcess() {
        return process;
    }
}
