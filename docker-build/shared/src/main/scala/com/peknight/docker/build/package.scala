package com.peknight.docker

import com.comcast.ip4s.host
import com.peknight.app.build.apolloconfig.apollo.version as apolloVersion
import com.peknight.app.build.xuxueli.`xxl-job`.version as xxlJobVersion
import com.peknight.docker.Identifier.ImageRepositoryTag
import com.peknight.ip4s.HostPort

package object build:
  object library:
    /** @versionCheck https://hub.docker.com/v2/repositories/library/alpine/tags */
    val alpine: ImageRepositoryTag = ImageRepositoryTag(Repository(None, None, "alpine"), Some(Tag("3.23.4")))
    /** @versionCheck https://hub.docker.com/v2/repositories/library/eclipse-temurin/tags */
    val `eclipse-temurin`: ImageRepositoryTag = ImageRepositoryTag(Repository(None, None, "eclipse-temurin"), Some(Tag("26_35-jdk")))
    /** @versionCheck https://hub.docker.com/v2/repositories/library/mysql/tags */
    val mysql: ImageRepositoryTag = ImageRepositoryTag(Repository(None, None, "mysql"), Some(Tag("9.7.0")))
    /** @versionCheck https://hub.docker.com/v2/repositories/library/nginx/tags */
    val nginx: ImageRepositoryTag = ImageRepositoryTag(Repository(None, None, "nginx"), Some(Tag("1.30.0-alpine3.23")))
    /** @versionCheck https://hub.docker.com/v2/repositories/library/postgres/tags */
    val postgres: ImageRepositoryTag = ImageRepositoryTag(Repository(None, None, "postgres"), Some(Tag("18.3-alpine3.23")))
    /** @versionCheck https://hub.docker.com/v2/repositories/library/redis/tags */
    val redis: ImageRepositoryTag = ImageRepositoryTag(Repository(None, None, "redis"), Some(Tag("8.6.3-alpine3.23")))
    /** @versionCheck https://hub.docker.com/v2/repositories/library/ubuntu/tags */
    val ubuntu: ImageRepositoryTag = ImageRepositoryTag(Repository(None, None, "ubuntu"), Some(Tag("26.04")))
  end library
  object alpine:
    /** @versionCheck https://hub.docker.com/v2/repositories/alpine/psql/tags */
    val psql: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("alpine")), "psql"), Some(Tag("18.3")))
  end alpine
  object apolloconfig:
    val namespace: Namespace = Namespace("apolloconfig")
    private val tag: Tag = Tag(apolloVersion)
    // @versionCheck skip (external module reference)
    val `apollo-configservice`: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(namespace), "apollo-configservice"), Some(tag))
    val `apollo-adminservice`: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(namespace), "apollo-adminservice"), Some(tag))
    val `apollo-portal`: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(namespace), "apollo-portal"), Some(tag))
  end apolloconfig
  object gitea:
    /** @versionCheck https://hub.docker.com/v2/repositories/gitea/gitea/tags */
    val gitea: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("gitea")), "gitea"), Some(Tag("1.26.1-rootless")))
  end gitea
  object jenkins:
    /** @versionCheck https://hub.docker.com/v2/repositories/jenkins/jenkins/tags */
    val jenkins: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("jenkins")), "jenkins"), Some(Tag("2.563-jdk25")))
  end jenkins
  object ollama:
    /** @versionCheck https://hub.docker.com/v2/repositories/ollama/ollama/tags */
    val ollama: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("ollama")), "ollama"), Some(Tag("0.23.2")))
  end ollama
  object openclaw:
    /** @versionCheck https://api.github.com/orgs/openclaw/packages/container/openclaw/versions */
    val openclaw: ImageRepositoryTag = ImageRepositoryTag(Repository(Some(HostPort(host"ghcr.io")), Some(Namespace("openclaw")), "openclaw"), Some(Tag("2026.5.7")))
  end openclaw
  object paulgauthier:
    /** @versionCheck https://hub.docker.com/v2/repositories/paulgauthier/aider/tags */
    val aider: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("paulgauthier")), "aider"), Some(Tag("v0.86.2")))
  end paulgauthier
  object sonatype:
    /** @versionCheck https://hub.docker.com/v2/repositories/sonatype/nexus3/tags */
    val nexus3: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("sonatype")), "nexus3"), Some(Tag("3.92.0-alpine")))
  end sonatype
  object rustdesk:
    /** @versionCheck https://hub.docker.com/v2/repositories/rustdesk/rustdesk-server/tags */
    val `rustdesk-server`: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("rustdesk")), "rustdesk-server"), Some(Tag("1.1.15")))
  end rustdesk
  object v2fly:
    /** @versionCheck https://hub.docker.com/v2/repositories/v2fly/v2fly-core/tags */
    val `v2fly-core`: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("v2fly")), "v2fly-core"), Some(Tag("v5.41.0")))
  end v2fly
  object xuxueli:
    // @versionCheck skip (external module reference)
    val `xxl-job-admin`: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("xuxueli")), "xxl-job-admin"), Some(Tag(xxlJobVersion)))
  end xuxueli
end build
