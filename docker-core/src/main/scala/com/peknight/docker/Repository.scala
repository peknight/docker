package com.peknight.docker

import cats.{Applicative, Show}
import com.peknight.codec.Codec
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.sum.StringType
import com.peknight.ip4s.HostPort

case class Repository(registry: Option[HostPort], namespace: Option[Namespace], repository: String):
  override def toString: String =
    registry.map(r => s"$r/${namespace.map(n => s"$n").getOrElse("")}/$repository")
      .getOrElse(s"${namespace.map(n => s"$n/").getOrElse("")}$repository")
end Repository
object Repository:
  def fromString(value: String): Option[Repository] =
    fromStringF(value)((registry, namespace, repository) => Some(Repository(registry, namespace, repository)))

  private[docker] def fromStringF[T](value: String)(f: (Option[HostPort], Option[Namespace], String) => Option[T]): Option[T] =
    val trim = value.trim
    val tuple: Option[(Option[HostPort], Option[Namespace], String)] =
      if trim.isBlank || trim.startsWith("/") || trim.endsWith("/") then None else
        trim.count(_ == '/') match
          case 0 => Some((None, None, trim))
          case 1 =>
            val index = trim.indexOf('/')
            Some((None, Some(Namespace(trim.substring(0, index))), trim.substring(index + 1)))
          case 2 =>
            val indexI = trim.indexOf('/')
            val indexJ = trim.lastIndexOf('/')
            HostPort.fromString(trim.substring(0, indexI)).map { registry =>
              val namespace = if indexI + 1 == indexJ then None else Some(Namespace(trim.substring(indexI + 1, indexJ)))
              (Some(registry), namespace, trim.substring(indexJ + 1))
            }
          case _ => None
    tuple.flatMap(f.tupled)

  given stringCodecRepository[F[_]: Applicative]: Codec[F, String, String, Repository] =
    Codec.mapOption[F, String, String, Repository](_.toString)(fromString)

  given codecRepositoryS[F[_]: Applicative, S: {StringType, Show}]: Codec[F, S, Cursor[S], Repository] =
    Codec.codecS[F, S, Repository]
end Repository

