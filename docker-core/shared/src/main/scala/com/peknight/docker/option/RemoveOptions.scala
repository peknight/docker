package com.peknight.docker.option

case class RemoveOptions(force: Boolean = false) extends DockerOptions:
  def options: List[String] = if force then List("-f") else Nil
end RemoveOptions
object RemoveOptions:
  val default: RemoveOptions = RemoveOptions()
end RemoveOptions

