package com.peknight.docker.command

import cats.syntax.option.*
import com.comcast.ip4s.host
import com.peknight.docker.Identifier.ContainerName
import com.peknight.docker.command.run.{RestartPolicy, RunOptions}
import org.scalatest.flatspec.AnyFlatSpec

class CommandOptionsFlatSpec extends AnyFlatSpec:
  "Run Options" should "succeed" in {
    println(RunOptions(detach = true.some, hostname = host"pek-hostname".some, name = ContainerName("pek-hostname").some,
      restart = RestartPolicy.always.some).options)
  }
end CommandOptionsFlatSpec
