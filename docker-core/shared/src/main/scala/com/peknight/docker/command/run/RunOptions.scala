package com.peknight.docker.command.run

import cats.syntax.applicative.*
import cats.syntax.functor.*
import cats.syntax.traverse.*
import cats.{Monad, Show}
import com.comcast.ip4s.Hostname
import com.peknight.codec.config.given
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.error.DecodingFailure
import com.peknight.codec.ip4s.instances.host.given
import com.peknight.codec.sum.{ArrayType, NullType, ObjectType, StringType}
import com.peknight.codec.{Codec, Decoder, Encoder}
import com.peknight.docker.Identifier
import com.peknight.docker.Identifier.ContainerName
import com.peknight.docker.option.DockerOptions
import com.peknight.query.option.OptionConfig
import com.peknight.query.option.OptionKey.ShortOption
import com.peknight.query.parser.pairParser
import com.peknight.query.syntax.id.query.toOptions
import spire.math.Interval

case class RunOptions(
                       detach: Option[Boolean] = None,
                       env: Map[String, String] = Map.empty,
                       hostname: Option[Hostname] = None,
                       name: Option[ContainerName] = None,
                       restart: Option[RestartPolicy] = None,
                       volume: List[VolumeMount] = Nil
                     )
  extends DockerOptions:
  def options: List[String] =
    given OptionConfig = OptionConfig.transformObjectKey(flagKeys = List("detach")) {
      case "detach" => List(ShortOption('d', argLen = Interval.point(0)))
      case "env" => List(ShortOption('e'))
      case "hostname" => List(ShortOption('h'))
      case "volume" => List(ShortOption('v'))
    }
    this.toOptions
end RunOptions
object RunOptions:
  val default: RunOptions = RunOptions()

  given codecRunOptions[F[_]: Monad, S: {ObjectType, NullType, ArrayType, StringType, Show}]: Codec[F, S, Cursor[S], RunOptions] =
    given Codec[F, S, Cursor[S], Map[String, String]] = {
      Codec.instance[F, S, Cursor[S], Map[String, String]] { map =>
        ArrayType[S].to(map.map { case (k, v) => StringType[S].to(s"$k=$v") }.toVector).pure[F]
      } { t =>
        Decoder.decodeListA[F, S, String]
          .or(Decoder.decodeStringS[F, S].map[List[String]](List(_)))
          .decode(t)
          .map(_.flatMap(_.traverse(value => pairParser.parseAll(value).left.map(DecodingFailure.apply))
            .map(_.toMap)
          ))
      }
    }
    Codec.derived[F, S, RunOptions]
end RunOptions
