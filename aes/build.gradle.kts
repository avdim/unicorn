plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
}

group = "ru.avdim.lib.aes"
version = "0.1"

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(kotlin("stdlib"))
  testImplementation("org.jetbrains.kotlin:kotlin-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions.jvmTarget = JVM_TARGET
//  kotlinOptions.freeCompilerArgs += listOf("-Xjvm-default=enable")
}
