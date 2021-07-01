plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
}

group = "ru.avdim.lib.aes"
version = "0.1"

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

repositories {
  mavenCentral()
}

dependencies {
  testImplementation("org.jetbrains.kotlin:kotlin-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions.jvmTarget = JVM_TARGET
  kotlinOptions.freeCompilerArgs += listOf("-Xjvm-default=enable")
}
