package com.peknight.docker.command.run

import cats.{Applicative, Show}
import com.peknight.codec.Codec
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.sum.StringType

sealed trait PortOption
object PortOption:
  case class Port(port: com.comcast.ip4s.Port) extends PortOption:
    override def toString: String = port.toString
  end Port
  // Inclusive
  case class Range(start: com.comcast.ip4s.Port, end: com.comcast.ip4s.Port) extends PortOption:
    override def toString: String = s"$start-$end"
  end Range

  def fromString(value: String): Option[PortOption] =
    if value.isBlank then None else
      val index = value.indexOf('-')
      if index < 0 then com.comcast.ip4s.Port.fromString(value).map(Port.apply)
      else if value.startsWith("-") || value.endsWith("-") then None
      else
        for
          start <- com.comcast.ip4s.Port.fromString(value.substring(0, index))
          end <- com.comcast.ip4s.Port.fromString(value.substring(index + 1))
        yield
          Range(start, end)

  def apply(port: com.comcast.ip4s.Port): Port = Port(port)

  extension (start: com.comcast.ip4s.Port)
    def to(end: com.comcast.ip4s.Port): Range = Range(start, end)
  end extension

  given stringCodecPortOption[F[_]: Applicative]: Codec[F, String, String, PortOption] =
    Codec.mapOption[F, String, String, PortOption](_.toString)(fromString)

  given codecPortOptionS[F[_]: Applicative, S: {StringType, Show}]: Codec[F, S, Cursor[S], PortOption] =
    Codec.codecS[F, S, PortOption]
end PortOption
