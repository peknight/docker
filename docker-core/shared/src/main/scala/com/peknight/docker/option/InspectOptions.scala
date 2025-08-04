package com.peknight.docker.option

case class InspectOptions() extends DockerOptions:
  def options: List[String] = Nil
end InspectOptions
object InspectOptions:
  def default: InspectOptions = InspectOptions()
end InspectOptions
