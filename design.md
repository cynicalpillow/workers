# Worker Library Design Doc

This document will describe a high level overview of a Java worker library with methods to start/stop/query status and get an output of a running job through a REST API.

## API

To keep things simple, the REST API will have no authentication. Endpoints return JSON data in the response.

**Endpoint to start a worker to run a Linux application with the specified file path:**

    POST /worker/start
**Parameters**
| Name | Type | In | Description |
|:--|:--|:--|:--|
| file_path | string | body | File path of the Linux application to run or command plus parameters |

**Headers**
| Key | Value |
|:--|:--|
| Content-Type | application/json |

Responses will be of the form:
```json
{
	"pid": number
}
```

**NOTE:** Shell constructs will not work (ie. ls -l | grep "\.txt$")

**Endpoint for retrieving information on the status of a running process/worker with a specified process id integer:**

    GET /worker/query/<pid>

Responses will be of the form:
```json
{
	"output": "string",
	"command": "string",
	"status": "string"
}
```
**NOTE:** Output string is a JSON string so cannot be multi-line.

**Endpoint for stopping a running process/worker with a specified process id integer:**

    GET /worker/stop/<pid>

For simplicity, this endpoint will return an empty JSON response since stopping the worker can take some time. Querying the worker later will return the updated status.


## Design

Fundamentally, we’ll have a concept of a job, process/worker pool, job manager and a result. Similar to how many other workers libraries function, there will be a job manager process that will determine which worker/process runs a job at a given time. Workers function by launching a new process of the Linux application and will use a secondary thread to read back the output to store the result back into the Job object. This pool of workers will automatically increase/decrease depending on the load. The Java servlet responsible for handling API requests can then ask for jobs from the manager and receive information on whether the job is finished, running, or stopped. If the job is finished, there will be a corresponding Result object that can be accessed for output. This information is then returned through the REST API as JSON.

![](https://i.imgur.com/QFnZZfa.jpg)

## Interfaces

**Result**

Result will be an immutable object that contains the corresponding output/error for an associated job.

**NOTE:** Error stream is redirected to output stream in implementation so we don't have to create an extra stream to read the errors.
- Properties
	- Output (string)
- Methods
	- getOutput() -> string

**Worker**

Each worker will hold references to the job it's running and the current process that is executing the application. With Java, we can acquire a ProcessHandle that will provide output and error streams that we can read from. Each worker will implement Runnable, and will start a new thread that will be responsible for reading from these streams as output is created from the process. After the process is finished, we then acquire a lock to write to the Job we're currently executing and assign the newly created Result. We don't have to worry about writes happening at the same time since each worker will be running separate jobs, but we do need to worry about the order of reads vs writes. Since a client program could depend on the Result of the Job, and if there isn't a Result at the time of reading (but the Job is already finished) it could introduce a deadlock even if the library is completely fine.
- Properties
	- Current Job reference (Job)
	- Process reference (Process)
- Methods
	- execute(job) -> long (pid)
	- getPID() -> long
	- getJob() -> Job
	- getProcess() -> Process
	- stopProcess() -> void

**Job**

Job is an abstraction of the application that's needed to run.
- Properties
	- Application to run (List[String])
	- Result (Result)
	- Status (JobStatus enum)
- Methods
	- getCommand() -> List[String]
	- getCommandString() -> String
	- getResult() -> Result
	- setResult(Result) -> void (Will only set the result once)
	- getStatus() -> JobStatus
	- setStatus(JobStatus) -> void

**Job Manager**

The Job Manager will be responsible for creating the Worker needed to run each Job. It will also store the mapping for each of the PID -> Job, PID -> Worker associations needed for queries.
- Properties
	- PID/Job mapping to retrieve results (Map)
	- PID/Worker mapping to stop process (Map)
- Methods
	- addJob(string) -> long (pid)
	- queryJob(long) -> Job
	- stopJob(long) -> void

## Tradeoffs and TODO
- The servlet is also a basic implementation that will not be appropriate for a production system. A better solution for the future is to convert it into a Java Spring application.
- Error and output streams are combined, which may or may not be ideal. If we want to read in error streams separately then we'll need to use a separate thread which introduces more complexity.
- URL filters for the endpoints are very basic, meaning more complex queries will not be allowed. This can be improved for the future.
- JobStatus is very basic, and only represents STOPPED, FINISHED, RUNNING, ERROR and does not provide any description. For the future, we can add a description method to properly describe these statuses and add more statuses as needed.
- Uses Tomcat to serve Servlet, which may or may not be ideal. Better configuration is needed.
- Using PID values as identification for jobs and workers could be detrimental for a larger scale application with many workers and jobs as the PID values will eventually be overwritten. This can result in lost Job results and losing references to Workers. For the future, we should use some kind of hash for id on a Job/Worker to unique identify it.
- Have not added tests for the API server because it seemed out of scope for this challenge as it wasn't the most critical part of this library. For the future, integration and unit tests are needed.
- On stops, we don't return any output. This means output is only provided once the target application is finished running and isn't interrupted. We can modify this library to return output as it's generated for the future.
