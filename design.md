# Worker Library Design Doc

This document will describe a high level overview of a Java worker library with methods to start/stop/query status and get an output of a running job through a REST API.

## API

To keep things simple, the REST API will have no authentication. Endpoints return JSON data in the response.

    POST /start
**Parameters**
| Name | Type  | In | Description |
|:--|:--|:--|:--|
| file_path | string | body | File path of the Linux application to run or command plus parameters |

Note: Shell constructs will not work (ie. ls -l | grep "\.txt$")

Endpoint to start a worker to run a Linux application with the specified file path.

    GET/query/<pid>
Endpoint for retrieving information on the status of a running process/worker with a specified process id integer.
  
    GET/stop/<pid>
Endpoint for stopping a running process/worker with a specified process id integer.

## Design

Fundamentally, we’ll have a concept of a job, process/worker pool, job manager and a result. Similar to how many other workers libraries function, there will be a job manager process that will determine which worker/process runs a job at a given time. Workers function by launching a new process of the Linux application and will use a secondary thread to read back the output to store the result back into the Job object. This pool of workers will automatically increase/decrease depending on the load. The Java servlet responsible for handling API requests can then ask for jobs from the manager and receive information on whether the job is finished, running, or not queued yet. If the job is finished, there will be a corresponding Result object that can be accessed for output and worker ID information. This information is then returned through the REST API as JSON. 

![](https://i.imgur.com/QFnZZfa.jpg)

## Interfaces

**Result**

Result will be an immutable object that contains the corresponding output/error for an associated job.
- Properties
	- Output (string)
	- Error output (string)
- Methods
	- getOutput() -> string
	- getError() -> string

**Worker**

Each worker will hold references to the job it's running and the current process that is executing the application. With Java, we can acquire a ProcessHandle that will provide output and error streams that we can read from. Each worker will implement Runnable, and will start a new thread that will be responsible for reading from these streams as output is created from the process. After the process is finished, we then acquire a lock to write to the Job we're currently executing and assign the newly created Result. We don't have to worry about writes happening at the same time since each worker will be running separate jobs, but we do need to worry about the order of reads vs writes. Since a client program could depend on the Result of the Job, and if there isn't a Result at the time of reading (but the Job is already finished) it could introduce a deadlock even if the library is completely fine. 
- Properties
	- Current Job reference (Job)
	- Process reference (Process)
	- Status (integer)
- Methods
	- execute(job) -> integer (pid)
	- getStatus() -> integer
	- getPID() -> integer
	- getJob() -> Job
	- stopProcess() -> void

**Job**

Job is an abstraction of the application that's needed to run. 
- Properties
	- Application to run (string)
	- Result (Result)
	- Status (integer)
- Methods
	- getCommand() -> string
	- getResult() -> Result
	- setResult() -> void (Will only set the result once)
	- getStatus() -> integer

**Job Manager**

The Job Manager will be responsible for creating the Worker needed to run each Job. It will also store the mapping for each of the PID -> Job, PID -> Worker associations needed for queries.
- Properties
	- PID/Job mapping to retrieve results (Map)
	- PID/Worker mapping to stop process (Map)
- Methods
	- addJob(string) -> integer (pid)
	- queryJob(integer) -> Job
	- stopJob(integer) -> void

## Tradeoffs
The servlet is also a basic implementation that will not be appropriate for a production system. A better solution for the future is to convert it into a Java Spring application.
