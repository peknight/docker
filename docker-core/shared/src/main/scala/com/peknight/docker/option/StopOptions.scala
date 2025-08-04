package com.peknight.docker.option

import com.peknight.os.Signal

import scala.concurrent.duration.Duration

case class StopOptions(signal: Option[Signal] = None, timeout: Option[Duration] = None) extends DockerOptions:
  def options: List[String] = signal.toList.flatMap(s => List("-s", s"$s")) ::: timeout.toList.map {
    case t if t.isFinite => s"${t.toSeconds}"
    case _ => "-1"
  }.flatMap(t => List("-t", t))
end StopOptions
object StopOptions:
  val default: StopOptions = StopOptions()
end StopOptions
