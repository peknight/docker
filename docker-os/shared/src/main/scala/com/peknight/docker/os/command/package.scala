package com.peknight.docker.os

import cats.Monad
import cats.data.IorT
import cats.effect.{MonadCancel, Resource, Sync}
import com.peknight.cats.ext.syntax.iorT.rLiftIT
import com.peknight.docker.Identifier.ContainerIdentifier
import com.peknight.docker.command.{InspectOptions, RemoveOptions, StopOptions}
import com.peknight.docker.{Identifier, docker}
import com.peknight.error.Error
import com.peknight.error.syntax.applicativeError.aeiAsIT
import com.peknight.logging.syntax.iorT.log
import com.peknight.os.fs2.isSuccess
import fs2.Compiler
import fs2.io.process.{Process, ProcessBuilder, Processes}
import org.typelevel.log4cats.Logger

package object command:
  def inspect[F[_]: Processes](identifier: Identifier)(options: InspectOptions = InspectOptions.default)
  : Resource[F, Process[F]] =
    ProcessBuilder(docker, com.peknight.docker.command.inspect :: options.options ::: identifier.value :: Nil).spawn[F]

  def exists[F[_]](identifier: Identifier)(using MonadCancel[F, Throwable], Processes[F], Compiler[F, F])
  : IorT[F, Error, Boolean] =
    inspect[F](identifier)().use(isSuccess).aeiAsIT

  def stop[F[_]: Processes](head: ContainerIdentifier, tail: ContainerIdentifier*)(options: StopOptions = StopOptions.default)
  : Resource[F, Process[F]] =
    ProcessBuilder(docker, com.peknight.docker.command.stop :: options.options ::: head.value :: tail.toList.map(_.value)).spawn[F]

  def remove[F[_]: Processes](head: ContainerIdentifier, tail: ContainerIdentifier*)(options: RemoveOptions = RemoveOptions.default)
  : Resource[F, Process[F]] =
    ProcessBuilder(docker, com.peknight.docker.command.remove :: options.options ::: head.value :: tail.toList.map(_.value)).spawn[F]

  def stopAndRemoveContainerIfExists[F[_]: {Sync, Processes, Logger}](container: ContainerIdentifier): IorT[F, Error, Unit] =
    type G[X] = IorT[F, Error, X]
    val stopAndRemove: IorT[F, Error, Unit] =
      for
        _ <- stop[F](container)().use(isSuccess).aeiAsIT.log("Docker#stop", Some(container))
        _ <- remove[F](container)(RemoveOptions(force = true)).use(isSuccess).aeiAsIT.log("Docker#remove", Some(container))
      yield
        ()
    Monad[G].ifM[Unit](exists[F](container))(stopAndRemove, ().rLiftIT)

end command
