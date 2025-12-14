package com.peknight.docker.command.run

import cats.{Monad, Show}
import com.comcast.ip4s.Hostname
import com.peknight.codec.config.given
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.ip4s.instances.host.given
import com.peknight.codec.path.{PathElem, PathToRoot}
import com.peknight.codec.sum.{NullType, ObjectType, StringType}
import com.peknight.codec.{Codec, Encoder}
import com.peknight.docker.Identifier
import com.peknight.docker.Identifier.ContainerName
import com.peknight.docker.option.DockerOptions
import com.peknight.query.option.OptionKey.ShortOption
import com.peknight.query.option.{OptionConfig, OptionKey}
import com.peknight.query.syntax.id.query.toOptions
import spire.math.Interval

case class RunOptions(detach: Option[Boolean] = None, hostname: Option[Hostname] = None, name: Option[ContainerName] = None)
  extends DockerOptions:
  def options: List[String] =
    given OptionConfig = OptionConfig(RunOptions.transformKey, flagKeys = RunOptions.flagKeys)
    this.toOptions
end RunOptions
object RunOptions:
  val default: RunOptions = RunOptions()

  given codecRunOptions[F[_]: Monad, S: {ObjectType, NullType, StringType, Show}]: Codec[F, S, Cursor[S], RunOptions] =
    Codec.derived[F, S, RunOptions]

  private def transformKey(pathToRoot: PathToRoot): List[OptionKey] =
    pathToRoot.value.lastOption match
      case Some(PathElem.ObjectKey("detach")) => List(ShortOption('d', argLen = Interval.point(0)))
      case Some(PathElem.ObjectKey("hostname")) => List(ShortOption('h'))
      case _ => Nil

  private val flagKeys: List[String] = List("detach")
end RunOptions
