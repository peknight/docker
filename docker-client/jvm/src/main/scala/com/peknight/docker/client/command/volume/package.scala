package com.peknight.docker.client.command

import cats.effect.Resource
import com.peknight.docker.Identifier.VolumeIdentifier
import com.peknight.docker.command.volume.create.VolumeCreateOptions
import com.peknight.docker.docker
import fs2.io.process.{Process, ProcessBuilder, Processes}

package object volume:
  def create[F[_]: Processes](volume: VolumeIdentifier)(options: VolumeCreateOptions = VolumeCreateOptions.default)
  : Resource[F, Process[F]] =
    ProcessBuilder(docker, com.peknight.docker.command.volume.command ::
      com.peknight.docker.command.volume.create.command :: options.options ::: volume.value :: Nil).spawn[F]
end volume
