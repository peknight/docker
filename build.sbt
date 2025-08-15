ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.7.1"

ThisBuild / organization := "com.peknight"

ThisBuild / versionScheme := Some("early-semver")

ThisBuild / publishTo := {
  val nexus = "https://nexus.peknight.com/repository"
  if (isSnapshot.value)
    Some("snapshot" at s"$nexus/maven-snapshots/")
  else
    Some("releases" at s"$nexus/maven-releases/")
}

ThisBuild / credentials ++= Seq(
  Credentials(Path.userHome / ".sbt" / ".credentials")
)

ThisBuild / resolvers ++= Seq(
  "Pek Nexus" at "https://nexus.peknight.com/repository/maven-public/",
)

lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-unchecked",
    "-Xfatal-warnings",
    "-language:strictEquality",
    "-Xmax-inlines:64"
  ),
)

lazy val docker = (project in file("."))
  .aggregate(
    dockerCore.jvm,
    dockerCore.js,
    dockerClient.jvm,
    dockerClient.js,
  )
  .settings(commonSettings)
  .settings(
    name := "docker",
  )

lazy val dockerCore = (crossProject(JSPlatform, JVMPlatform) in file("docker-core"))
  .settings(commonSettings)
  .settings(
    name := "docker-core",
    libraryDependencies ++= Seq(
      "com.peknight" %%% "os-core" % pekOsVersion,
      "com.comcast" %%% "ip4s-core" % ip4sCoreVersion,
    ),
  )

lazy val dockerClient = (crossProject(JSPlatform, JVMPlatform) in file("docker-client"))
  .dependsOn(dockerCore)
  .settings(commonSettings)
  .settings(
    name := "docker-client",
    libraryDependencies ++= Seq(
      "com.peknight" %%% "os-fs2" % pekOsVersion,
      "com.peknight" %%% "cats-ext" % pekExtVersion,
      "com.peknight" %%% "logging-core" % pekLoggingVersion,
    ),
  )

val ip4sCoreVersion = "3.7.0"
val pekVersion = "0.1.0-SNAPSHOT"
val pekExtVersion = pekVersion
val pekLoggingVersion = pekVersion
val pekOsVersion = pekVersion
