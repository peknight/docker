package com.peknight.docker.command

import com.peknight.os.Options

case class RunOptions() extends Options:
  def options: List[String] = Nil
end RunOptions
object RunOptions:
  val default: RunOptions = RunOptions()
end RunOptions
