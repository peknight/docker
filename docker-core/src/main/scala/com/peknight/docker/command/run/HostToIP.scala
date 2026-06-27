package com.peknight.docker.command.run

import cats.parse.Parser
import cats.{Applicative, Id, Show}
import com.comcast.ip4s.{Hostname, IpAddress}
import com.peknight.codec.Codec
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.error.DecodingFailure
import com.peknight.codec.ip4s.instances.host.{stringCodecHostname, stringCodecIpAddress}
import com.peknight.codec.sum.StringType

case class HostToIP(host: Hostname, ip: IpAddress)
object HostToIP:
  given stringCodecHostToIP[F[_]: Applicative]: Codec[F, String, String, HostToIP] =
    Codec.applicative[F, String, String, HostToIP](hostToIP =>
      s"${hostToIP.host.toString}:${hostToIP.ip.toUriString}"
    )(t =>
      ((Parser.charsWhile(_ != ':') <* Parser.char(':')) ~ Parser.anyChar.rep.string).parseAll(t)
        .left.map(DecodingFailure.apply)
        .flatMap { case (host, ip) =>
          for
            hostname <- stringCodecHostname[Id].decode(host)
            ipAddress <-  stringCodecIpAddress[Id].decode(ip)
          yield
            HostToIP(hostname, ipAddress)
        }
    )

  given codecHostToIp[F[_]: Applicative, S: {StringType, Show}]: Codec[F, S, Cursor[S], HostToIP] =
    Codec.codecS[F, S, HostToIP]

end HostToIP
