import com.peknight.build.gav.*
import com.peknight.build.sbt.*

commonSettings

lazy val docker = (project in file("."))
  .settings(name := "docker")
  .aggregate(dockerCore.projectRefs *)
  .aggregate(dockerClient.projectRefs *)
  .aggregate(dockerService.projectRefs *)
  .aggregate(dockerCustom.projectRefs *)
  .aggregate(dockerBuild.projectRefs *)

lazy val dockerCore = (projectMatrix in file("docker-core"))
  .settings(name := "docker-core")
  .settings(libraryDependencies ++= dependencies(
    peknight.os,
    peknight.fs2.io,
    peknight.network,
    peknight.query,
    peknight.squants,
    peknight.catsParse,
    peknight.codec.ip4s,
    peknight.codec.fs2.io,
    peknight.codec.squants,
  ))
  .settings(libraryDependencies ++= testDependencies(scalaTest))
  .jvmPlatform(scalaVersions = Seq(scala.scala3.version))
  .jsPlatform(scalaVersions = Seq(scala.scala3.version))

lazy val dockerClient = (projectMatrix in file("docker-client"))
  .dependsOn(dockerCore)
  .settings(name := "docker-client")
  .settings(libraryDependencies ++= dependencies(http4s))
  .jvmPlatform(scalaVersions = Seq(scala.scala3.version))
  .jsPlatform(scalaVersions = Seq(scala.scala3.version))

lazy val dockerService = (projectMatrix in file("docker-service"))
  .dependsOn(dockerClient)
  .settings(name := "docker-service")
  .settings(libraryDependencies ++= dependencies(
    peknight.logging,
  ))
  .jvmPlatform(scalaVersions = Seq(scala.scala3.version))
  .jsPlatform(scalaVersions = Seq(scala.scala3.version))

lazy val dockerCustom = (projectMatrix in file("docker-custom"))
  .dependsOn(dockerService)
  .settings(name := "docker-custom")
  .settings(libraryDependencies ++= dependencies(
    peknight.build.gav,
    peknight.fs2.io,
    peknight.app,
  ))
  .jvmPlatform(scalaVersions = Seq(scala.scala3.version))
  .jsPlatform(scalaVersions = Seq(scala.scala3.version))

lazy val dockerBuild = (projectMatrix in file("docker-build"))
  .dependsOn(dockerCore)
  .settings(name := "docker-build")
  .settings(libraryDependencies ++= dependencies(
    peknight.app.build,
  ))
  .jvmPlatform(scalaVersions = Seq(scala.scala3.version))
  .jsPlatform(scalaVersions = Seq(scala.scala3.version))
