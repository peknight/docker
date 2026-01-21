package com.peknight.docker

import cats.Monad
import cats.data.IorT
import cats.effect.{MonadCancel, Sync}
import cats.syntax.option.*
import com.comcast.ip4s.Hostname
import com.peknight.cats.syntax.iorT.rLiftIT
import com.peknight.docker.Identifier
import com.peknight.docker.Identifier.{ContainerName, ImageIdentifier, ImageRepositoryTag, NetworkName}
import com.peknight.docker.client.command.network.create as createNetwork
import com.peknight.docker.client.command.{inspect, pull, rm, rmi, stop, tag, run as runContainer}
import com.peknight.docker.command.network.create.NetworkCreateOptions
import com.peknight.docker.command.pull.PullOptions
import com.peknight.docker.command.remove.{RemoveImageOptions, RemoveOptions}
import com.peknight.docker.command.run.RunOptions
import com.peknight.error.Error
import com.peknight.error.syntax.applicativeError.aeiAsIT
import com.peknight.logging.syntax.iorT.log
import com.peknight.os.process.isSuccess
import fs2.Compiler
import fs2.io.process.Processes
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.extras.LogLevel

package object service:

  def exists[F[_]](identifier: Identifier)(using MonadCancel[F, Throwable], Processes[F], Compiler[F, F])
  : IorT[F, Error, Boolean] =
    inspect[F](identifier)().use(isSuccess).aeiAsIT

  private def ifExists[F[_], I <: Identifier, A](identifier: I)(iorT: IorT[F, Error, A])
                                                (using MonadCancel[F, Throwable], Processes[F], Compiler[F, F])
  : IorT[F, Error, Option[A]] =
    type G[X] = IorT[F, Error, X]
    Monad[G].ifM[Option[A]](exists[F](identifier))(iorT.map(_.some), none[A].rLiftIT)

  private def ifNotExists[F[_], I <: Identifier, A](identifier: I)(iorT: IorT[F, Error, A])
                                                   (using MonadCancel[F, Throwable], Processes[F], Compiler[F, F])
  : IorT[F, Error, Option[A]] =
    type G[X] = IorT[F, Error, X]
    Monad[G].ifM[Option[A]](exists[F](identifier))(none[A].rLiftIT, iorT.map(_.some))

  def createNetworkIfNotExists[F[_]: {Sync, Processes, Logger}](network: NetworkName)(
    networkCreateOptions: NetworkCreateOptions = NetworkCreateOptions.default): IorT[F, Error, Boolean] =
    ifNotExists[F, NetworkName, Boolean](network) {
      createNetwork[F](network)(networkCreateOptions).use(isSuccess).aeiAsIT.log("Docker#networkCreate", Some(network))
    }.map(_.getOrElse(true))

  def pullIfNotExists[F[_]: {Sync, Processes, Logger}](image: ImageRepositoryTag)
                                                      (pullOptions: PullOptions = PullOptions.default)
  : IorT[F, Error, Boolean] =
    ifNotExists[F, ImageRepositoryTag, Boolean](image) {
      pull[F](image)(pullOptions).use(isSuccess).aeiAsIT.log("Docker#pull", Some(image), startLevel = Some(LogLevel.Info))
    }.map(_.getOrElse(true))

  def removeImageIfExists[F[_]: {Sync, Processes, Logger}](image: ImageIdentifier)(
    removeImageOptions: RemoveImageOptions = RemoveImageOptions.default): IorT[F, Error, Boolean] =
    ifExists[F, ImageIdentifier, Boolean](image) {
      rmi[F](image)(removeImageOptions).use(isSuccess).aeiAsIT.log("Docker#removeImage", Some(image))
    }.map(_.getOrElse(true))

  def renameImageIfExists[F[_]: {Sync, Processes, Logger}]
                         (sourceImage: ImageRepositoryTag, targetImage: ImageRepositoryTag)
                         (removeImageOptions: RemoveImageOptions = RemoveImageOptions.default): IorT[F, Error, Boolean] =
    type G[X] = IorT[F, Error, X]
    ifExists[F, ImageRepositoryTag, Boolean](sourceImage)(Monad[G]
      .ifM[Boolean](tag[F](sourceImage, targetImage).use(isSuccess).aeiAsIT.log("Docker#tag", Some((sourceImage, targetImage))))(
        rmi[F](sourceImage)(removeImageOptions).use(isSuccess).aeiAsIT.log("Docker#removeImage", Some(sourceImage)),
        false.rLiftIT))
      .map(_.getOrElse(true))

  def run[F[_]: {Sync, Processes, Logger}](image: ImageIdentifier, container: ContainerName)
                                          (runOptions: RunOptions = RunOptions.default,
                                           command: Option[String] = None,
                                           args: List[String] = Nil): IorT[F, Error, Boolean] =
    for
      _ <- ifExists[F, ContainerName, Unit](container) {
        for
          _ <- stop[F](container)().use(isSuccess).aeiAsIT
            .log("Docker#stop", Some(container), startLevel = Some(LogLevel.Info))
          _ <- rm[F](container)(RemoveOptions(force = Some(true))).use(isSuccess).aeiAsIT
            .log("Docker#remove", Some(container), startLevel = Some(LogLevel.Info))
        yield
          ()
      }
      res <- runContainer[F](image)(runOptions.copy(
        detach = runOptions.detach.orElse(true.some),
        hostname = runOptions.hostname.orElse(Hostname.fromString(container.value)),
        name = runOptions.name.orElse(container.some),
      ), command, args).use(isSuccess[F]).aeiAsIT.log("Docker#run", Some(container), startLevel = Some(LogLevel.Info))
    yield
      res
end service
