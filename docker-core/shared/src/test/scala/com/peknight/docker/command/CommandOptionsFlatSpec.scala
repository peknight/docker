package com.peknight.docker.command

import cats.syntax.option.*
import com.comcast.ip4s.{host, ipv4, ipv6}
import com.peknight.docker.Identifier.ContainerName
import com.peknight.docker.command.run.{HostToIP, RestartPolicy, RunOptions, VolumeMount}
import com.peknight.os.group.Group.{GroupId, GroupName}
import fs2.io.file.Path
import org.scalatest.flatspec.AnyFlatSpec

class CommandOptionsFlatSpec extends AnyFlatSpec:
  "Run Options" should "succeed" in {
    println(RunOptions(
      addHost = List(HostToIP(host"www.peknight.com", ipv6"::1"), HostToIP(host"local.peknight.com", ipv4"127.0.0.1")),
      detach = true.some,
      env = Map("haha" -> "hehe", "rua" -> "asdf"),
      groupAdd = List(GroupId(999), GroupName("docker")),
      hostname = host"pek-hostname".some,
      name = ContainerName("pek-hostname").some,
      restart = RestartPolicy.always.some,
      volume = List(VolumeMount(Path("/a/b") / Path("c"), Path("/d") / Path("e")))
    ).options)
  }
end CommandOptionsFlatSpec
