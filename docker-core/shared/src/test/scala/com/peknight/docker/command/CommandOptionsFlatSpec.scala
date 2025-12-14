package com.peknight.docker.command

import cats.syntax.option.*
import com.comcast.ip4s.host
import com.peknight.docker.Identifier.ContainerName
import com.peknight.docker.command.run.{RestartPolicy, RunOptions, VolumeMount}
import fs2.io.file.Path
import org.scalatest.flatspec.AnyFlatSpec

class CommandOptionsFlatSpec extends AnyFlatSpec:
  "Run Options" should "succeed" in {
    println(RunOptions(
      detach = true.some,
      env = Map("haha" -> "hehe", "rua" -> "asdf"),
      hostname = host"pek-hostname".some,
      name = ContainerName("pek-hostname").some,
      restart = RestartPolicy.always.some,
      volume = List(VolumeMount(Path("/a/b") / Path("c"), Path("/d") / Path("e")))
    ).options)
  }
end CommandOptionsFlatSpec
