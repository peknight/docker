package com.peknight.docker.command.volume.create

import cats.{Monad, Show}
import com.peknight.codec.Codec
import com.peknight.codec.config.given
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.sum.{NullType, ObjectType, StringType}
import com.peknight.docker.option.DockerOptions
import com.peknight.query.option.given
import com.peknight.query.syntax.id.query.toOptions


case class VolumeCreateOptions() extends DockerOptions:
  def options: List[String] = this.toOptions
end VolumeCreateOptions
object VolumeCreateOptions:
  val default: VolumeCreateOptions = VolumeCreateOptions()

  given codecVolumeCreateOptions[F[_]: Monad, S: {ObjectType, NullType, StringType, Show}]
  : Codec[F, S, Cursor[S], VolumeCreateOptions] =
    Codec.derived[F, S, VolumeCreateOptions]
end VolumeCreateOptions

