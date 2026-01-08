package com.peknight.docker

import com.comcast.ip4s.host
import com.peknight.build.gav.peknight.version
import com.peknight.docker.Identifier.ImageRepositoryTag
import com.peknight.ip4s.HostPort

package object custom:
  val registry: HostPort = HostPort(host"docker.peknight.com")
  val namespace: Namespace = Namespace("peknight")
  val tag: Tag = Tag(version)
  def repository(repo: String): Repository = Repository(Some(registry), Some(namespace), repo)
  def image(repo: String, tag: Option[Tag] = Some(tag)): ImageRepositoryTag = ImageRepositoryTag(repository(repo), tag)
  def backupTag(version: String = version, identifier: Option[String]): Tag =
    Tag(s"$version-backup${identifier.filter(_.nonEmpty).fold("")(id => s"-$id")}")
  val backupTag: Tag = backupTag(version, None)
  def backupImage(repo: String, tag: Tag = backupTag): ImageRepositoryTag = image(repo, Some(tag))
end custom
