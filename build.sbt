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
  )

lazy val dockerCore = (crossProject(JVMPlatform, JSPlatform) in file("docker-core"))
  .settings(name := "docker-core")
  .settings(crossDependencies(
    peknight.os,
    peknight.query,
    peknight.codec.ip4s,
  ))
  .settings(crossTestDependencies(scalaTest))

lazy val dockerClient = (crossProject(JVMPlatform, JSPlatform) in file("docker-client"))
  .dependsOn(dockerCore)
  .settings(name := "docker-client")
  .settings(crossDependencies(
    peknight.os.fs2,
    peknight.ext.cats,
    peknight.logging,
  ))
