# DAVE
## The Data Analytics Visualization Efficiency Framework for xAPI and the Total Learning Architecture

*How to contribute to Project DAVE: https://github.com/yetanalytics/dave/wiki/Contributing (includes workflow requirements).*

*Quickstart: Click on "Projects" above to see the current status of issues. Feel free to start working on the issues of your choice.*

**What is DAVE?**

The DAVE Framework will provide an open source means of creating domain-based xAPI learning data dashboards. It is extendable to new learning problems, instructional strategies, technological realities, and metrics objectives and will provide a framework for analysis and visualization which aligns with xAPI, xAPI Profiles, and the Total Learning Architecture (TLA).

**The Framework Will Feature**

* a suite of prototype analytics algorithms and data visualization templates
* open source dashboard prototypes for TLA data analytics and visualization
* open source code — reusable by developers and learning engineers — that is modular and aligned to the capabilities of xAPI & xAPI Profiles and the flexible and extensible needs of the Total Learning Architecture.

Project DAVE is funded by the Advanced Distributed Learning Initiative at the U.S. Department of Defense.

**Take Part**

Here is a quick link to the master doc for review of the template: https://github.com/yetanalytics/dave/blob/master/docs/algorithms/master.pdf

Please join the Google Group to keep up with DAVE-related conversation and notifications: https://groups.google.com/forum/#!forum/project-dave

## For Developers: Running the Interactive Workbooks

DAVE's interactive workbooks are written in [ClojureScript](https://clojurescript.org/)
which is compiled to Javascript to run in a browser. To get started, you'll need
the Java JDK version 1.8 or later, and a working installation of the [Clojure CLI](https://clojure.org/guides/getting_started).

To get an interactive development environment run:

    clj -A:fig:build

A browser window will open automatically.

To live-compile SASS files to CSS (do this in another shell):

    clojure -A:watch-sass

The root SASS file can be found at `/resources/sass/style.scss`

This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    rm -rf target/public

To create a production build run:

	rm -rf target/public
	clojure -A:fig:min

## License
Copyright © 2018 Yet Analytics Inc

Distributed under the Apache 2.0 License. See the file LICENSE for more information.
