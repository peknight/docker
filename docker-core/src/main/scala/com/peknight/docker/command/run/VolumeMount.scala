package com.peknight.docker.command.run

import cats.parse.Parser
import cats.syntax.either.*
import cats.syntax.functor.*
import cats.syntax.option.*
import cats.{Applicative, Id, Show}
import com.peknight.codec.Codec
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.error.DecodingFailure
import com.peknight.codec.fs2.io.instances.path.stringCodecPath
import com.peknight.codec.sum.StringType
import com.peknight.docker.Identifier.VolumeIdentifier
import fs2.io.file.Path

case class VolumeMount(hostPath: Path, containerPath: Path, permission: Option[Permission] = None)
object VolumeMount:
  def of(volume: VolumeIdentifier, containerPath: Path, permission: Option[Permission] = None): VolumeMount =
    VolumeMount(Path(volume.value), containerPath, permission)

  given stringCodecVolumeMount[F[_]: Applicative]: Codec[F, String, String, VolumeMount] =
    Codec.applicative[F, String, String, VolumeMount](volumeMount =>
      s"${volumeMount.hostPath}:${volumeMount.containerPath}${volumeMount.permission.map(p => s":$p").getOrElse("")}"
    ) { volumeMount =>
      val stringParser: Parser[String] = Parser.charsWhile(_ != ':')
      (((stringParser <* Parser.char(':')) ~ stringParser) ~ (Parser.char(':') *> stringParser.?).?)
        .parseAll(volumeMount)
        .left.map(DecodingFailure.apply)
        .flatMap { case ((hostPath, containerPath), permissionOption) =>
          for
            host <- stringCodecPath[Id].decode(hostPath)
            container <- stringCodecPath[Id].decode(containerPath)
            permission <- permissionOption.flatten.map(Permission.stringCodecPermission[Id].decode.map(_.map(_.some)))
              .getOrElse(none[Permission].asRight[DecodingFailure])
          yield
            VolumeMount(host, container, permission)
        }
    }
  given codecVolumeMount[F[_]: Applicative, S: {StringType, Show}]: Codec[F, S, Cursor[S], VolumeMount] =
    Codec.codecS[F, S, VolumeMount]
end VolumeMount
