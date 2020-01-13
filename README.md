## Long-Running Operation With Polling
The <i>callback</i> is a well-known pattern for handling long-running operations in self-contained software systems. One component of the system issues a request to another. Some time later, on completion of the request, the component that received the request <i>calls back</i> the component that made the request to hand over the results. In the meantime the requester is free to carry on performing other tasks, without putting the entire system on hold while it awaits the results of the long-running operation.

This pattern can work well in distributed systems, too&mdash;say a microservces environment where little or no distinction is made between components making requests (<i>clients</i>) and those receiving and fulfilling them (<i>servers</i>).

But what about the internet, where a web client (browser) makes a request for a long-running operation to a  server on the other side of the globe? The request part is easy; the callback not so much.

For this type of interaction we need a different approach; one that is implemented in this small framework, and to which I refer as the <i>order-for-pickup</i> pattern (whereas the traditional callback might be referred to as <i>order-for-delivery</i> pattern).

This framework implements a simplified version of the <i><a href=http://restalk-patterns.org/long-running-operation-polling.html target="_blank">Long Running Operation with Polling</a></i> pattern. Known uses cases for this pattern are the <i><a href=https://docs.aws.amazon.com/amazonglacier/latest/dev/job-operations.html target="_blank">AWS Glacier REST API</a></i> and the <i><a href=https://docs.microsoft.com/en-us/azure/virtual-machines/linux/create-vm-rest-api target="_blank">Microsoft Azure REST API for Virtual Machines</a></i>.

For documentation on the specific implementation details of this framework, you can build and inspect the Javadocs for this project (see the section <i>Build the Project</i> below).

### Project Requirements
* Maven 3+
* Java 1.8+

### Install the Project
Use either of the following methods to install the project source code, and build and runtime dependencies: 
* If git is installed, use <i>git clone https://github.com/dchampion/http.git</i>. This will install the project in the <i>http</i> subdirectory of the file system directory from which you execute it. If you prefer a directory name other than <i>http</i>, simply type this name as a suffix to the <i>clone</i> command provided above.

* Or, using the link on this page, you can download and extract a zipped version of this project into a local file system directory on your computer.

### Build the Project
* Using a command (Windows) or bash (Linux) shell, navigate to the project root directory and type <i>mvn clean install</i>.

* (Optional) To generate project Javadocs, type <i>mvn javadoc:javadoc</i>. Then open <i>server/target/site/apidocs/index.html</i> (relative to the project root directory).

### Run the Project
* From the project root directory, type <i>java -jar server/target/http-server-1.0.0.jar</i>.

* Navigate to <i>http://localhost:8080</i> in a web browser.

### Start Experimenting
Experiment with the capabilities of the long-call framework by manipulating the controls in the simple browser interface.

For finer-grained detail on the interations between the browser and the server, load the browser developer tools (provided with most web browsers) and navigate to the <i>Network</i> view to inspect client requests, server responses, status codes, header values and other useful information.