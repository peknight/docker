package com.peknight.docker

import cats.{Applicative, Show}
import com.peknight.codec.Encoder

sealed trait Identifier:
  def value: String
end Identifier
object Identifier:
  sealed trait Name extends Identifier
  sealed trait Id extends Identifier

  sealed trait ImageIdentifier extends Identifier
  case class ImageRepositoryTag(repository: String, tag: Option[String] = None) extends ImageIdentifier:
    def value: String = s"$repository${tag.fold("")(t => s":$t")}"
  end ImageRepositoryTag
  case class ImageId(value: String) extends ImageIdentifier with Id

  sealed trait ContainerIdentifier extends Identifier
  case class ContainerName(value: String) extends ContainerIdentifier with Name
  case class ContainerId(value: String) extends ContainerIdentifier with Id

  sealed trait VolumeIdentifier extends Identifier
  case class VolumeName(value: String) extends VolumeIdentifier with Name

  sealed trait NetworkIdentifier extends Identifier
  case class NetworkName(value: String) extends NetworkIdentifier with Name
  case class NetworkId(value: String) extends NetworkIdentifier with Id

  given showIdentifier[I <: Identifier]: Show[I] = Show.show(_.value)
  given stringEncodeIdentifier[F[_]: Applicative, I <: Identifier]: Encoder[F, String, I] =
    Encoder.applicative[F, String, I](_.value)
end Identifier
