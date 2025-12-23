package com.peknight.docker.command.remove

import cats.{Monad, Show}
import com.peknight.codec.Codec
import com.peknight.codec.config.given
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.sum.{NullType, ObjectType, StringType}
import com.peknight.docker.option.DockerOptions
import com.peknight.query.option.OptionConfig
import com.peknight.query.option.OptionKey.ShortOption
import com.peknight.query.syntax.id.query.toOptions
import spire.math.Interval

case class RemoveImageOptions(force: Option[Boolean] = None) extends DockerOptions:
  def options: List[String] =
    given OptionConfig = OptionConfig.transformObjectKey(flagKeys = List("force")) {
      case "force" => List(ShortOption('f', argLen = Interval.point(0)))
    }
    this.toOptions
end RemoveImageOptions
object RemoveImageOptions:
  val default: RemoveImageOptions = RemoveImageOptions()
  given codecRemoveImageOptions[F[_]: Monad, S: {ObjectType, NullType, StringType, Show}]
  : Codec[F, S, Cursor[S], RemoveImageOptions] =
    Codec.derived[F, S, RemoveImageOptions]
end RemoveImageOptions

