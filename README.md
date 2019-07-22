# sparttt.app

A running time trial app written in ClojureScript.

## Overview

The goal of this application is to be an improved rewrite of the first
iteration that was done in JavaScript. It became quite large and hard
to maintain in vanilla Javascript.

## Development

To get an interactive development environment run:

    lein fig:build

This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

	lein clean

To create a production build run:

	lein clean
	lein fig:min


## License

Copyright © 2019 Johan Mynhardt

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
