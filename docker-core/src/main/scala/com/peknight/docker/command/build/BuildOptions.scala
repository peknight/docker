package com.peknight.docker.command.build

import cats.{Monad, Show}
import com.peknight.codec.config.given
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.sum.{NullType, ObjectType, StringType}
import com.peknight.codec.{Codec, Encoder}
import com.peknight.docker.Identifier.ImageRepositoryTag
import com.peknight.docker.option.DockerOptions
import com.peknight.query.Query
import com.peknight.query.Query.given
import com.peknight.query.option.OptionConfig
import com.peknight.query.option.OptionKey.ShortOption
import com.peknight.query.syntax.id.query.toOptions

case class BuildOptions(tag: Option[ImageRepositoryTag] = None) extends DockerOptions:
  def options: List[String] =
    given OptionConfig = OptionConfig.transformObjectKey() {
      case "tag" => List(ShortOption('t'))
    }
    this.toOptions
end BuildOptions
object BuildOptions:
  def default: BuildOptions = BuildOptions()
  given codecBuildOptions[F[_]: Monad, S: {ObjectType, NullType, StringType, Show}]: Codec[F, S, Cursor[S], BuildOptions] =
    Codec.derived[F, S, BuildOptions]
end BuildOptions
