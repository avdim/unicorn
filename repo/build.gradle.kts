plugins {
  java//delete?
  kotlin("jvm")
  application
  id("com.github.johnrengelman.shadow") version "6.0.0"
  kotlin("plugin.serialization")
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

application {
  mainClassName = "org.sample.StarterKt"
  applicationName = "repo"
}

tasks.getByName<JavaExec>("run").workingDir = rootProject.projectDir
tasks.withType<Test> {
  testLogging {
    showStandardStreams = true
  }
}

group = "org.sample"
version = "1.0-SNAPSHOT"

repositories {
  jcenter()
  mavenCentral()
}

dependencies {
  implementation(if (true) "ch.qos.logback:logback-classic:1.2.3" else "org.slf4j:slf4j-simple:1.7.28")
  implementation("org.eclipse.jgit:org.eclipse.jgit:4.11.0.201803080745-r")
  implementation("com.jcraft:jsch.agentproxy.jsch:0.0.9")
  implementation("com.jcraft:jsch.agentproxy.usocket-jna:0.0.9")
  implementation("com.jcraft:jsch.agentproxy.sshagent:0.0.9")
//  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$COROUTINES_VERSION")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$SERIALIZATION_VERSION")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$SERIALIZATION_VERSION")

  testImplementation("org.jetbrains.kotlin:kotlin-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
//  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$COROUTINES_VERSION")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions.jvmTarget = "1.8"
}
