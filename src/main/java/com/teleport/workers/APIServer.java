package com.teleport.workers;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.logging.Logger;
import java.util.StringJoiner;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.util.Map;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * API server implemented as a Java servlet
 * Provides an HTTP api to start, query, and stop workers
 *
 * IMPORTANT: This server will url filter to only receive requests that match /worker/* as specified in the design doc.
 *
 * TODO: TESTS! Since it is much more involved than the unit tests currently written, in the interest of time and simplicity for
 * this challenge I haven't written tests. This class is less critical than the workers and jobs/job managers as it is more dependent on
 * api specifications and how we want to handle dealing with invalid requests/errors.
 */
public class APIServer extends HttpServlet {

    private final static Logger LOGGER = Logger.getLogger(APIServer.class.getName());
    private final static int INDENT_SIZE = 4;
    private final static String file_path_key = "file_path";

    private JobManager manager;
    private String invalidRequest;
    private Map<Job.JobStatus, String> statusMap;

    @Override
    public void init() throws ServletException {
        manager = new JobManager();
        invalidRequest = new JSONObject().put("error", "invalid request").toString(INDENT_SIZE);
        statusMap = new HashMap<>();
        statusMap.put(Job.JobStatus.STOPPED, "stopped");
        statusMap.put(Job.JobStatus.FINISHED, "finished");
        statusMap.put(Job.JobStatus.RUNNING, "running");
    }

    /**
     * Helper functions
     */
    public String[] getPaths(HttpServletRequest request) {
        String requestUrl = request.getRequestURI();
        return requestUrl.split("/");
    }

    private void printAndFlush(PrintWriter out, String output) {
        out.print(output);
        out.flush();
    }

    private String buildJobJson(Job job) {
        if (job == null) {
            return new JSONObject().put("error", "job does not exist").toString(INDENT_SIZE);
        }

        String status = statusMap.get(job.getStatus());
        JSONObject outputJson = new JSONObject();
        try {
            outputJson = outputJson.put("status", status);
            outputJson = outputJson.put("command", job.getCommandString());
            if (job.getResult() != null) {
                outputJson = outputJson.put("output", job.getResult().getOutput());
            }
        } catch (JSONException e) {
            LOGGER.warning(String.format("Exception while creating JSONOjbect: %s", e.getMessage()));
        }
        return outputJson.toString(INDENT_SIZE);
    }

    private JSONObject getRequestData(HttpServletRequest request) throws IOException, JSONException {
        // TODO: Currently throwing JSONException for simplicity, but ideally should be more verbose and maybe use own exception or find
        // a more suitable exception in other libraries (Spring MVC etc.) so there's more granularity on error responses
        if (!request.getHeader("Content-Type").equals("application/json")) {
            throw new JSONException("Invalid Content-Type header");
        }

        StringJoiner jsonJoiner = new StringJoiner("");
        String line = null;
        BufferedReader reader = request.getReader();
        while ((line = reader.readLine()) != null) {
            jsonJoiner.add(line);
        }
        reader.close();
        return new JSONObject(jsonJoiner.toString());
    }

    /**
     * GET endpoints
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        PrintWriter out = response.getWriter();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Basic url filtering
        String[] paths = getPaths(request);
        if (paths.length < 4) {
            out.print(invalidRequest);
            return;
        }

        String endpoint = paths[2];
        Long pid = Long.parseLong(paths[3]);

        if (endpoint.equals("query")) {
            if (paths.length > 4) {
                printAndFlush(out, invalidRequest);
                return;
            }
            Job job = manager.queryJob(pid);
            out.print(buildJobJson(job));
        } else if (endpoint.equals("stop")) {
            if (paths.length > 4) {
                printAndFlush(out, invalidRequest);
                return;
            }
            manager.stopJob(pid);
            out.print(new JSONObject().toString(INDENT_SIZE));
        } else {
            out.print(invalidRequest);
        }
        out.flush();
    }

    /**
     * POST endpoints
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {

        PrintWriter out = response.getWriter();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Basic url filtering
        String[] paths = getPaths(request);
        if (!paths[2].equals("start")) {
            printAndFlush(out, invalidRequest);
            return;
        }

        JSONObject requestData;
        String command;
        try {
            requestData = getRequestData(request);
            command = requestData.getString(file_path_key);
        } catch (IOException | JSONException e) {
            // Exceptions are caught together for now just because our response is the same for both and is not critical
            // The error message in logging should determine which part of the process caused the exception (JSONObject creation vs stream reading)
            LOGGER.warning(String.format("Exception while retrieving request data: %s", e.getMessage()));
            printAndFlush(out, invalidRequest);
            return;
        }

        long pid = manager.addJob(new Job(command));
        if (pid == -1) {
            printAndFlush(out, new JSONObject().put("error", "could not execute command").toString(INDENT_SIZE));
            return;
        }

        printAndFlush(out, new JSONObject().put("pid", pid).toString(INDENT_SIZE));
    }
}
