package com.peknight.docker.command.run

import cats.syntax.either.*
import cats.{Applicative, Show}
import com.peknight.codec.Codec
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.error.{DecodingFailure, NoSuchEnum}
import com.peknight.codec.sum.StringType

import scala.util.Try

enum RestartPolicy:
  case no extends RestartPolicy
  case `on-failure`(maxRetries: Int) extends RestartPolicy
  case always extends RestartPolicy
  case `unless-stopped` extends RestartPolicy
end RestartPolicy
object RestartPolicy:
  given stringCodecRestartPolicy[F[_]: Applicative]: Codec[F, String, String, RestartPolicy] =
    Codec.applicative[F, String, String, RestartPolicy] {
      case `on-failure`(maxRetries) => s"on-failure:$maxRetries"
      case restartPolicy => s"$restartPolicy"
    } {
      case "no" => no.asRight
      case "always" => always.asRight
      case "unless-stopped" => `unless-stopped`.asRight
      case policy if policy.startsWith("on-failure:") =>
        Try(policy.drop(11).toInt).map(`on-failure`.apply).toEither.left.map(DecodingFailure.apply)
      case policy => NoSuchEnum(policy).asLeft
    }

  given codecRestartPolicy[F[_]: Applicative, S: {StringType, Show}]: Codec[F, S, Cursor[S], RestartPolicy] =
    Codec.codecS[F, S, RestartPolicy]
end RestartPolicy