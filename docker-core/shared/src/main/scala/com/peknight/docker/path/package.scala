package com.peknight.docker

import com.peknight.fs2.io.file.path.{Root, `var`, etc, opt}
import fs2.io.file.Path

package object path:
  val docker: Path = Root / opt / com.peknight.docker.docker
  val Dockerfile: Path = Path("Dockerfile")
  val daemonJson: Path = Root / etc / com.peknight.docker.docker / "daemon.json"
  val dockerSock: Path = Root / `var` / "run" / s"${com.peknight.docker.docker}.sock"
  def `.docker`(home: Path): Path = home / Path(".docker")
  def configJson(home: Path): Path = `.docker`(home) / "config.json"
end path
