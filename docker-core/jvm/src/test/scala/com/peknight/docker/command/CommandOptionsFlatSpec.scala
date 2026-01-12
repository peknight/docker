package com.peknight.docker.command

import cats.syntax.option.*
import com.comcast.ip4s.*
import com.peknight.docker.Identifier.{ContainerName, NetworkName}
import com.peknight.docker.command.network.create.NetworkCreateOptions
import com.peknight.docker.command.run.*
import com.peknight.network.transport.TransportProtocol
import com.peknight.os.group.Group.{GroupId, GroupName}
import fs2.io.file.Path
import org.scalatest.flatspec.AnyFlatSpec

class CommandOptionsFlatSpec extends AnyFlatSpec:
  "Run Options" should "pass" in {
    println(RunOptions(
      addHost = List(HostToIP(host"www.peknight.com", ipv6"::1"), HostToIP(host"local.peknight.com", ipv4"127.0.0.1")),
      detach = true.some,
      env = Map("haha" -> "hehe", "rua" -> "asdf"),
      groupAdd = List(GroupId(999), GroupName("docker")),
      hostname = host"pek-hostname".some,
      ip = ipv4"172.18.0.2".some,
      name = ContainerName("pek-hostname").some,
      network = NetworkName("pek-network").some,
      publish = List(
        PortMapping(port"8080", port"80"),
        PortMapping(port"8088", port"88", protocol = TransportProtocol.UDP.some),
        PortMapping(port"8090", port"90", ipv4"192.168.0.1".some, TransportProtocol.TCP.some)
      ),
      restart = RestartPolicy.always.some,
      volume = List(VolumeMount(Path("/a/b") / Path("c"), Path("/d") / Path("e")))
    ).options)
  }

  "Network Create Options" should "pass" in {
    println(NetworkCreateOptions(
      subnet = Cidr[Ipv4Address](ipv4"172.18.0.0", 16).some,
      gateway = ipv4"172.18.0.1".some
    ).options)
  }
end CommandOptionsFlatSpec
