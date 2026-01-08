package com.peknight.docker

import cats.{Applicative, Show}
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.sum.StringType
import com.peknight.codec.{Decoder, Encoder}

import scala.reflect.ClassTag

sealed trait Identifier:
  def value: String
end Identifier
object Identifier:
  case class RawIdentifier(value: String) extends Identifier

  sealed trait Name extends Identifier
  case class RawName(value: String) extends Name

  sealed trait Id extends Identifier
  case class RawId(value: String) extends Id

  sealed trait ImageIdentifier extends Identifier
  case class ImageRepositoryTag(repository: Repository, tag: Option[Tag] = None) extends ImageIdentifier:
    def value: String = s"$repository${tag.fold("")(t => s":$t")}"
  end ImageRepositoryTag
  object ImageRepositoryTag:
    def fromString(value: String): Option[ImageRepositoryTag] =
      Repository.fromStringF[ImageRepositoryTag](value) { (registry, namespace, last) =>
        if last.startsWith(":") || last.endsWith(":") then None else
          val index = last.indexOf(':')
          val (repository, tag) =
            if index >= 0 then (last.substring(0, index), Some(Tag(last.substring(index + 1)))) else (last, None)
          Some(ImageRepositoryTag(Repository(registry, namespace, repository), tag))
      }
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

  given encodeIdentifierS[F[_]: Applicative, S: StringType, I <: Identifier]: Encoder[F, S, I] =
    Encoder.encodeS[F, S, I]

  given stringDecodeRawIdentifier[F[_]: Applicative]: Decoder[F, String, RawIdentifier] =
    Decoder.map(RawIdentifier.apply)
  given stringDecodeIdentifier[F[_]: Applicative]: Decoder[F, String, Identifier] =
    Decoder.map(RawIdentifier.apply)
  given stringDecodeRawName[F[_]: Applicative]: Decoder[F, String, RawName] =
    Decoder.map(RawName.apply)
  given stringDecodeName[F[_]: Applicative]: Decoder[F, String, Name] =
    Decoder.map(RawName.apply)
  given stringDecodeRawId[F[_]: Applicative]: Decoder[F, String, RawId] =
    Decoder.map(RawId.apply)
  given stringDecodeId[F[_]: Applicative]: Decoder[F, String, Id] =
    Decoder.map(RawId.apply)
  given stringDecodeImageRepositoryTag[F[_]: Applicative]: Decoder[F, String, ImageRepositoryTag] =
    Decoder.mapOption(ImageRepositoryTag.fromString)
  given stringDecodeImageId[F[_]: Applicative]: Decoder[F, String, ImageId] =
    Decoder.map(ImageId.apply)
  given stringDecodeImageIdentifier[F[_]: Applicative]: Decoder[F, String, ImageIdentifier] =
    stringDecodeImageRepositoryTag.map(identity)
  given stringDecodeContainerName[F[_]: Applicative]: Decoder[F, String, ContainerName] =
    Decoder.map(ContainerName.apply)
  given stringDecodeContainerId[F[_]: Applicative]: Decoder[F, String, ContainerId] =
    Decoder.map(ContainerId.apply)
  given stringDecodeContainerIdentifier[F[_]: Applicative]: Decoder[F, String, ContainerIdentifier] =
    Decoder.map(ContainerName.apply)
  given stringDecodeVolumeName[F[_]: Applicative]: Decoder[F, String, VolumeName] =
    Decoder.map(VolumeName.apply)
  given stringDecodeVolumeIdentifier[F[_]: Applicative]: Decoder[F, String, VolumeIdentifier] =
    Decoder.map(VolumeName.apply)
  given stringDecodeNetworkName[F[_]: Applicative]: Decoder[F, String, NetworkName] =
    Decoder.map(NetworkName.apply)
  given stringDecodeNetworkId[F[_]: Applicative]: Decoder[F, String, NetworkId] =
    Decoder.map(NetworkId.apply)
  given stringDecodeNetworkIdentifier[F[_]: Applicative]: Decoder[F, String, NetworkIdentifier] =
    Decoder.map(NetworkName.apply)

  given decodeIdentifierS[F[_], S, I <: Identifier](using Applicative[F], StringType[S], Show[S], ClassTag[I], Decoder[F, String, I])
  : Decoder[F, Cursor[S], I] =
    Decoder.decodeS[F, S, I]
end Identifier
