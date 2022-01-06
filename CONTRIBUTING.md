Contributing
============

## Workflow

Clone the project and create a branch off the `main` branch. Commit your changes,
push them to a fork of this repository, and [create a pull request](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request).

## Build the Project

Use sbt to build the project:

~~~ sh
sbt
~~~

And then:

~~~
compile
~~~

And, to run all the tests, on all the supported Scala versions:

~~~
+test
~~~

To check that you didn’t break the intended binary or source compatibility:

~~~
versionPolicyCheck
~~~

To build and preview the documentation:

~~~
previewSite
~~~

To format the source code:

~~~
scalafmt
~~~

## Publish a Release

We use sbt-ci-release. To publish a release, push a tag (e.g. `v.2.3.4`) on the `main` branch
and let the CI publish the release. The CI job checks that the version number in the tag is
consistent with the declared compatibility intention, publishes the artifacts to Maven Central,
and publishes the website to https://endpoints4s.github.io/scalaj.

Then, reset the compatibility intention to `BinaryAndSourceCompatible` (see below).

Write the release notes on the [releases page](https://github.com/endpoints4s/scalaj/releases).
The release notes must document all the deprecations and breaking changes, all the bug fixes,
and all the new features.

## Compatibility Policy

This project follows [Semantic Versioning](https://docs.scala-lang.org/overviews/core/binary-compatibility-for-library-authors.html#recommended-versioning-scheme).

It uses sbt-version-policy to manage the compatibility checks and version checks.

After a release, reset `versionPolicyIntention` to `BinaryAndSourceCompatible`:

~~~ scala
// in build.sbt
versionPolicyIntention := Compatibility.BinaryAndSourceCompatible
~~~

If you submitted a PR and the CI tells you that you have to relax the compatibility intention,
update `versionPolicyIntention` accordingly:

~~~ scala
// in build.sbt
versionPolicyIntention := Compatibility.BinaryCompatible
// or
versionPolicyIntention := Compatibility.None
~~~
