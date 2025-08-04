package com.peknight.docker.option

import com.comcast.ip4s.Hostname
import com.peknight.docker.Identifier.ContainerName
import com.peknight.os.Options

case class RunOptions(detach: Boolean = false, hostname: Option[Hostname] = None, name: Option[ContainerName] = None) extends Options:
  def options: List[String] =
    (if detach then List("-d") else Nil) :::
      hostname.map(h => List("-h", s"$h")).getOrElse(Nil) :::
      name.map(n => List("--name", s"${n.value}")).getOrElse(Nil) :::
      Nil
end RunOptions
object RunOptions:
  val default: RunOptions = RunOptions()
end RunOptions
