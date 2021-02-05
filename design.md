# Worker Library Design Doc

This document will describe a high level overview of a Java worker library with methods to start/stop/query status and get an output of a running job through a REST API.

## API

To keep things simple, the REST API will have no authentication. Endpoints return JSON data in the response.

    POST /start
**Parameters**
| Name | Type  | In | Description |
|:--|:--|:--|:--|
| file_path | string | body | File path of the Linux application to run |

Endpoint to start a worker to run a Linux application with the specified file path.

    GET/query/<pid>
Endpoint for retrieving information on the status of a running process/worker with a specified process id integer.
  
    GET/stop/<pid>
Endpoint for stopping a running process/worker with a specified process id integer.

## Design

Fundamentally, we’ll have a concept of a job, process/worker pool, job queue, job manager and a result. Similar to how many other workers libraries function, there will be a job manager process that will determine which worker/process runs a job at a given time. Other libraries might have concepts of a message queue or something similar. In our case, we’ll instead have a job queue that can be added to the job manager. Jobs that can be processed immediately don’t have to enter the queue, and the manager will automatically choose a worker to run it in a round robin fashion. These workers are created on launch and how many being launched will be determined by a pool size variable. Workers function by launching a new process of the Linux application and will use a secondary thread to read back the output to store the result back into the Job object. The Java servlet responsible for handling API requests can then ask for jobs from the manager and receive information on whether the job is finished, running, or not queued yet. If the job is finished, there will be a corresponding Result object that can be accessed for output and worker ID information. This information is then returned through the REST API as JSON. 

![](https://i.imgur.com/LfoYLAw.jpg)

## Tradeoffs
Some tradeoffs are that the pool won't be automatically expanded, so on high loads the tasks could take much longer. We don't consume as much resources this way, but performance wise it could suffer. One way to improve on this in the future is to auto scale the workers as needed based on load. Another thing is that the job queue won't be the most efficient implementation, as it will just be a naïve ConcurrentLinkedQueue that will be holding job objects. Other implementations like ZeroMQ will have better options for configuration and will be better optimized. Finally, the servlet is also a basic implementation that will not be appropriate for a production system. A better solution for the future is to convert it into a Java Spring application.
