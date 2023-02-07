import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.8.0"
  `java-library`
  `maven-publish`
  signing
  id("pl.allegro.tech.build.axion-release") version "1.14.3"
}

group = "io.github.pitagoras3"
version = scmVersion.version

scmVersion {
  tag {
    prefix.set(project.rootProject.name + "-")
  }
  versionCreator("versionWithBranch")
}

repositories {
  mavenCentral()
}

dependencies {
  api("org.mongodb:mongodb-driver-sync:4.8.2")

  testImplementation(kotlin("test"))
  testImplementation("ch.qos.logback:logback-core:1.4.5")
  testImplementation("ch.qos.logback:logback-classic:1.4.5")
  testImplementation("org.testcontainers:mongodb:1.17.6")
  testImplementation("io.kotest:kotest-runner-junit5:5.5.5")
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
  repositories {
    maven {
      name = "MavenCentral"
      val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
      val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots"
      url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
      credentials {
        username = System.getenv("SONATYPE_USERNAME")
        password = System.getenv("SONATYPE_PASSWORD")
      }
    }
  }
  publications {
    create<MavenPublication>("mavenJava") {
      artifactId = "mongo-index-copy"
      from(components["kotlin"])
      artifact(tasks["sourcesJar"])
      artifact(tasks["javadocJar"])

      pom {
        name.set("mongo-index-copy")
        description.set("Kotlin/Java library for copying indexes from one MongoDB collection to another.")
        url.set("https://github.com/pitagoras3/mongo-index-copy")
        licenses {
          license {
            name.set("The Apache License, Version 2.0")
            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
          }
        }
        developers {
          developer {
            id.set("pitagoras3")
            name.set("Szymon Marcinkiewicz")
            email.set("szymon.mar1@gmail.com")
          }
        }
        scm {
          connection.set("scm:git:https://github.com/pitagoras3/mongo-index-copy.git")
          developerConnection.set("scm:git:https://github.com/pitagoras3/mongo-index-copy.git")
          url.set("https://github.com/pitagoras3/mongo-index-copy")
        }
      }
    }
  }
}

if (System.getenv("PGP_SIGNING_KEY") != null) {
  signing {
    useInMemoryPgpKeys(System.getenv("PGP_SIGNING_KEY"), System.getenv("PGP_SIGNING_PASSWORD"))
    sign(publishing.publications["mavenJava"])
  }
}
