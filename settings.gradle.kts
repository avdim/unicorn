pluginManagement {
  if (false/*todo check @Incubating warning*/) {
    val KOTLIN_VERSION = "1.3.72"
    plugins {
      kotlin("plugin.serialization") version KOTLIN_VERSION //apply false
      kotlin("multiplatform") version KOTLIN_VERSION //apply false
    }
  }

  repositories {
//    mavenCentral()
//    maven { setUrl("https://jcenter.bintray.com/") }
    gradlePluginPortal()//todo alternative?: maven { setUrl("https://plugins.gradle.org/m2/") }
    jcenter()
    mavenCentral()
//    maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
//    maven { setUrl("https://dl.bintray.com/kotlin/kotlin-dev") }
    maven { setUrl("https://dl.bintray.com/kotlin/kotlinx") }
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

include("uni-idea-plugin")
include("update-plugin")
include("share-plugin")
include("repo")
include("lib-github")
