package com.peknight.docker.command.run

import cats.syntax.applicative.*
import cats.syntax.functor.*
import cats.syntax.traverse.*
import cats.{Id, Monad, Show}
import com.comcast.ip4s.{Hostname, IpAddress}
import com.peknight.codec.config.CodecConfig
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.error.DecodingFailure
import com.peknight.codec.ip4s.instances.host.given
import com.peknight.codec.sum.*
import com.peknight.codec.{Codec, Decoder, Encoder}
import com.peknight.commons.text.cases.KebabCase
import com.peknight.commons.text.syntax.cases.to
import com.peknight.docker.Identifier
import com.peknight.docker.Identifier.{ContainerName, NetworkIdentifier}
import com.peknight.docker.option.DockerOptions
import com.peknight.os.group.Group
import com.peknight.query.option.OptionConfig
import com.peknight.query.option.OptionKey.ShortOption
import com.peknight.query.parser.pairParser
import com.peknight.query.syntax.id.query.toOptions
import spire.math.Interval

case class RunOptions(
                       addHost: List[HostToIP] = List.empty,
                       detach: Option[Boolean] = None,
                       env: Map[String, String] = Map.empty,
                       groupAdd: List[Group] = List.empty,
                       hostname: Option[Hostname] = None,
                       ip: Option[IpAddress] = None,
                       name: Option[ContainerName] = None,
                       network: Option[NetworkIdentifier] = None,
                       publish: List[PortMapping] = Nil,
                       restart: Option[RestartPolicy] = None,
                       user: Option[UserGroup] = None,
                       volume: List[VolumeMount] = Nil
                     )
  extends DockerOptions:
  def options: List[String] =
    given OptionConfig = OptionConfig.transformObjectKey(flagKeys = List("detach")) {
      case "detach" => List(ShortOption('d', argLen = Interval.point(0)))
      case "env" => List(ShortOption('e'))
      case "hostname" => List(ShortOption('h'))
      case "publish" => List(ShortOption('p'))
      case "user" => List(ShortOption('u'))
      case "volume" => List(ShortOption('v'))
    }
    this.toOptions
end RunOptions
object RunOptions:
  val default: RunOptions = RunOptions()

  given codecRunOptions[F[_]: Monad, S: {ObjectType, NullType, ArrayType, BooleanType, StringType, Show}]
  : Codec[F, S, Cursor[S], RunOptions] =
    given CodecConfig = CodecConfig.default.withTransformMemberName(_.to(KebabCase))
    given Codec[F, S, Cursor[S], Boolean] = Codec.applicative[F, S, Cursor[S], Boolean](
      flag => if flag then BooleanType[S].to(true) else NullType[S].unit
    )(Decoder.decodeBooleanBS[Id, S].decode)
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
