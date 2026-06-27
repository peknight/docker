package com.peknight.docker.command.run

import cats.{Applicative, Show}
import com.peknight.codec.Codec
import com.peknight.codec.config.given
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.derivation.EnumCodecDerivation
import com.peknight.codec.sum.StringType

enum Permission:
  case rw, ro
end Permission
object Permission:
  given stringCodecPermission[F[_]: Applicative]: Codec[F, String, String, Permission] =
    EnumCodecDerivation.unsafeDerivedStringCodecEnum[F, Permission]
    
  given codecPermission[F[_]: Applicative, S: {StringType, Show}]: Codec[F, S, Cursor[S], Permission] =
    Codec.codecS[F, S, Permission]
end Permission