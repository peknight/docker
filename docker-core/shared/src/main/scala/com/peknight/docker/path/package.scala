package com.peknight.docker

import fs2.io.file.Path

package object path:
  val docker: Path = Path("/opt/docker")
end path
