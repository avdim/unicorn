plugins {
  kotlin("multiplatform") version KOTLIN_VERSION apply false
  kotlin("plugin.serialization") version KOTLIN_VERSION apply false

//  kotlin("multiplatform") version KOTLIN_VERSION apply false
//  id("kotlin-dce-js") version KOTLIN_VERSION apply false
//  id("kotlinx-serialization") version KOTLIN_VERSION apply false
}

buildscript {
  repositories {
    google()
  }
  dependencies {
    classpath("com.android.tools.build:gradle:3.4.2")//todo const
  }
}

allprojects {//todo allprojects bad?
//  buildDir = File("/dev/shm/$name")
//  version = "1.0"
  repositories {
    jcenter()
    mavenCentral()
    maven { setUrl("https://dl.bintray.com/kotlin/kotlinx") }
    //maven { setUrl("https://dl.bintray.com/kotlin/exposed") }
    maven { setUrl("https://kotlin.bintray.com/ktor") }

    if (USE_KOTLIN_DEV_REPOSITORY) {
      maven { setUrl("https://dl.bintray.com/kotlin/kotlin-dev") }
      maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
      maven { setUrl("https://dl.bintray.com/kotlin/kotlinx") }
      maven { setUrl("https://dl.bintray.com/kotlin/kotlin-js-wrappers") }
    }
  }
  tasks.withType(AbstractTestTask::class) {
    testLogging {
      showStandardStreams = true
      events("passed", "failed")
    }
  }
  //todo check difference if use afterEvaluate { tasks... }
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = MIN_JDK_VERSION.kotlinTarget
  }
  if (ConfBuild.TRACE_GRADLE_TASKS) {
    afterEvaluate {
      tasks.all {
        doFirst {
          println("before task ${this.path}")
        }
        doLast {
          println("after task ${this.path}")
        }
      }
    }
  }
}
