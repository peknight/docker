package com.peknight.docker.command

import com.peknight.docker
import com.peknight.os.Options

case class InspectOptions() extends Options:
  def options: List[String] = Nil
end InspectOptions
object InspectOptions:
  def default: InspectOptions = InspectOptions()
end InspectOptions
