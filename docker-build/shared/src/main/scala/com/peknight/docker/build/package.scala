package com.peknight.docker

import com.peknight.docker.Identifier.ImageRepositoryTag

package object build:
  // https://hub.docker.com/r/gitea/gitea/tags
  val gitea: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("gitea")), "gitea"), Some(Tag("1.25.3-rootless")))
  // https://hub.docker.com/r/jenkins/jenkins/tags
  val jenkins: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("jenkins")), "jenkins"), Some(Tag("2.545")))
  // https://hub.docker.com/r/sonatype/nexus3/tags
  val nexus: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("sonatype")), "nexus3"), Some(Tag("3.87.1-alpine")))
  // https://hub.docker.com/_/postgres/tags
  val postgres: ImageRepositoryTag = ImageRepositoryTag(Repository(None, None, "postgres"), Some(Tag("18.1-alpine")))
  // https://hub.docker.com/r/alpine/psql/tags
  val psql: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("alpine")), "psql"), Some(Tag("18.1")))
  // https://hub.docker.com/r/rustdesk/rustdesk-server/tags
  val rustdesk: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("rustdesk")), "rustdesk-server"), Some(Tag("1.1.14")))
end build
