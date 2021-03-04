buildscript {//todo workaround https://github.com/JetBrains/gradle-intellij-plugin/issues/537
  repositories {
    maven("https://jetbrains.bintray.com/intellij-plugin-service")
  }
}

group = "com.domain.plugin"
version = UNI_VERSION

plugins {
  java
  kotlin("jvm")
  id("org.jetbrains.intellij") version INTELLIJ_GRADLE
  id("com.github.kukuhyoniatmoko.buildconfigkotlin") version "1.0.5"
  id("org.jetbrains.compose") version DESKTOP_COMPOSE
  idea
}

idea {
  module {
    excludeDirs = excludeDirs + listOf(file("${project.projectDir}/build/libs"))
  }
}

buildConfigKotlin {
  sourceSet("main") {
    packageName = "com.unicorn"
    buildConfig(name = "BUILD_TIME", value = BUILD_TIME_STR)
    buildConfig(name = "INTEGRATION_TEST", value = UNI_BUILD_TYPE == BuildType.IntegrationTest)
  }
}

java {
  if (true) {
    toolchain {
      languageVersion.set(JavaLanguageVersion.of(11))
      if (false) {
        vendor.set(JvmVendorSpec.BELLSOFT)
        implementation.set(JvmImplementation.VENDOR_SPECIFIC)
      }
    }
  } else {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

repositories {
  mavenCentral()
  maven("https://www.jetbrains.com/intellij-repository/snapshots")
  maven("https://jetbrains.bintray.com/intellij-plugin-service")
  maven("https://jetbrains.bintray.com/intellij-third-party-dependencies")
//  maven { setUrl("https://oss.sonatype.org/content/repositories/snapshots/") }
//  maven { setUrl("https://dl.bintray.com/jetbrains/intellij-plugin-service") }
}

dependencies {
  implementation(LOG_MAVEN_ARTIFACT)
  implementation("org.jgroups:jgroups:4.0.0.Final")//todo
  implementation("io.ktor:ktor-server-netty:$KTOR_VERSION")
  implementation("io.ktor:ktor-server-cio:$KTOR_VERSION")
  implementation("io.ktor:ktor-client-core:$KTOR_VERSION")
  implementation("io.ktor:ktor-client-apache:$KTOR_VERSION")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$COROUTINE_VERSION")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$COROUTINE_VERSION")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:$COROUTINE_VERSION")
  implementation(project(":share-plugin"))
  implementation(project(":repo"))
  implementation(compose.desktop.currentOs)
//  implementation(compose.desktop.all)//todo linux
  testImplementation("junit:junit:4.12")
}

intellij {
  // See https://github.com/JetBrains/gradle-intellij-plugin/

  val selfIdea="/home/dim/Desktop/github/JetBrains/intellij-community/out/deploy/dist"
  val idea2020_2 = "/home/dim/Desktop/programs/idea2020.2/2020.2-beta/idea-IC-202.6250.13"
  val as4_0 = "/home/dim/Desktop/programs/android-studio-4.0"
//  alternativeIdePath = as4_2

  val ideaVersion = IDEA_VERSION
  when (ideaVersion) {
    is IdeaVersion.Download -> {
      version = ideaVersion.version
    }
    is IdeaVersion.Local -> {
      localPath = ideaVersion.localPath
    }
  }
  ideaVersion.type?.let {
    type = it
  }
  sandboxDirectory = myIdeaSandboxDir

  pluginName = "unicorn"
  updateSinceUntilBuild = false
  sameSinceUntilBuild = true
  downloadSources = true//todo check debug
  instrumentCode = true//todo if value is false - NPE в KeyPromoter plugin
//    setPlugins("org.jetbrains.kotlin:1.3.11-release-IJ2018.3-1")
  setPlugins(
    "terminal",
    "github",
    "git4idea"
//    "Kotlin", "java"
    /*, "org.jetbrains.kotlin:1.3.72-release-IJ2020.1-1"*/
  )
}

tasks.withType<org.jetbrains.intellij.tasks.PatchPluginXmlTask>() {
//  sinceBuild("202")
//  untilBuild("299.*")
//  changeNotes(closure { changelog.getLatest().withHeader(false).toHTML() })
  pluginDescription
}

tasks.withType<org.jetbrains.intellij.tasks.RunIdeTask> {
  //MacOS:
//  jbrVersion("jbsdk8u202b1491") //"jbsdk8u202b1491"Win, "8u232b1638.6", "jbrex8u152b1024.10"
  // "jbr_jcef-11_0_6b840.3" for webview
  // jbsdk8u202b1491_osx_x64.tar.gz
//  jbrVersion("jbrsdk-11_0_9-osx-x64-b944.45")//with jcef

  //Linux:
  //https://bintray.com/jetbrains/intellij-jbr/jbrsdk11-linux-x64
  //https://bintray.com/jetbrains/intellij-jbr/download_file?file_path=jbrsdk-11_0_7-linux-x64-b989.1.tar.gz
  // https://cache-redirector.jetbrains.com/jetbrains.bintray.com/intellij-jbr/jbrsdk-11_0_7-linux-x64-b989.1.tar.gz
//  jbrVersion("jbrsdk-11_0_7b989.1")
  systemProperties["ide.browser.jcef.enabled"] = true
//  systemProperties["pdf.viewer.debug"] = true
  jvmArgs("-Xmx2048m", "-Xms128m")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions.jvmTarget = "1.8"
  kotlinOptions.freeCompilerArgs += listOf("-Xjvm-default=enable")
}

tasks {
  publishPlugin {
    token(System.getenv("PUBLISH_TOKEN"))
  }
}

//plugins {
//    idea
//}
//configure<org.gradle.plugins.ide.idea.model.IdeaModel> {
//    module.excludeDirs = module.excludeDirs + listOf(file("build/idea-sandbox"))
//}
