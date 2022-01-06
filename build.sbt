import xerial.sbt.Sonatype.GitHubHosting
import com.lightbend.paradox.markdown.Writer

inThisBuild(List(
  versionPolicyIntention := Compatibility.BinaryAndSourceCompatible,
  organization := "org.endpoints4s",
  sonatypeProjectHosting := Some(
    GitHubHosting("endpoints4s", "scalaj", "julien@richard-foy.fr")
  ),
  homepage := Some(sonatypeProjectHosting.value.get.scmInfo.browseUrl),
  licenses := Seq(
    "MIT License" -> url("http://opensource.org/licenses/mit-license.php")
  ),
  developers := List(
    Developer("julienrf", "Julien Richard-Foy", "julien@richard-foy.fr", url("http://julien.richard-foy.fr"))
  ),
  scalaVersion := "2.13.7",
  crossScalaVersions := Seq("2.13.7", "3.0.2", "2.12.13"),
  versionPolicyIgnoredInternalDependencyVersions := Some("^\\d+\\.\\d+\\.\\d+\\+\\d+".r)
))

val `scalaj-client` =
  project
    .in(file("client"))
    .settings(
      name := "scalaj-client",
      libraryDependencies ++= Seq(
        ("org.endpoints4s" %% "openapi" % "4.0.0").cross(CrossVersion.for3Use2_13),
        ("org.scalaj" %% "scalaj-http" % "2.4.2").cross(CrossVersion.for3Use2_13),
        ("org.endpoints4s" %% "algebra-testkit" % "1.0.0" % Test).cross(CrossVersion.for3Use2_13),
        ("org.endpoints4s" %% "algebra-circe-testkit" % "1.0.0" % Test).cross(CrossVersion.for3Use2_13)
      ),
      publish / skip := scalaVersion.value.startsWith("3"), // Don’t publish Scala 3 artifacts for now because the algebra is not published for Scala 3
      // Scala 2.x vs 3.x
      scalacOptions ++= {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, _)) =>
            Seq(
              "-feature",
              "-deprecation",
              "-encoding",
              "UTF-8",
              "-unchecked",
              "-language:implicitConversions",
              "-Ywarn-dead-code",
              "-Ywarn-numeric-widen",
              "-Ywarn-value-discard"
            )
          case _ =>
            Seq(
              "-feature",
              "-deprecation",
              "-encoding",
              "UTF-8",
              "-unchecked",
              "-language:implicitConversions,Scala2Compat"
            )
        }
      },
      // Scala 2.12 vs 2.13
      scalacOptions ++= {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, n)) if n >= 13 =>
            Seq(
              "-Xlint:adapted-args,nullary-unit,inaccessible,infer-any,missing-interpolator,doc-detached,private-shadow,type-parameter-shadow,poly-implicit-overload,option-implicit,delayedinit-select,package-object-classes,stars-align,constant,unused,nonlocal-return,implicit-not-found,serial,valpattern,eta-zero,eta-sam,deprecation"
            ) ++ (if (insideCI.value) Seq("-Xfatal-warnings") else Nil)
          case Some((2, _)) =>
            Seq(
              "-Xlint",
              "-Yno-adapted-args",
              "-Ywarn-unused-import",
              "-Xexperimental",
              "-Xfuture",
              "-language:higherKinds"
            )
          case _ =>
            Seq()
        }
      }
    )

val documentation =
  project.in(file("documentation"))
    .enablePlugins(ParadoxMaterialThemePlugin, ParadoxPlugin, ParadoxSitePlugin, ScalaUnidocPlugin)
    .settings(
      publish / skip := true,
      coverageEnabled := false,
      autoAPIMappings := true,
      Compile / paradoxMaterialTheme := {
        val theme = (Compile / paradoxMaterialTheme).value
        val repository =
          (ThisBuild / sonatypeProjectHosting).value.get.scmInfo.browseUrl.toURI
        theme
          .withRepository(repository)
          .withSocial(repository)
          .withCustomStylesheet("snippets.css")
      },
      paradoxProperties ++= Map(
        "version" -> version.value,
        "scaladoc.base_url" -> s".../${(packageDoc / siteSubdirName).value}",
        "github.base_url" -> s"${homepage.value.get}/blob/v${version.value}"
      ),
      paradoxDirectives += ((_: Writer.Context) =>
        org.endpoints4s.paradox.coordinates.CoordinatesDirective
      ),
      ScalaUnidoc / unidoc / scalacOptions ++= Seq(
        "-implicits",
        "-diagrams",
        "-groups",
        "-doc-source-url",
        s"${homepage.value.get}/blob/v${version.value}€{FILE_PATH}.scala",
        "-sourcepath",
        (ThisBuild / baseDirectory).value.absolutePath
      ),
      ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(
        `scalaj-client`
      ),
      packageDoc / siteSubdirName := "api",
      addMappingsToSiteDir(
        ScalaUnidoc / packageDoc / mappings,
        packageDoc / siteSubdirName
      )
    )

val scalaj =
  project.in(file("."))
    .aggregate(`scalaj-client`, documentation)
    .settings(
      publish / skip := true
    )

Global / onChangedBuildSource := ReloadOnSourceChanges
