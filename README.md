<!--- http://dillinger.io/ --->
# sqrl-server-demo


This JEE app is the demo for the [sqrl-server-base](https://github.com/sqrlserverjava/sqrl-server-base)  and [sqrl-server-atmosphere](https://github.com/sqrlserverjava/sqrl-server-atmosphere) libraries.  This demo app is running at https://sqrljava.tech:20000/sqrlexample/login

You can run the demo app locally by:
1. Checkout the sqrlexample project:  `git clone https://github.com/sqrlserverjava/sqrl-server-example.git`
1. Move into the directory: `cd sqrl-server-example`
1. Start the server with jetty: `mvn jetty:run`
1. Install a native SQRL client such as sqrl.exe from [grc.com](https://www.grc.com/dev/sqrl.exe) - mobile clients will not work when running on localhost
1. Open https://127.0.0.1/sqrlexample/ in Chrome, IE, or Edge (firefox does not work on localhost for some reason)
1. Bypass the certificate warning (unavoidable on localhost)

You can also run the maven jetty command in debug mode in eclipse (or your favorite IDE) to step through the code, etc

#### Reporting Issues
See [CONTRIBUTING.md](https://github.com/sqrlserverjava/sqrl-server-example/blob/master/CONTRIBUTING.md)