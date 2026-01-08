package com.peknight.docker

import cats.{Applicative, Show}
import com.peknight.codec.Codec
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.sum.StringType

case class Namespace(value: String):
  override def toString: String = value
end Namespace
object Namespace:
  given stringCodecNamespace[F[_]: Applicative]: Codec[F, String, String, Namespace] =
    Codec.map[F, String, String, Namespace](_.value)(Namespace.apply)

  given codecNamespaceS[F[_]: Applicative, S: {StringType, Show}]: Codec[F, S, Cursor[S], Namespace] =
    Codec.codecS[F, S, Namespace]
end Namespace
