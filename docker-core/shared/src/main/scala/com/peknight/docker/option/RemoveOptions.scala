package com.peknight.docker.option

import com.peknight.docker
import com.peknight.os.Options

case class RemoveOptions(force: Boolean = false) extends Options:
  def options: List[String] = if force then List("-f") else Nil
end RemoveOptions
object RemoveOptions:
  val default: RemoveOptions = RemoveOptions()
end RemoveOptions

