package com.peknight.docker.option

import cats.{Monad, Show}
import com.peknight.codec.Codec
import com.peknight.codec.config.given
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.path.{PathElem, PathToRoot}
import com.peknight.codec.sum.{NullType, NumberType, ObjectType, StringType}
import com.peknight.os.signal.Signal
import com.peknight.query.option.OptionKey
import com.peknight.query.option.OptionKey.ShortOption

import scala.concurrent.duration.*

case class StopOptions(signal: Option[Signal] = None, timeout: Option[Duration] = None) extends DockerOptions:
  def options: List[String] =
    signal.toList.flatMap(s => List("-s", s"$s")) ::: timeout.toList.map {
      case t if t.isFinite => s"${t.toSeconds}"
      case _ => "-1"
    }.flatMap(t => List("-t", t))
end StopOptions
object StopOptions:
  val default: StopOptions = StopOptions()

  given codecStopOptions[F[_]: Monad, S: {ObjectType, NullType, NumberType, StringType, Show}]
  : Codec[F, S, Cursor[S], StopOptions] =
    given Codec[F, S, Cursor[S], Duration] = Codec[F, S, Cursor[S], Long]
      .imap(sec => sec.seconds)(duration => if duration.isFinite then -1 else duration.toSeconds)
    Codec.derived[F, S, StopOptions]

  private def transformKey(pathToRoot: PathToRoot): List[OptionKey] =
    pathToRoot.value.lastOption match
      case Some(PathElem.ObjectKey("signal")) => List(ShortOption('s'))
      case Some(PathElem.ObjectKey("timeout")) => List(ShortOption('t'))
      case _ => Nil
end StopOptions
