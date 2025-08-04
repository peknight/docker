package com.peknight.docker

import cats.Show

sealed trait Identifier:
  def value: String
end Identifier
object Identifier:
  sealed trait Name extends Identifier
  sealed trait Id extends Identifier

  sealed trait ImageIdentifier extends Identifier
  case class ImageName(value: String) extends ImageIdentifier with Name
  case class ImageId(value: String) extends ImageIdentifier with Id

  sealed trait ContainerIdentifier extends Identifier
  case class ContainerName(value: String) extends ContainerIdentifier with Name
  case class ContainerId(value: String) extends ContainerIdentifier with Id

  sealed trait VolumeIdentifier extends Identifier
  case class VolumeName(value: String) extends VolumeIdentifier with Name
  case class VolumeId(value: String) extends VolumeIdentifier with Id

  sealed trait NetworkIdentifier extends Identifier
  case class NetworkName(value: String) extends NetworkIdentifier with Name
  case class NetworkId(value: String) extends NetworkIdentifier with Id

  given showIdentifier[I <: Identifier]: Show[I] = Show.show(_.value)
end Identifier
