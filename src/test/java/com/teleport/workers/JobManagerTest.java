package com.teleport.workers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * Unit tests for JobManager
 */
public class JobManagerTest {

    /**
     * Test manager correctly returns results
     */
    @Test
    public void jobManagerStartsJobAndMapsResult() throws InterruptedException {
        JobManager manager = new JobManager();
        Job job1 = new Job("ls -l");
        Job job2 = new Job("echo foobar");
        Job job3 = new Job("pwd");
        manager.addJob(job1);
        long pid = manager.addJob(job2);
        manager.addJob(job3);
        WorkerTest.waitOnStatus(job2, 5);
        Result result = manager.queryJob(pid).getResult();
        assertNotNull(result);
        assertEquals(result.getOutput(), "foobar");
        assertEquals(job2.getStatus(), Job.JobStatus.FINISHED);
    }
}
