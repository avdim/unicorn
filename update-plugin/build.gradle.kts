//buildscript {//todo workaround https://github.com/JetBrains/gradle-intellij-plugin/issues/537
//  repositories {
//    maven("https://jetbrains.bintray.com/intellij-plugin-service")
//  }
//}

plugins {
  java
  kotlin("jvm")
  id("org.jetbrains.intellij") version INTELLIJ_GRADLE
  id("com.github.gmazzo.buildconfig") version GMAZZO_BUILDCONFIG_VERSION
  idea
}

idea {
  module {
    excludeDirs = excludeDirs + listOf(file("${project.projectDir}/build/libs"))
  }
}

buildConfig {
  className("BuildConfig")   // forces the class name. Defaults to 'BuildConfig'
  packageName("com.unicorn.update")  // forces the package. Defaults to '${project.group}'
  useKotlinOutput()                               // forces the outputType to 'kotlin', generating an `object`
  buildConfigField("String", "UNI_ZIP_BUILD_DIST", "\"${rootProject.file("uni-idea-plugin/build/distributions").absolutePath}\"")
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

group = "ru.avdim.idea-plugin-update"
version = "0.1"

dependencies {
  implementation(LOG_MAVEN_ARTIFACT)
  implementation("io.ktor:ktor-server-netty:$KTOR_VERSION")
  implementation("io.ktor:ktor-server-cio:$KTOR_VERSION")
  implementation("io.ktor:ktor-client-core:$KTOR_VERSION")
  implementation("io.ktor:ktor-client-apache:$KTOR_VERSION")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$COROUTINE_VERSION")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:$COROUTINE_VERSION")
  implementation(project(":share-plugin"))
  implementation(project(":lib-github"))
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
//    plugins.set(listOf("org.jetbrains.kotlin:1.3.11-release-IJ2018.3-1"))
}

tasks.withType<org.jetbrains.intellij.tasks.PatchPluginXmlTask>() {
//  sinceBuild("202")
//  untilBuild("299.*")
//  changeNotes(closure { changelog.getLatest().withHeader(false).toHTML() })
  pluginDescription
}

tasks.withType<org.jetbrains.intellij.tasks.RunIdeTask> {
  systemProperties["ide.browser.jcef.enabled"] = true
  jvmArgs("-Xmx2000m", "-Xms128m")
  autoReloadPlugins.set(true)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions.jvmTarget = JVM_TARGET
  kotlinOptions.freeCompilerArgs += listOf("-Xjvm-default=enable")
}

tasks {
  named("runIde") {
    dependsOn(":uni-idea-plugin:buildPlugin")
  }
}
