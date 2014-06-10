#!/bin/sh
sbt "project service-net-tests" assembly >&2 &&
java -cp tests/target/scala-*/*.jar \
  mesosphere.servicenet.tests.TestDocGenerator "$@"
