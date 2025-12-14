package com.peknight.docker.client

import cats.Monad
import cats.data.IorT
import cats.effect.{MonadCancel, Resource, Sync}
import com.peknight.cats.ext.syntax.iorT.rLiftIT
import com.peknight.docker.Identifier.{ContainerIdentifier, ImageIdentifier}
import com.peknight.docker.command.inspect.InspectOptions
import com.peknight.docker.command.remove.RemoveOptions
import com.peknight.docker.command.run.RunOptions
import com.peknight.docker.command.stop.StopOptions
import com.peknight.docker.{Identifier, docker}
import com.peknight.error.Error
import com.peknight.error.syntax.applicativeError.aeiAsIT
import com.peknight.logging.syntax.iorT.log
import com.peknight.os.process.isSuccess
import fs2.Compiler
import fs2.io.process.{Process, ProcessBuilder, Processes}
import org.typelevel.log4cats.Logger

package object command:
  def inspect[F[_]: Processes](identifier: Identifier)(options: InspectOptions = InspectOptions.default)
  : Resource[F, Process[F]] =
    ProcessBuilder(docker, com.peknight.docker.command.inspect.command :: options.options ::: identifier.value :: Nil).spawn[F]

  def exists[F[_]](identifier: Identifier)(using MonadCancel[F, Throwable], Processes[F], Compiler[F, F])
  : IorT[F, Error, Boolean] =
    inspect[F](identifier)().use(isSuccess).aeiAsIT

  def stop[F[_]: Processes](head: ContainerIdentifier, tail: ContainerIdentifier*)(options: StopOptions = StopOptions.default)
  : Resource[F, Process[F]] =
    ProcessBuilder(docker, com.peknight.docker.command.stop.command :: options.options ::: head.value :: tail.toList.map(_.value)).spawn[F]

  def remove[F[_]: Processes](head: ContainerIdentifier, tail: ContainerIdentifier*)(options: RemoveOptions = RemoveOptions.default)
  : Resource[F, Process[F]] =
    ProcessBuilder(docker, com.peknight.docker.command.remove.command :: options.options ::: head.value :: tail.toList.map(_.value)).spawn[F]

  def run[F[_]: Processes](image: ImageIdentifier)(options: RunOptions = RunOptions.default, command: Option[String] = None, args: List[String] = Nil)
  : Resource[F, Process[F]] =
    ProcessBuilder(docker, com.peknight.docker.command.run.command :: options.options ::: image.value :: command.toList ::: args).spawn[F]

  def stopAndRemoveContainerIfExists[F[_]: {Sync, Processes, Logger}](container: ContainerIdentifier): IorT[F, Error, Unit] =
    type G[X] = IorT[F, Error, X]
    val stopAndRemove: IorT[F, Error, Unit] =
      for
        _ <- stop[F](container)().use(isSuccess).aeiAsIT.log("Docker#stop", Some(container))
        _ <- remove[F](container)(RemoveOptions(force = Some(true))).use(isSuccess).aeiAsIT.log("Docker#remove", Some(container))
      yield
        ()
    Monad[G].ifM[Unit](exists[F](container))(stopAndRemove, ().rLiftIT)
end command
