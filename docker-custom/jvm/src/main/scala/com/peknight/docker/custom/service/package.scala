package com.peknight.docker.custom

import cats.Monad
import cats.data.IorT
import cats.effect.{Async, Sync}
import cats.syntax.option.*
import com.comcast.ip4s.{Cidr, Ipv4Address, ipv4}
import com.peknight.app.AppName
import com.peknight.cats.syntax.iorT.rLiftIT
import com.peknight.docker.Identifier.{ContainerName, ImageRepositoryTag}
import com.peknight.docker.command.build.BuildOptions
import com.peknight.docker.command.network.create.NetworkCreateOptions
import com.peknight.docker.command.run.{RestartPolicy, RunOptions, VolumeMount}
import com.peknight.docker.custom.{backupImage as customBackupImage, container as customContainer, image as customImage, network as customNetwork}
import com.peknight.docker.network
import com.peknight.docker.path.docker
import com.peknight.docker.service.{buildIfNotExists, createNetworkIfNotExists, pullIfNotExists, removeImageIfExists, renameImageIfExists, run as runContainer}
import com.peknight.error.Error
import com.peknight.error.syntax.applicativeError.asIT
import com.peknight.fs2.io.file.path.*
import com.peknight.fs2.io.syntax.path.writeFileIfNotExists
import fs2.Stream
import fs2.io.file.{Files, Path}
import fs2.io.process.Processes
import fs2.text.utf8
import org.typelevel.log4cats.Logger

package object service:
  def runScalaApp[F[_]: {Sync, Files, Processes, Logger}](appName: AppName, home: Path, mountTimezone: Boolean = true)(env: Map[String, String] = Map.empty)
  : IorT[F, Error, Boolean] =
    val appHome: Path = home / `.local` / opt / appName.value
    val certsDirectory: Path = appHome / certs
    val logsDirectory: Path = appHome / logs
    type G[X] = IorT[F, Error, X]
    for
      _ <- Files[F].createDirectories(certsDirectory).asIT
      _ <- Files[F].createDirectories(logsDirectory).asIT
      image = customImage(appName)
      backupImage = customBackupImage(appName)
      container = customContainer(appName)
      volume = List(localtimeVolumeMount, VolumeMount(certsDirectory, docker / certs),
        VolumeMount(logsDirectory, docker / logs))
      res <- Monad[G].ifM[Boolean](runContainer[F](image, container)(RunOptions(
        restart = RestartPolicy.`unless-stopped`.some,
        env = env,
        volume = if mountTimezone then timezoneVolumeMount :: volume else volume,
        network = network.host.some
      )))(removeImageIfExists[F](backupImage)(), false.rLiftIT)
    yield
      res

  def renameImageAsBackup[F[_]: {Sync, Processes, Logger}](appName: AppName): IorT[F, Error, Boolean] =
    renameImageIfExists[F](customImage(appName), customBackupImage(appName))()

  def createNetwork[F[_]: {Sync, Processes, Logger}]: IorT[F, Error, Boolean] =
    createNetworkIfNotExists[F](customNetwork)(NetworkCreateOptions(
      Cidr[Ipv4Address](ipv4"172.18.0.0", 16).some, gateway.some))

  def runNetworkApp[F[_]: {Async, Processes, Logger}](appName: AppName, image: ImageRepositoryTag)
                                                     (runOptions: RunOptions = RunOptions.default,
                                                      command: Option[String] = None,
                                                      args: List[String] = Nil): IorT[F, Error, Boolean] =
    type G[X] = IorT[F, Error, X]
    val container: ContainerName = customContainer(appName)
    Monad[G].ifM[Boolean](pullIfNotExists[F](image)())(
      Monad[G].ifM[Boolean](createNetwork[F])(
        runContainer[F](image, container)(runOptions.copy(network = runOptions.network.orElse(customNetwork.some)),
          command, args),
        false.rLiftIT
      ),
      false.rLiftIT
    )

  def runHostApp[F[_]: {Async, Processes, Logger}](appName: AppName, image: ImageRepositoryTag)
                                                  (runOptions: RunOptions = RunOptions.default,
                                                   command: Option[String] = None,
                                                   args: List[String] = Nil): IorT[F, Error, Boolean] =
    type G[X] = IorT[F, Error, X]
    val container: ContainerName = customContainer(appName)
    Monad[G].ifM[Boolean](pullIfNotExists[F](image)())(
      runContainer[F](image, container)(runOptions.copy(network = runOptions.network.orElse(network.host.some)),
        command, args),
      false.rLiftIT
    )

  def buildImageIfNotExists[F[_]: {Async, Files, Processes, Logger}](image: ImageRepositoryTag, dockerfile: String,
                                                                     context: Path = com.peknight.fs2.io.file.path.docker)
                                                                    (buildOptions: BuildOptions = BuildOptions.default)
  : IorT[F, Error, Boolean] =
    for
      _ <- (context / "Dockerfile").writeFileIfNotExists[F](Stream(dockerfile).covary[F].through(utf8.encode[F])).asIT
      res <- buildIfNotExists[F](image, context)(buildOptions)
    yield
      res
end service
