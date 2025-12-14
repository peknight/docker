package com.peknight.docker.command.inspect

import cats.{Monad, Show}
import com.peknight.codec.config.given
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.sum.{NullType, ObjectType, StringType}
import com.peknight.codec.{Codec, Encoder}
import com.peknight.docker.option.DockerOptions
import com.peknight.query.Query
import com.peknight.query.Query.given
import com.peknight.query.option.given
import com.peknight.query.syntax.id.query.toOptions

case class InspectOptions() extends DockerOptions:
  def options: List[String] = this.toOptions
end InspectOptions
object InspectOptions:
  def default: InspectOptions = InspectOptions()
  given codecInspectOptions[F[_]: Monad, S: {ObjectType, NullType, StringType, Show}]: Codec[F, S, Cursor[S], InspectOptions] =
    Codec.derived[F, S, InspectOptions]
end InspectOptions
