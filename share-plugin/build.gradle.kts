//buildscript {//todo workaround https://github.com/JetBrains/gradle-intellij-plugin/issues/537
//  repositories {
//    maven("https://jetbrains.bintray.com/intellij-plugin-service")
//  }
//}

plugins {
  java
  kotlin("jvm")
  id("org.jetbrains.intellij") version INTELLIJ_GRADLE
  idea
}

idea {
  module {
    excludeDirs = excludeDirs + listOf(file("${project.projectDir}/build/libs"))
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

group = "ru.avdim.idea-plugin-update"
version = "0.1"

repositories {
  mavenCentral()
  maven("https://www.jetbrains.com/intellij-repository/snapshots")
//  maven("https://jetbrains.bintray.com/intellij-plugin-service")
  maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
//  maven { setUrl("https://oss.sonatype.org/content/repositories/snapshots/") }
//  maven { setUrl("https://dl.bintray.com/jetbrains/intellij-plugin-service") }
}

dependencies {
  implementation(LOG_MAVEN_ARTIFACT)
  implementation("io.ktor:ktor-server-netty:$KTOR_VERSION")
  implementation("io.ktor:ktor-server-cio:$KTOR_VERSION")
  implementation("io.ktor:ktor-client-core:$KTOR_VERSION")
  implementation("io.ktor:ktor-client-apache:$KTOR_VERSION")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$COROUTINE_VERSION")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:$COROUTINE_VERSION")
  testImplementation("junit:junit:4.12")
}

intellij {
  val ideaVersion = IDEA_VERSION
  when(ideaVersion) {
    is IdeaVersion.Download -> {
      version.set(ideaVersion.version)
    }
    is IdeaVersion.Local -> {
      ideaVersion.version?.let {
        version.set(it)
      }
      localPath.set(ideaVersion.localPath)
    }
  }
  ideaVersion.type?.let {
    type.set(it)
  }
  sandboxDir.set(myIdeaSandboxDir)

  pluginName.set("unicorn-update")
  updateSinceUntilBuild.set(false)
  sameSinceUntilBuild.set(true)
  downloadSources.set(true)
  instrumentCode.set(true)
  ideaDependencyCachePath.set(myIdeaDependencyCachePath)
//    setPlugins("org.jetbrains.kotlin:1.3.11-release-IJ2018.3-1")
}

tasks.withType<org.jetbrains.intellij.tasks.PatchPluginXmlTask>() {
//  sinceBuild("202")
//  untilBuild("299.*")
//  changeNotes(closure { changelog.getLatest().withHeader(false).toHTML() })
  pluginDescription
}

tasks.withType<org.jetbrains.intellij.tasks.RunIdeTask> {
  jvmArgs("-Xmx2000m", "-Xms128m")
  autoReloadPlugins.set(true)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions.jvmTarget = JVM_TARGET
  kotlinOptions.freeCompilerArgs += listOf("-Xjvm-default=enable")
}

tasks {
  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }
}
