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

//  id("com.codingfeline.buildkonfig") version "0.8.0" // https://github.com/yshrsmz/BuildKonfig
  id("com.github.gmazzo.buildconfig") version GMAZZO_BUILDCONFIG_VERSION
  id("org.jetbrains.compose") version DESKTOP_COMPOSE
  idea
}

idea {
  module {
    excludeDirs = excludeDirs + listOf(file("${project.projectDir}/build/libs"))
    excludeDirs = excludeDirs + listOf(file("${project.projectDir}/.exclude"))
  }
}

buildConfig {
  className("BuildConfig")   // forces the class name. Defaults to 'BuildConfig'
  packageName("com.unicorn")  // forces the package. Defaults to '${project.group}'
//  useJavaOutput()                                 // forces the outputType to 'java'
  useKotlinOutput()                               // forces the outputType to 'kotlin', generating an `object`
//  useKotlinOutput { topLevelConstants = true }    // forces the outputType to 'kotlin', generating top-level declarations
//  useKotlinOutput { internalVisibility = true }   // adds `internal` modifier to all declarations
  buildConfigField("long", "BUILD_TIME_LONG", "${System.currentTimeMillis()}L")
  buildConfigField("String", "BUILD_TIME", "\"$BUILD_TIME_STR\"")
  buildConfigField("String", "HAND_TEST_EMPTY_PROJECT", "\"${rootDir.resolve("hand-test-empty-project").absolutePath}\"")
  buildConfigField("boolean", "INTEGRATION_TEST", "${UNI_BUILD_TYPE == BuildType.IntegrationTest}")
  buildConfigField("boolean", "HAND_TEST", "${UNI_BUILD_TYPE == BuildType.HandTest}")
//  buildConfigField("IntArray", "MAGIC_NUMBERS", "intArrayOf(1, 2, 3, 4)")
//  buildConfigField("com.github.gmazzo.SomeData", "MY_DATA", "new SomeData(\"a\",1)")
  sourceSets.getByName("test") {
    buildConfigField("String", "TEST_CONSTANT", "\"aTestValue\"")
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
  maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
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
  implementation(project(":aes"))
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
      version.set(ideaVersion.version)
    }
    is IdeaVersion.Local -> {
      localPath.set(ideaVersion.localPath)
    }
  }
  ideaVersion.type?.let {
    type.set(it)
  }
  sandboxDir.set(myIdeaSandboxDir)

  pluginName.set("unicorn")
  updateSinceUntilBuild.set(false)
  sameSinceUntilBuild.set(true)
  downloadSources.set(true)//todo check debug
  instrumentCode.set(true)//todo if value is false - NPE в KeyPromoter plugin
//    setPlugins("org.jetbrains.kotlin:1.3.11-release-IJ2018.3-1")
  plugins.set(
    listOf(
      "terminal",
      "github",
      "git4idea",
      "android",
//      "org.jetbrains.plugins.ruby:211.7142.36"//https://plugins.jetbrains.com/plugin/1293-ruby/versions/stable
      "org.jetbrains.plugins.ruby:211.7442.9"//https://plugins.jetbrains.com/plugin/1293-ruby/versions/stable
//      "org.jetbrains.plugins.ruby:212.3116.29"//https://plugins.jetbrains.com/plugin/1293-ruby/versions/stable
//      "org.jetbrains.plugins.ruby:212.4037.9"//https://plugins.jetbrains.com/plugin/1293-ruby/versions/stable
//      "org.jetbrains.plugins.ruby:212.4321.30"//https://plugins.jetbrains.com/plugin/1293-ruby/versions/stable
//      "io.intellij-sdk-thread-access:1.0.2"//https://plugins.jetbrains.com/plugin/16815-thread-access-info/
//    "Kotlin", "java"
      /*, "org.jetbrains.kotlin:1.3.72-release-IJ2020.1-1"*/
    )
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
//  jbrVersion("jbr_dcevm-11_0_10-linux-x64-b1341.35")
  systemProperties["ide.browser.jcef.enabled"] = true
//  systemProperties["pdf.viewer.debug"] = true
  jvmArgs("-Xmx2548m", "-Xms128m")
  autoReloadPlugins.set(true)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions.jvmTarget = "1.8"
  kotlinOptions.freeCompilerArgs += listOf("-Xjvm-default=enable")
}

tasks {
  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }
}

//plugins {
//    idea
//}
//configure<org.gradle.plugins.ide.idea.model.IdeaModel> {
//    module.excludeDirs = module.excludeDirs + listOf(file("build/idea-sandbox"))
//}
