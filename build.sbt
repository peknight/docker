import com.peknight.build.gav.*
import com.peknight.build.sbt.*

commonSettings

lazy val docker = (project in file("."))
  .settings(name := "docker")
  .aggregate(
    dockerCore.jvm,
    dockerCore.js,
    dockerClient.jvm,
    dockerClient.js,
    dockerService.jvm,
    dockerService.js,
    dockerCustom.jvm,
    dockerCustom.js,
    dockerBuild.jvm,
    dockerBuild.js,
  )

lazy val dockerCore = (crossProject(JVMPlatform, JSPlatform) in file("docker-core"))
  .settings(name := "docker-core")
  .settings(crossDependencies(
    peknight.os,
    peknight.network,
    peknight.query,
    peknight.codec.ip4s,
    peknight.codec.fs2.io,
  ))
  .settings(crossTestDependencies(scalaTest))

lazy val dockerClient = (crossProject(JVMPlatform, JSPlatform) in file("docker-client"))
  .dependsOn(dockerCore)
  .settings(name := "docker-client")
  .settings(crossDependencies(http4s))

lazy val dockerService = (crossProject(JVMPlatform, JSPlatform) in file("docker-service"))
  .dependsOn(dockerClient)
  .settings(name := "docker-service")
  .settings(crossDependencies(
    peknight.logging,
  ))

lazy val dockerCustom = (crossProject(JVMPlatform, JSPlatform) in file("docker-custom"))
  .dependsOn(dockerService)
  .settings(name := "docker-custom")
  .settings(crossDependencies(
    peknight.build.gav,
    peknight.fs2.io,
    peknight.app,
  ))

lazy val dockerBuild = (crossProject(JVMPlatform, JSPlatform) in file("docker-build"))
  .dependsOn(dockerCore)
  .settings(name := "docker-build")
