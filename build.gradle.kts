import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.8.0"
  `java-library`
  `maven-publish`
}

group = "io.github.pitagoras3"
version = "1.0.0"

repositories {
  mavenCentral()
}

dependencies {
  api("org.mongodb:mongodb-driver-sync:4.8.2")

  testImplementation(kotlin("test"))
  testImplementation("ch.qos.logback:logback-core:1.4.5")
  testImplementation("ch.qos.logback:logback-classic:1.4.5")
  testImplementation("org.testcontainers:mongodb:1.17.6")
  testImplementation("io.kotest:kotest-runner-junit5:5.5.4")
  testImplementation("io.kotest:kotest-assertions-core:5.5.4")
  testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:1.3.3") {
    exclude("org.apache.kafka", "kafka-clients")
  }
  testImplementation("io.mockk:mockk:1.13.4")
}

tasks.withType<Test> {
  useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "11"
}

sourceSets {
  create("integrationTest") {
    kotlin {
      compileClasspath += main.get().output + configurations.testRuntimeClasspath.get()
      runtimeClasspath += output + compileClasspath
    }
  }
}

val integrationTest = task<Test>("integrationTest") {
  description = "Runs the integration tests"
  group = "verification"
  testClassesDirs = sourceSets["integrationTest"].output.classesDirs
  classpath = sourceSets["integrationTest"].runtimeClasspath
  mustRunAfter(tasks["test"])
}

tasks.check {
  dependsOn(integrationTest)
}

tasks.register<Jar>("sourcesJar") {
  archiveClassifier.set("sources")
  from(sourceSets.main.get().allSource)
}

tasks.register<Jar>("javadocJar") {
  dependsOn("javadoc")
  archiveClassifier.set("javadoc")
  from(tasks.javadoc.get().destinationDir)
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      artifactId = "mongo-index-copy"
      from(components["kotlin"])
      artifact(tasks["sourcesJar"])
      artifact(tasks["javadocJar"])
    }
  }
}
