package com.peknight.docker

import cats.syntax.option.*
import com.comcast.ip4s.{Hostname, Ipv4Address, host, ipv4}
import com.peknight.app.AppName
import com.peknight.build.gav.peknight.version
import com.peknight.docker.Identifier.{ContainerName, ImageRepositoryTag, NetworkName}
import com.peknight.docker.command.run.Permission.ro
import com.peknight.docker.command.run.VolumeMount
import com.peknight.fs2.io.file.path.{etcLocaltime, etcTimezone}
import com.peknight.ip4s.HostPort

package object custom:
  val registry: HostPort = HostPort(host"docker.peknight.com")
  val namespace: Namespace = Namespace("peknight")
  val tag: Tag = Tag(version)
  val network: NetworkName = NetworkName("pek-network")
  val maintainer: String = "MAINTAINER peknight <JKpeknight@gmail.com>"
  val gateway: Ipv4Address = ipv4"172.18.0.1"

  def repository(appName: AppName): Repository = Repository(Some(registry), Some(namespace), appName.value)
  def image(appName: AppName, tag: Option[Tag] = Some(tag)): ImageRepositoryTag = ImageRepositoryTag(repository(appName), tag)

  def backupTag(version: String = version, identifier: Option[String] = None): Tag =
    Tag(s"${Option(version).filter(_.nonEmpty).fold("")(ver => s"$ver-")}backup${identifier.filter(_.nonEmpty).fold("")(id => s"-$id")}")
  def backupImageRepositoryTag(image: ImageRepositoryTag, identifier: Option[String] = None): ImageRepositoryTag =
    image.copy(tag = backupTag(image.tag.map(_.value).getOrElse(""), identifier).some)
  val backupTag: Tag = backupTag(version, None)
  def backupImage(appName: AppName, tag: Tag = backupTag): ImageRepositoryTag = image(appName, Some(tag))

  def container(appName: AppName): ContainerName = ContainerName(s"pek-$appName")

  def hostname(appName: AppName): Hostname = Hostname.fromString(s"pek-$appName").get

  val timezoneVolumeMount: VolumeMount = VolumeMount(etcTimezone, etcTimezone, ro.some)
  val localtimeVolumeMount: VolumeMount = VolumeMount(etcLocaltime, etcLocaltime, ro.some)
end custom
