plugins {
  kotlin("jvm") version embeddedKotlinVersion
//  kotlin("jvm") version "1.3.70-dev-1093-12"
}

repositories {
//  flatDir {
//    dir("libs")
//  }
  mavenLocal {
    url = uri("$projectDir/repository")
  }
  jcenter()
//  maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
//  maven { setUrl("https://dl.bintray.com/kotlin/kotlin-dev") }
}

dependencies {
  //api("ru.uni:lib:0.0.1")
  api("ru.uni:lib-gradle:0.0.2")

//  api(fileTree("libs") { include("*.jar") }) //groovy: compile fileTree(dir: 'libs', include: ['*.jar'])

//  val libV = "0.1"
//  api(module(":lib-jvm:$libV") {
////    dependency("lib-metadata-0.1")
//  })
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  kotlinOptions.jvmTarget = "1.8"
}
