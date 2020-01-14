## Long-Running Operation With Polling
The <i>callback</i> is a well-known pattern for handling long-running operations in self-contained software systems. One component of the system issues a request to another. Some time later, on completion of the request, the component that received the request <i>calls back</i> the component that made the request to hand over the results. In the meantime the requester is free to carry on performing other tasks, without putting the entire system on hold while it awaits the results of the long-running operation.

This pattern can work well in distributed systems, too&mdash;say in a microservices environment where little or no distinction is made between components making requests (<i>clients</i>) and those receiving and fulfilling them (<i>servers</i>).

But what about the internet, where a web client (browser, e.g.) makes a request for a long-running operation to a server <i>n</i> hops away on the other side of the globe? The request part is easy; the callback not so much.

For this type of interaction we need a different approach; one that is implemented in this small framework, and to which I refer as the <i>order-for-pickup</i> pattern (whereas the traditional callback might be referred to as <i>order-for-delivery</i> pattern).

This framework implements a simplified version of the <i>Long Running Operation with Polling</i> pattern <a href=http://restalk-patterns.org/long-running-operation-polling.html target="_blank">described in this blog post</a>. Some known uses cases of this pattern are the <i><a href=https://docs.aws.amazon.com/amazonglacier/latest/dev/job-operations.html target="_blank">AWS Glacier REST API</a></i> and the <i><a href=https://docs.microsoft.com/en-us/azure/virtual-machines/linux/create-vm-rest-api target="_blank">Microsoft Azure REST API for Virtual Machines</a></i>.

For documentation on the specific implementation details of this framework, you can build and inspect the Javadocs for this project (see the section <i>Build the Project</i> in this README for more information).

### Project Requirements
* Maven 3+
* Java 1.8+

### Install the Project
Use either of the following methods to install the project source code, and build and runtime dependencies: 
* If git is installed, use <code>git clone https<nolink>://github.com/dchampion/http.git</code>. This will install the project in the <code>http</code> subdirectory of the file system directory from which you execute it. If you prefer a directory name other than <code>http</code>, simply type this name as a suffix to the <code>clone</code> command provided above.

* Or, using the link on this page, you can download and extract a zipped version of this project into a local file system directory on your computer.

### Build the Project
* Using a command (Windows) or bash (Linux or Mac) shell, navigate to the project root directory and type <code>mvn clean install</code>.

* (Optional) To generate project Javadocs, type <code>mvn javadoc:javadoc</code>. To browse the Javadocs, open <code>server/target/site/apidocs/index.html</code> (relative to the project root directory) in a web browser.

### Run the Project
* From the project root directory, type <code>java -jar server/target/http-server-1.0.0.jar</code> (Java 1.8+ must be in your search path for this command to work).

* Navigate to <code>http<nolink>://localhost:8080</code> in a web browser.

### Start Experimenting
Experiment with the capabilities of the long-call framework by manipulating the controls in its simple browser interface.

For finer-grained detail on the interations between the browser and the server, load the browser developer tools (provided with most web browsers) and navigate to the <code>Network</code> view to inspect client requests, server responses, status codes, header values and other useful information.