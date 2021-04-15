plugins {
  kotlin("multiplatform") version KOTLIN_VERSION apply false
  kotlin("plugin.serialization") version KOTLIN_VERSION apply false
  id("ru.tutu.github.token") version ("1.2.0")

//  kotlin("multiplatform") version KOTLIN_VERSION apply false
//  id("kotlin-dce-js") version KOTLIN_VERSION apply false
//  id("kotlinx-serialization") version KOTLIN_VERSION apply false
}

gitHubToken {
  scope = "repo gist workflow read"
  secretAES = "gh_token_aes_secret_1"
  id = "uni-token-id-1"
  storeTokenAtLocalProperties()
}
val GH_TOKEN = gitHubToken.getToken(project)

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
    mavenCentral()
    maven { setUrl("https://dl.bintray.com/kotlin/kotlinx") }
    //maven { setUrl("https://dl.bintray.com/kotlin/exposed") }
    maven { setUrl("https://kotlin.bintray.com/ktor") }
    maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")

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
