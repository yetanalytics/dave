# DAVE
## The Data Analytics Visualization Environment for xAPI and the Total Learning Architecture

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

The structure, contents and format of the above document are currently undergoing a revision. For more information, see:
- [Primitives Documentation Branch](../../tree/primitives-document)
   - [Introduction to Operations, Primitives and Algorithms](../../tree/primitives-document/docs/algorithms/introduction.pdf)
      - [Example Operation - Associate](../../tree/primitives-document/docs/operations/associate.pdf)
      - [Example Primitive - Accumulate](../../tree/primitives-document/docs/primitives/accumulate.pdf)
      - [Example of Algorithm - Rate of Completions](../../tree/primitives-document/docs/algorithm_definitions/rateOfCompletions.pdf)

The [Primitives Documentation Branch](../../tree/primitives-document) is Active and thus subject to change. Please avoid committing directly to this branch. Instead, either open an [Issue](../../issues) or [branch](https://help.github.com/en/articles/about-branches) off of the [Primitives Documentation Branch](../../tree/primitives-document). For more information, see the [Contributing Wiki](../../wiki/Contributing)

## For Developers: Running the Interactive Workbooks & Testing

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

Copyright © 2018-2019 Yet Analytics Inc

Distributed under the Apache 2.0 License. See the file LICENSE for more information.
