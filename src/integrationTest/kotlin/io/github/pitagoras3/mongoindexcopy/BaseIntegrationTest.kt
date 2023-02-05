package io.github.pitagoras3.mongoindexcopy

import io.kotest.core.extensions.install
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.testcontainers.TestContainerExtension
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName

internal abstract class BaseIntegrationTest(testImplementation: BaseIntegrationTest.() -> Unit = {}) : StringSpec() {
  val mongoDb36Container = MongoDBContainer(DockerImageName.parse("mongo:3.6"))
  val mongoDb60Container = MongoDBContainer(DockerImageName.parse("mongo:6.0"))

  init {
    install(TestContainerExtension(mongoDb36Container))
    install(TestContainerExtension(mongoDb60Container))
    testImplementation()
  }
}
