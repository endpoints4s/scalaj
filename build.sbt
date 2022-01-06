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
      )
    )

val documentation =
  project.in(file("documentation"))
    .enablePlugins(ParadoxMaterialThemePlugin, ParadoxPlugin, ParadoxSitePlugin, ScalaUnidocPlugin)
    .settings(
      skip / publish := true,
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
      skip / publish := true
    )

Global / onChangedBuildSource := ReloadOnSourceChanges
