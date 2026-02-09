package com.peknight.docker.command.run

import cats.syntax.applicative.*
import cats.syntax.apply.*
import cats.syntax.either.*
import cats.syntax.functor.*
import cats.syntax.option.*
import cats.{Applicative, Show}
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.error.MissingField
import com.peknight.codec.sum.StringType
import com.peknight.codec.{Codec, Decoder}
import com.peknight.os.group.Group
import com.peknight.os.user.User

case class UserGroup(user: User, group: Option[Group] = None):
  override def toString: String = s"$user${group.map(g => s":$g").getOrElse("")}"
end UserGroup
object UserGroup:
  given stringCodecUserGroup[F[_]: Applicative]: Codec[F, String, String, UserGroup] =
    Codec.instance[F, String, String, UserGroup](_.toString.pure[F]) { t =>
      val index = t.indexOf(":")
      if index < 0 then Decoder[F, String, User].decode(t).map(_.map(UserGroup(_)))
      else if index == 0 then MissingField.label("user").asLeft[UserGroup].pure[F]
      else if index == t.length - 1 then Decoder[F, String, User].decode(t.substring(0, index)).map(_.map(UserGroup(_)))
      else
        (Decoder[F, String, User].decode(t.substring(0, index)), Decoder[F, String, Group].decode(t.substring(index + 1)))
          .mapN((userEither, groupEither) => userEither.flatMap(user => groupEither.map(group => UserGroup(user, group.some))))
    }

  given codecUserGroupS[F[_]: Applicative, S: {StringType, Show}]: Codec[F, S, Cursor[S], UserGroup] =
    Codec.codecS[F, S, UserGroup]
end UserGroup
