package com.peknight.docker

import cats.syntax.option.*
import com.comcast.ip4s.{Hostname, host}
import com.peknight.app.AppName
import com.peknight.build.gav.peknight.version
import com.peknight.docker.Identifier.{ContainerName, ImageRepositoryTag, NetworkName}
import com.peknight.docker.command.run.Permission.ro
import com.peknight.docker.command.run.VolumeMount
import com.peknight.fs2.io.file.path.{localtime, timezone}
import com.peknight.ip4s.HostPort

package object custom:
  val registry: HostPort = HostPort(host"docker.peknight.com")
  val namespace: Namespace = Namespace("peknight")
  val tag: Tag = Tag(version)
  val network: NetworkName = NetworkName("pek-network")

  def repository(appName: AppName): Repository = Repository(Some(registry), Some(namespace), appName.value)
  def image(appName: AppName, tag: Option[Tag] = Some(tag)): ImageRepositoryTag = ImageRepositoryTag(repository(appName), tag)

  def backupTag(version: String = version, identifier: Option[String]): Tag =
    Tag(s"$version-backup${identifier.filter(_.nonEmpty).fold("")(id => s"-$id")}")
  val backupTag: Tag = backupTag(version, None)
  def backupImage(appName: AppName, tag: Tag = backupTag): ImageRepositoryTag = image(appName, Some(tag))

  def container(appName: AppName): ContainerName = ContainerName(s"pek-$appName")

  def hostname(appName: AppName): Hostname = Hostname.fromString(s"pek-$appName").get

  val timezoneVolumeMount: VolumeMount = VolumeMount(timezone, timezone, ro.some)
  val localtimeVolumeMount: VolumeMount = VolumeMount(localtime, localtime, ro.some)
end custom
