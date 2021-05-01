pluginManagement {
  if (false/*todo check @Incubating warning*/) {
    val KOTLIN_VERSION = "1.3.72"
    plugins {
      kotlin("plugin.serialization") version KOTLIN_VERSION //apply false
      kotlin("multiplatform") version KOTLIN_VERSION //apply false
    }
  }

  repositories {
    gradlePluginPortal()//todo alternative?: maven { setUrl("https://plugins.gradle.org/m2/") }
    mavenCentral()
    maven { setUrl("https://dl.bintray.com/kotlin/kotlinx") }
    maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
    maven { setUrl("https://dl.bintray.com/kotlin/kotlin-dev") }
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/") // plugin id("org.jetbrains.intellij") SNAPSHOT
  }

//  resolutionStrategy {
//    eachPlugin {
//      when (requested.id.id) {
////        "kotlin-dce-js" -> useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
////        "kotlinx-serialization" -> useModule("org.jetbrains.kotlin:kotlin-serialization:${requested.version}")
////        "org.jetbrains.kotlin.multiplatform" -> useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${target.version}")
//      }
//    }
//  }
}
rootProject.name = "unicorn"
enableFeaturePreview("GRADLE_METADATA")

include("uni-idea-plugin")
include("update-plugin")
include("share-plugin")
include("repo")
include("lib-github")
include("aes")
include("app-github-workflow")
