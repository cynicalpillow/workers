package com.teleport.workers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * Unit tests for Workers
 */
public class WorkerTest {

    private boolean waitOnProcess(Process process) throws InterruptedException {
        if (!process.waitFor(5000, TimeUnit.MILLISECONDS)) {
            // Test processes should finish reasonably fast
            // Otherwise something probably went wrong
            return false;
        }
        return true;
    }

    public static void waitOnStatus(Job job, int secs) throws InterruptedException {
        int count = secs * 2;
        while (job.getStatus() == Job.JobStatus.RUNNING && count > 0) {
            TimeUnit.MILLISECONDS.sleep(500);
            --count;
        }
    }

    /**
     * Test worker reads output streams correctly
     */
    @Test
    public void workerShouldCreateResult() throws InterruptedException {
        Worker worker = new Worker();
        Job validJob = new Job("echo foobar");
        assertEquals(validJob.getStatus(), Job.JobStatus.STOPPED);
        worker.execute(validJob);
        Process process = worker.getProcess();
        assertTrue(waitOnProcess(process));
        // Some delay in saving results as process finishes
        // Give another 5 secs max to make sure result is received
        WorkerTest.waitOnStatus(validJob, 5);
        assertEquals(validJob.getResult().getOutput(), "foobar");
        assertEquals(validJob.getStatus(), Job.JobStatus.FINISHED);
    }

    /**
     * Test worker reads error streams correctly
     */
    @Test
    public void workerShouldCreateErrorResult() throws InterruptedException {
        Worker worker = new Worker();
        Job errorJob = new Job("./test_apps/error.sh");
        worker.execute(errorJob);
        Process process = worker.getProcess();
        assertTrue(waitOnProcess(process));
        WorkerTest.waitOnStatus(errorJob, 5);
        assertEquals(errorJob.getResult().getOutput(), "error message");
        assertEquals(errorJob.getStatus(), Job.JobStatus.FINISHED);
    }

    /**
     * Test worker stops process
     */
    @Test
    public void workerShouldStop() throws InterruptedException {
        Worker worker = new Worker();
        // Job runs for 100 secs
        Job longJob = new Job("./test_apps/loop.sh");
        worker.execute(longJob);
        Process process = worker.getProcess();
        worker.stopProcess();
        assertTrue(waitOnProcess(process));
        // Leave time for handling stream closing
        WorkerTest.waitOnStatus(longJob, 5);
        assertTrue(process.isAlive() == false);
        assertEquals(longJob.getStatus(), Job.JobStatus.STOPPED);
    }

    /**
     * Test workers should not overwrite Results
     */
    @Test
    public void workerShouldNotOverwriteResult() throws InterruptedException {
        Worker worker = new Worker();
        Job timeJob = new Job("time");
        worker.execute(timeJob);

        Process process = worker.getProcess();
        assertTrue(waitOnProcess(process));
        WorkerTest.waitOnStatus(timeJob, 5);

        String output = timeJob.getResult().getOutput();

        TimeUnit.MILLISECONDS.sleep(10);
        Worker worker2 = new Worker();
        worker2.execute(timeJob);

        process = worker2.getProcess();
        assertTrue(waitOnProcess(process));
        WorkerTest.waitOnStatus(timeJob, 5);

        assertEquals(output, timeJob.getResult().getOutput());
        assertEquals(timeJob.getStatus(), Job.JobStatus.FINISHED);
    }

    /**
     * Test invalid command should set status to -1
     */
    @Test
    public void workerShouldNotRunInvalidCommand() throws InterruptedException {
        Worker worker = new Worker();
        Job job = new Job("foobar123app");
        worker.execute(job);

        assertNull(worker.getProcess());
        assertEquals(job.getStatus(), Job.JobStatus.ERROR);
    }
}
