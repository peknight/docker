package com.peknight.docker.client

import cats.effect.Resource
import com.peknight.docker.Identifier.{ContainerIdentifier, ImageIdentifier, ImageRepositoryTag}
import com.peknight.docker.command.build.BuildOptions
import com.peknight.docker.command.inspect.InspectOptions
import com.peknight.docker.command.pull.PullOptions
import com.peknight.docker.command.remove.{RemoveImageOptions, RemoveOptions}
import com.peknight.docker.command.run.RunOptions
import com.peknight.docker.command.stop.StopOptions
import com.peknight.docker.{Identifier, docker}
import fs2.io.file.Path
import fs2.io.process.{Process, ProcessBuilder, Processes}
import org.http4s.Uri

package object command:
  def build[F[_]: Processes](context: Path | Uri)(options: BuildOptions = BuildOptions.default): Resource[F, Process[F]] =
    ProcessBuilder(docker, com.peknight.docker.command.build.command :: options.options ::: context.toString :: Nil)
      .spawn[F]

  def inspect[F[_]: Processes](identifier: Identifier)(options: InspectOptions = InspectOptions.default)
  : Resource[F, Process[F]] =
    ProcessBuilder(docker, com.peknight.docker.command.inspect.command :: options.options ::: identifier.value :: Nil)
      .spawn[F]

  def pull[F[_]: Processes](image: ImageRepositoryTag)(options: PullOptions = PullOptions.default)
  : Resource[F, Process[F]] =
    ProcessBuilder(docker, com.peknight.docker.command.pull.command :: options.options ::: image.value :: Nil).spawn[F]

  def rm[F[_] : Processes](head: ContainerIdentifier, tail: ContainerIdentifier*)
                          (options: RemoveOptions = RemoveOptions.default): Resource[F, Process[F]] =
    ProcessBuilder(docker, com.peknight.docker.command.remove.command :: options.options ::: head.value ::
      tail.toList.map(_.value)).spawn[F]

  def rmi[F[_] : Processes](head: ImageIdentifier, tail: ImageIdentifier*)
                           (options: RemoveImageOptions = RemoveImageOptions.default): Resource[F, Process[F]] =
    ProcessBuilder(docker, com.peknight.docker.command.remove.rmiCommand :: options.options ::: head.value ::
      tail.toList.map(_.value)).spawn[F]

  def run[F[_] : Processes](image: ImageIdentifier)(options: RunOptions = RunOptions.default,
                                                    command: Option[String] = None, args: List[String] = Nil)
  : Resource[F, Process[F]] =
    ProcessBuilder(docker, com.peknight.docker.command.run.command :: options.options ::: image.value ::
      command.toList ::: args).spawn[F]

  def stop[F[_]: Processes](head: ContainerIdentifier, tail: ContainerIdentifier*)
                           (options: StopOptions = StopOptions.default)
  : Resource[F, Process[F]] =
    ProcessBuilder(docker, com.peknight.docker.command.stop.command :: options.options ::: head.value ::
      tail.toList.map(_.value)).spawn[F]

  def tag[F[_]: Processes](sourceImage: ImageRepositoryTag, targetImage: ImageRepositoryTag): Resource[F, Process[F]] =
    ProcessBuilder(docker, com.peknight.docker.command.tag.command :: sourceImage.value :: targetImage.value :: Nil).spawn[F]
end command
