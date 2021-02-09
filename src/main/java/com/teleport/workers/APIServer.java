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
 */
public class APIServer extends HttpServlet {

    private final static Logger LOGGER = Logger.getLogger(APIServer.class.getName());
    private final static int INDENT_SIZE = 4;

    private JobManager manager;
    private String invalidRequest;
    private Map<Job.JobStatus, String> statusMap;
    private String file_path_key;

    @Override
    public void init() throws ServletException {
        manager = new JobManager();
        invalidRequest = new JSONObject().put("error", "invalid request").toString(INDENT_SIZE);
        file_path_key = "file_path";
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

    private String buildJobJson(Job job, boolean raw) {
        if (raw && (job == null || job.getResult() == null)) {
            return "";
        }
        if (raw) {
            return job.getResult().getOutput();
        }
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

    private JSONObject getRequestData(HttpServletRequest request) {
        StringJoiner jsonJoiner = new StringJoiner("");
        String line = null;
        JSONObject jsonObject = null;
        try {
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                jsonJoiner.add(line);
            }
            jsonObject = new JSONObject(jsonJoiner.toString());
        } catch (IOException e) {
            LOGGER.warning(String.format("Exception while reading request stream: %s", e.getMessage()));
        } catch (JSONException e) {
            LOGGER.warning(String.format("Exception while creating JSONOjbect: %s", e.getMessage()));
        }
        return jsonObject;
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

        String[] paths = getPaths(request);
        if (paths.length < 4) {
            out.print(invalidRequest);
            return;
        }

        String endpoint = paths[2];
        Long pid = Long.parseLong(paths[3]);

        if (endpoint.equals("stop") && paths.length > 4) {
            printAndFlush(out, invalidRequest);
            return;
        }

        if (endpoint.equals("query")) {
            boolean raw = false;
            if (paths.length == 5 && paths[4].equals("raw")) {
                raw = true;
            } else if (paths.length > 4) {
                printAndFlush(out, invalidRequest);
                return;
            }
            Job job = manager.queryJob(pid);
            out.print(buildJobJson(job, raw));
        } else if (endpoint.equals("stop")) {
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

        String[] paths = getPaths(request);
        if (!paths[2].equals("start")) {
            printAndFlush(out, invalidRequest);
            return;
        }

        JSONObject requestData = getRequestData(request);
        if (requestData == null || !requestData.has(file_path_key)) {
            printAndFlush(out, invalidRequest);
            return;
        }

        String command = "";
        try {
            command = requestData.getString(file_path_key);
        } catch (JSONException e) {
            LOGGER.warning(String.format("Exception while retrieving command string: %s", e.getMessage()));
        }
        long pid = manager.addJob(new Job(command));

        if (pid == -1) {
            printAndFlush(out, new JSONObject().put("error", "could not execute command").toString(INDENT_SIZE));
            return;
        }

        printAndFlush(out, new JSONObject().put("pid", pid).toString(INDENT_SIZE));
    }
}
