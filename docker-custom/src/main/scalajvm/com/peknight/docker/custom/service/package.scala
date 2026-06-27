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
import com.peknight.docker.service.{buildIfNotExists, createNetworkIfNotExists, dockerInDockerVolume, dockerSockGroup, pullIfNotExists, removeImageIfExists, renameImageIfExists, run as runContainer}
import com.peknight.error.Error
import com.peknight.error.syntax.applicativeError.asIT
import com.peknight.fs2.io.file.path.*
import com.peknight.fs2.io.syntax.path.writeString
import fs2.io.file.{Files, Path}
import fs2.io.process.Processes
import org.typelevel.log4cats.Logger

package object service:
  def runScalaApp[F[_]: {Sync, Files, Processes, Logger}](appName: AppName, home: Path,
                                                          certificatesDirectory: Option[Path] = None,
                                                          containerHome: Option[Path] = None,
                                                          acme: Boolean = true,
                                                          host: Boolean = true,
                                                          mountTimezone: Boolean = true,
                                                          dockerInDocker: Boolean = false)
                                                         (runOptions: RunOptions = RunOptions.default)
  : IorT[F, Error, Boolean] =
    val appHome: Path = home / `.local` / opt / appName.value
    val certsDirectory: Path = certificatesDirectory.getOrElse(appHome / certs)
    val logsDirectory: Path = appHome / logs
    type G[X] = IorT[F, Error, X]
    for
      _ <- if acme then Files[F].createDirectories(certsDirectory).asIT else ().rLiftIT
      _ <- Files[F].createDirectories(logsDirectory).asIT
      sockGroups <- if dockerInDocker then dockerSockGroup[F].map(_ :: Nil) else Nil.rLiftIT
      dockerVolumes <-
        if dockerInDocker then dockerInDockerVolume[F](home, containerHome.getOrElse(Root / root)) else Nil.rLiftIT
      certVolumes = if acme then List(VolumeMount(certsDirectory, docker / certs)) else Nil
      timezoneVolumes = if mountTimezone then List(timezoneVolumeMount) else Nil
      image = customImage(appName)
      backupImage = customBackupImage(appName)
      container = customContainer(appName)
      volume = VolumeMount(logsDirectory, docker / logs) :: certVolumes ::: timezoneVolumes ::: localtimeVolumeMount ::
        dockerVolumes ::: runOptions.volume
      res <- Monad[G].ifM[Boolean](runContainer[F](image, container)(runOptions.copy(
        groupAdd = sockGroups ::: runOptions.groupAdd,
        restart = runOptions.restart.getOrElse(RestartPolicy.`unless-stopped`).some,
        volume = volume,
        network = runOptions.network.orElse(if host then network.host.some else none)
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
      _ <- (context / "Dockerfile").writeString[F](dockerfile).asIT
      res <- buildIfNotExists[F](image, context)(buildOptions)
    yield
      res
end service
