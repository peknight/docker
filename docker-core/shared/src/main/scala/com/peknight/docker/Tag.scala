package com.peknight.docker

import cats.{Applicative, Show}
import com.peknight.codec.Codec
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.sum.StringType

case class Tag(value: String):
  override def toString: String = value
end Tag
object Tag:
  given stringCodecTag[F[_]: Applicative]: Codec[F, String, String, Tag] =
    Codec.map[F, String, String, Tag](_.value)(Tag.apply)

  given codecTagS[F[_]: Applicative, S: {StringType, Show}]: Codec[F, S, Cursor[S], Tag] =
    Codec.codecS[F, S, Tag]
end Tag
