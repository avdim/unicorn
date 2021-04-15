plugins {
  java//delete?
  kotlin("jvm")
  application
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

application {
  mainClass.set("org.sample.github.AppGithubWorkflowStarterKt")
//  mainClassName = "org.sample.github.AppGithubWorkflowStarterKt"
  applicationName = "app-github-workflows"
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
  mavenCentral()
}

dependencies {

  testImplementation("org.jetbrains.kotlin:kotlin-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
//  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$COROUTINES_VERSION")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions.jvmTarget = "1.8"
}
