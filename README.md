# DAVE
## The Data Analytics Visualization Environment for xAPI and the Total Learning Architecture

**What is DAVE?**

The DAVE Framework will provide an open source means of creating domain-based xAPI learning data dashboards. It is extendable to new learning problems, instructional strategies, technological realities, and metrics objectives and will provide a framework for analysis and visualization which aligns with xAPI, xAPI Profiles, and the Total Learning Architecture (TLA).

**The Beta Framework Features**

* an analysis tool suite for the transform and visualization of xAPI Data
* a custom query language for the creation of transforms and aggregates of xAPI Data
* a visualization specification for the creation of custom visualization types
* workbooks for revisiting and reusing previous analyses
* definitions of primitive operations for xAPI transforms
* open source code — reusable by developers and learning engineers — that is modular and aligned to the capabilities of xAPI & xAPI Profiles and the flexible and extensible needs of the Total Learning Architecture.

Project DAVE is funded by the Advanced Distributed Learning Initiative at the U.S. Department of Defense.

**Primitives Resources**

Here is a quick link to the master doc for review of the new xAPI Primitives Specification: https://github.com/yetanalytics/dave/blob/master/docs/main.pdf

For more information on Primitives, see:
   - [Introduction to Operations, Primitives and Algorithms](https://github.com/yetanalytics/dave/blob/master/docs/algorithms/introduction.pdf)

**DAVE Query Language Resources**

See the [Query Readme](https://github.com/yetanalytics/dave/blob/master/QUERY.md)

**Vega Resources**

This version of the DAVE Framework uses Vega as its visualization specification. It will be crucial to have some familiarity to use DAVE's more interesting visualization features. Some tuorials can be found [here](https://vega.github.io/vega/tutorials/).

## For Developers: Running the Beta & Testing

### Interactive Workbooks

DAVE's interactive workbooks are written in [ClojureScript](https://clojurescript.org/)
which is compiled to Javascript to run in a browser. To get started, you'll need
the Java JDK version 1.8 or later, and a working installation of the [Clojure CLI](https://clojure.org/guides/getting_started).

To get an interactive development environment run:

    clj -A:fig:build

Or via the `Makefile`:

    make dev-repl

A browser window will open automatically to `http://localhost:9500`

To live-compile SASS files to CSS (do this in another shell):

    clojure -A:watch-sass

Or via the `Makefile`:

    make watch-sass

The root SASS file can be found at `/resources/dave/ui/sass/style.scss`

### Testing

The ClojureScript tests will run automatically while Figwheel is running, to view their output navigate to `http://localhost:9500/figwheel-extra-main/auto-testing`.

To run the ClojureScript tests outside of figwheel:

    clojure -Afig:test-cljs

Or via the `Makefile`:

    make test-cljs

To run Clojure tests on the JVM:

    clojure -Atest-clj

Or via the `Makefile`:

    make test-clj

All tests can be run via the `Makefile`:

    make ci

And an application package for GitHub Pages can be generated at `./gh_pages`:

    make gh_pages

And all project artifacts can be deleted:

    make clean

## License

Copyright © 2018-2020 Yet Analytics Inc

Distributed under the Apache 2.0 License. See the file LICENSE for more information.
