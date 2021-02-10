# Workers

Worker library with HTTP API endpoints to start/stop/query status and get an output
of a running job. Created for Teleport technical challenge Summer 2021.

## Getting Started
Before building the project, we need to install Maven and Tomcat. This will be dependent on your Linux distribution, but I've listed the basic instructions for Ubuntu.

**Maven**
```bash
sudo apt update
sudo apt install maven
```
Verify installation with
```bash
mvn -version
```
**Apache Tomcat**

Tomcat version >= 8

```bash
sudo apt update
sudo apt install tomcat9
```
Tomcat automatically starts up. We can stop it with
```bash
sudo service tomcat9 stop
```
This will be useful if we want to deploy to embedded Tomcat.

**NOTE:** Installing from binary is bit more involved and may require more troubleshooting and configuration as I haven't tried this route. If there's any questions, feel free to ping me on Slack.

## Build

Make sure to run these commands in the project root.

Run tests
```bash
mvn test
```
Build jar
```bash
mvn package
```
Deploy to embedded Tomcat to use API

**NOTE:** Make sure Tomcat service is not already running otherwise this will fail!

```bash
mvn package tomcat:run-war
# OR
mvn package tomcat:run
```
### Advanced (TODO)
**NOTE:** This works on Mac OS, but for some reason isn't working on Ubuntu. These steps are not necessary to run the app. Starting an embedded Tomcat instance is good enough.

If we're running Tomcat as a service, we'll need to configure some users. To do this, navigate to wherever `tomcat-users.xml` is located (For Ubuntu, should be at `/etc/tomcat9/tomcat-users.xml` if installed as listed above). Open the file and make sure it contains:
```xml
<role rolename="manager-script"/>
<user username="workers" password="teleport" roles="manager-script"/>
```
This will configuration defines a role named `manager-script` and a user called `workers` with password `teleport`. Modify this as needed, and it's alright to have other users/roles configured already.

Next, navigate to `~/.m2` and create/open file called `settings.xml` and make sure it contains:
```xml
<settings>
  <servers>
    <server>
      <id>workers</id>
      <username>workers</username>
      <password>teleport</password>
    </server>
  </servers>
</settings>
```
Make sure the username and password match the ones defined in `tomcat-users.xml`.

Next, start up Tomcat and run
```bash
mvn package tomcat:redeploy
```
Which should deploy the application.
