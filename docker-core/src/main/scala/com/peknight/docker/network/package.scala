package com.peknight.docker

import com.peknight.docker.Identifier.NetworkName

package object network:
  val host: NetworkName = NetworkName("host")
  val bridge: NetworkName = NetworkName("bridge")
end network
