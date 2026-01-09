package com.peknight.docker.custom

import cats.Monad
import cats.data.IorT
import cats.effect.Sync
import cats.syntax.applicative.*
import cats.syntax.option.*
import com.comcast.ip4s.{Cidr, Ipv4Address, ipv4}
import com.peknight.app.AppName
import com.peknight.cats.syntax.iorT.rLiftIT
import com.peknight.docker.command.network.create.NetworkCreateOptions
import com.peknight.docker.command.run.Permission.ro
import com.peknight.docker.command.run.{RestartPolicy, RunOptions, VolumeMount}
import com.peknight.docker.custom.{backupImage as customBackupImage, container as customContainer, image as customImage, network as customNetwork}
import com.peknight.docker.custom.{backupImage as customBackupImage, image as customImage}
import com.peknight.docker.network
import com.peknight.docker.path.docker
import com.peknight.docker.service.{createNetworkIfNotExists, removeImageIfExists, renameImageIfExists, run as runContainer}
import com.peknight.error.Error
import com.peknight.error.syntax.applicativeError.asIT
import com.peknight.fs2.io.file.path.*
import fs2.io.file.{Files, Path}
import fs2.io.process.Processes
import org.typelevel.log4cats.Logger

package object service:
  def runScalaApp[F[_]: {Sync, Files, Processes, Logger}](appName: AppName)(env: Map[String, String] = Map.empty)
  : IorT[F, Error, Boolean] =
    val appHome: Path = opt / appName.value
    val logDirectory: Path = varLog / appName.value
    val certsDirectory: Path = appHome / certs
    val logsDirectory: Path = appHome / logs
    type G[X] = IorT[F, Error, X]
    for
      _ <- Files[F].createDirectories(logDirectory).asIT
      _ <- Files[F].createDirectories(certsDirectory).asIT
      _ <- Monad[F].ifM[Unit](Files[F].exists(logsDirectory))(().pure[F],
        Files[F].createSymbolicLink(logsDirectory, logDirectory)).asIT
      image = customImage(appName.value)
      backupImage = customBackupImage(appName.value)
      container = customContainer(appName.value)
      res <- Monad[G].ifM[Boolean](runContainer[F](image, container)(RunOptions(
        restart = RestartPolicy.`unless-stopped`.some,
        env = env,
        volume = List(
          VolumeMount(timezone, timezone, ro.some),
          VolumeMount(localtime, localtime, ro.some),
          VolumeMount(logDirectory, docker / logs),
          VolumeMount(certsDirectory, docker / certs)
        ),
        network = network.host.some
      )))(removeImageIfExists[F](backupImage)(), false.rLiftIT)
    yield
      res

  def renameImageAsBackup[F[_]: {Sync, Processes, Logger}](appName: AppName): IorT[F, Error, Boolean] =
    renameImageIfExists[F](customImage(appName.value), customBackupImage(appName.value))()

  def createNetwork[F[_]: {Sync, Processes, Logger}]: IorT[F, Error, Boolean] =
    createNetworkIfNotExists[F](customNetwork)(NetworkCreateOptions(
      Cidr[Ipv4Address](ipv4"172.18.0.0", 16).some, ipv4"172.18.0.1".some))
end service
