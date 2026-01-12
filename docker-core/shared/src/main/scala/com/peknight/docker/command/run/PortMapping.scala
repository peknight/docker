package com.peknight.docker.command.run

import cats.syntax.eq.*
import cats.syntax.option.*
import cats.{Applicative, Show}
import com.comcast.ip4s.*
import com.peknight.codec.Codec
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.sum.StringType
import com.peknight.network.transport.TransportProtocol

case class PortMapping(hostPort: Port, containerPort: Port, hostIp: Option[IpAddress] = None, protocol: Option[TransportProtocol] = None):
  override def toString: String = s"${hostIp.map(ip => s"$ip:").getOrElse("")}$hostPort:$containerPort${protocol.map(protocol => s"/${protocol.toString.toLowerCase}").getOrElse("")}"
end PortMapping
object PortMapping extends App:
  def fromString(value: String): Option[PortMapping] =
    if value.isBlank || value.startsWith("/") || value.endsWith("/") then None else
      val protocolIndex = value.lastIndexOf("/")
      if protocolIndex > 0 then
        val protocolValue = value.substring(protocolIndex + 1)
        TransportProtocol.values.find(protocol => protocol.toString.toLowerCase === protocolValue)
          .flatMap(protocol => handleMapping(value.substring(0, protocolIndex), protocol.some))
      else handleMapping(value, none)

  private def handleMapping(value: String, protocol: Option[TransportProtocol]): Option[PortMapping] =
    if value.isBlank || value.endsWith(":") then None else
      val containerPortIndex = value.lastIndexOf(":")
      if containerPortIndex <= 0 then None else Port.fromString(value.substring(containerPortIndex + 1))
        .flatMap { containerPort =>
          val hostValue = value.substring(0, containerPortIndex)
          if hostValue.isBlank || hostValue.endsWith(":") then None else
            val hostPortIndex = hostValue.lastIndexOf(":")
            val (hostIpValue, hostPortValue) =
              if hostPortIndex > 0 then (hostValue.substring(0, hostPortIndex).some, hostValue.substring(hostPortIndex + 1))
              else if hostPortIndex == 0 then (none[String], hostValue.substring(1))
              else (none[String], hostValue)
            Port.fromString(hostPortValue)
              .flatMap(hostPort => hostIpValue
                .map(hostIp => IpAddress.fromString(hostIp).map(ip => PortMapping(hostPort, containerPort, ip.some, protocol)))
                .getOrElse(PortMapping(hostPort, containerPort, none, protocol).some))
        }

  given stringCodecPortMapping[F[_]: Applicative]: Codec[F, String, String, PortMapping] =
    Codec.mapOption[F, String, String, PortMapping](_.toString)(fromString)

  given codecPortMappingS[F[_]: Applicative, S: {StringType, Show}]: Codec[F, S, Cursor[S], PortMapping] =
    Codec.codecS[F, S, PortMapping]
end PortMapping
