package com.peknight.docker

import com.peknight.docker.Identifier.ImageRepositoryTag

package object build:
  object library:
    // https://hub.docker.com/_/postgres/tags
    val postgres: ImageRepositoryTag = ImageRepositoryTag(Repository(None, None, "postgres"), Some(Tag("18.1-alpine")))
  end library
  object alpine:
    // https://hub.docker.com/r/alpine/psql/tags
    val psql: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("alpine")), "psql"), Some(Tag("18.1")))
  end alpine
  object gitea:
    // https://hub.docker.com/r/gitea/gitea/tags
    val gitea: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("gitea")), "gitea"), Some(Tag("1.25.3-rootless")))
  end gitea
  object jenkins:
    // https://hub.docker.com/r/jenkins/jenkins/tags
    val jenkins: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("jenkins")), "jenkins"), Some(Tag("2.545")))
  end jenkins
  object sonatype:
    // https://hub.docker.com/r/sonatype/nexus3/tags
    val nexus3: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("sonatype")), "nexus3"), Some(Tag("3.87.1-alpine")))
  end sonatype
  object rustdesk:
    // https://hub.docker.com/r/rustdesk/rustdesk-server/tags
    val `rustdesk-server`: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("rustdesk")), "rustdesk-server"), Some(Tag("1.1.14")))
  end rustdesk
  object v2fly:
    // https://hub.docker.com/r/v2fly/v2fly-core/tags
    val `v2fly-core`: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("v2fly")), "v2fly-core"), Some(Tag("v5.41.0")))
  end v2fly
end build
