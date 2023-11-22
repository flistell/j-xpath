#!/usr/bin/env bash

JXPATH_HOME="${JXPATH_HOME:-$(dirname $0)/target}"

$JAVA_HOME/bin/java $JAVA_OPTIONS -jar $JXPATH_HOME/j-xpath-1.0-SNAPSHOT.jar $@
