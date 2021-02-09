package com.teleport.workers;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Job Manager class
 * Responsible for creating the Worker needed to run each Job. Each Job is associated with one worker.
 * Used by API server to start Jobs, get Job results/status, and stop running processes
 *
 * NOTE: This implementation is dependent on the pids being unique, which isn't the case. For a small number of processes,
 * it should be fine, but ideally for a larger system, there needs to be another form of ID to uniquely identify jobs.
 * In the current implementation an old job's result will eventually get replaced as the pids get reused. Since there's a low limit
 * on pid values, the worker map will not have deletions and will eventually just auto replace itself as pids get reused.
 */
public class JobManager {
    private ConcurrentMap<Long, Job> jobs;
    private ConcurrentMap<Long, Worker> workers;

    /**
    * Each Job Manager has mappings for workers and jobs
    */
    public JobManager() {
        jobs = new ConcurrentHashMap<>();
        workers = new ConcurrentHashMap<>();
    }

    /**
    * Adding a job should return a corresponding PID that can be used for
    * later queries or for stopping the worker process.
    *
    * NOTE: Context switches between threads could cause issues if the pids are the same.
    */
    public synchronized long addJob(Job job) {
        Worker worker = new Worker();
        long pid = worker.execute(job);
        if (pid == -1) {
            return pid;
        }

        jobs.put(pid, job);
        workers.put(pid, worker);
        return pid;
    }

    /**
    * Returns the corresponding job. API server will be responsible for creating
    * the appropriate response and transforming the integer statuses to meaningful string statuses.
    */
    public synchronized Job queryJob(long pid) {
        return jobs.get(pid);
    }

    public void stopJob(long pid) {
        Worker worker = workers.get(pid);
        if (worker == null) {
            return;
        }
        worker.stopProcess();
    }
}
