package com.peknight.docker.option

import cats.{Monad, Show}
import com.peknight.codec.Codec
import com.peknight.codec.config.given
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.path.{PathElem, PathToRoot}
import com.peknight.codec.sum.{NullType, ObjectType, StringType}
import com.peknight.query.option.OptionKey.ShortOption
import com.peknight.query.option.{OptionConfig, OptionKey}
import com.peknight.query.syntax.id.query.toOptions
import spire.math.Interval

case class RemoveOptions(force: Option[Boolean] = None) extends DockerOptions:
  def options: List[String] =
    given OptionConfig = OptionConfig(RemoveOptions.transformKey, flagKeys = RemoveOptions.flagKeys)
    this.toOptions
end RemoveOptions
object RemoveOptions:
  val default: RemoveOptions = RemoveOptions()
  given codecRemoveOptions[F[_]: Monad, S: {ObjectType, NullType, StringType, Show}]: Codec[F, S, Cursor[S], RemoveOptions] =
    Codec.derived[F, S, RemoveOptions]

  private def transformKey(pathToRoot: PathToRoot): List[OptionKey] =
    pathToRoot.value.lastOption match
      case Some(PathElem.ObjectKey("force")) => List(ShortOption('f', argLen = Interval.point(0)))
      case _ => Nil

  private val flagKeys: List[String] = List("force")
end RemoveOptions

