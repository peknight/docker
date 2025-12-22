package com.peknight.docker.command.network.create

import cats.{Monad, Show}
import com.comcast.ip4s.{Cidr, IpAddress}
import com.peknight.codec.Codec
import com.peknight.codec.config.given
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.ip4s.instances.cidr.given
import com.peknight.codec.ip4s.instances.host.given
import com.peknight.codec.sum.{NullType, ObjectType, StringType}
import com.peknight.docker.option.DockerOptions
import com.peknight.query.option.given
import com.peknight.query.syntax.id.query.toOptions


case class NetworkCreateOptions(
                                 subnet: Option[Cidr[IpAddress]] = None,
                                 gateway: Option[IpAddress] = None
                               ) extends DockerOptions:
  def options: List[String] = this.toOptions

end NetworkCreateOptions
object NetworkCreateOptions:
  val default: NetworkCreateOptions = NetworkCreateOptions()

  given codecNetworkCreateOptions[F[_]: Monad, S: {ObjectType, NullType, StringType, Show}]
  : Codec[F, S, Cursor[S], NetworkCreateOptions] =
    Codec.derived[F, S, NetworkCreateOptions]
end NetworkCreateOptions

