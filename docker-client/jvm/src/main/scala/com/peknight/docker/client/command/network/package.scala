package com.peknight.docker.client.command

import cats.effect.Resource
import com.peknight.docker.Identifier.NetworkIdentifier
import com.peknight.docker.command.network.create.NetworkCreateOptions
import com.peknight.docker.docker
import fs2.io.process.{Process, ProcessBuilder, Processes}

package object network:
  def create[F[_]: Processes](network: NetworkIdentifier)(options: NetworkCreateOptions = NetworkCreateOptions.default)
  : Resource[F, Process[F]] =
    ProcessBuilder(docker, com.peknight.docker.command.network.command :: com.peknight.docker.command.network.create.command :: options.options ::: network.value :: Nil).spawn[F]
end network
