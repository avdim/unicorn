import org.gradle.api.Project
import java.text.SimpleDateFormat
import java.util.*

val BUILD_TIME_STR = SimpleDateFormat("yyyy-MM-dd_HH:mm", Locale("ru", "RU")).format(Date())
//val BUILD_TIME_STR = Date().toString()

/**
 * Отладочная версия JVM схожа с JS. Можно выставлять не зависимо от JS.
 * Но если игровая логика содержит проверки IS_DEBUG, или DEBUG{...}, то возможна рассинхронизация стейта итгры.
 */
val DEBUG_JVM = true//todo false

/**
 * Минимальная версия JDK. В production использую 11. А для целей отладки подойдёт и 8-ая.
 */
val MIN_JDK_VERSION: JdkVersion = if (true/*DEBUG_JVM*/) JdkVersion.JDK8 else JdkVersion.JDK11

/**
 * Если хочется потестировать EAP или DEV версии kotlin.
 * Dev релизы на свой страх и риск: https://dl.bintray.com/kotlin/kotlin-dev/org/jetbrains/kotlin/kotlin-gradle-plugin/
 */
val USE_KOTLIN_DEV_REPOSITORY = true

val KOTLIN_VERSION = "1.4.20"
val SERIALIZATION_VERSION = "1.0.1"
val COROUTINE_VERSION = "1.4.1"
val KTOR_VERSION = "1.4.1"
val LOG_MAVEN_ARTIFACT = if (DEBUG_JVM) "ch.qos.logback:logback-classic:1.2.3" else "org.slf4j:slf4j-simple:1.7.28"

//https://github.com/Kotlin/kotlinx.coroutines/commit/e37aec4edd09bfb7f622e113553aa88a0a5bd27c
val COMPILER_ARGS = listOf<String>()
//val COMPILER_ARGS = listOf<String>("-Xir-produce-js", "-Xgenerate-dts", "-XXLanguage:+NewInference")
//tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
//        kotlinOptions {
//            freeCompilerArgs += COMPILER_ARGS
//or in gradle.properties: kotlin.js.compiler=ir

// https://github.com/JetBrains/gradle-intellij-plugin
val INTELLIJ_GRADLE = "0.6.5"
// https://maven.pkg.jetbrains.space/public/p/compose/dev/org/jetbrains/compose/org.jetbrains.compose.gradle.plugin/
//val DESKTOP_COMPOSE = "0.2.0-build132"
val DESKTOP_COMPOSE = "0.3.0-build133"

val Project.UNI_BUILD_TYPE: BuildType get() =
  when (safeArgument("uniBuildType")) {
    "release" -> BuildType.Release
    "integration-test" -> BuildType.IntegrationTest
    else -> BuildType.Debug
  }

val UNI_VERSION = "0.12.5"

// https://www.jetbrains.com/intellij-repository/snapshots/
//    IdeaVersion.Community("2020.1.2")
//    IdeaVersion.Community("2020.2.3")
// val LAST_COMMUNITY = IdeaVersion.Download("203.5784.10-EAP-SNAPSHOT")
val LAST_COMMUNITY = IdeaVersion.Download("2020.3", "IC")

val Project.IDEA_VERSION: IdeaVersion get() =
  when (UNI_BUILD_TYPE) {
    is BuildType.Release, BuildType.Debug -> {
//      IdeaVersion.Download("2020.3", "IU")
      LAST_COMMUNITY
      //IdeaVersion.Community("LATEST-EAP-SNAPSHOT")
    }
    is BuildType.IntegrationTest -> {
      IdeaVersion.Download("203.4818-EAP-CANDIDATE-SNAPSHOT")//jvm8 stable integration test
//      IdeaVersion.Community("203.5251-EAP-CANDIDATE-SNAPSHOT")//jvm11
//      IdeaVersion.Community("203.5600.34-EAP-SNAPSHOT")//jvm11
//      IdeaVersion.Community("203.5784.10-EAP-SNAPSHOT")
//      LAST_COMMUNITY
    }
    is BuildType.UseLocal -> {
      if(isMacOS) {
//        IdeaVersion.Local("/Users/dim/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-1/202.6863838/Android Studio 4.2 Preview.app/Contents")
        IdeaVersion.Local("/Users/dim/Library/Application Support/JetBrains/Toolbox/apps/IDEA-U/ch-2/203.5784.10/IntelliJ IDEA 2020.3 EAP.app/Contents")
      } else {
        LAST_COMMUNITY
      }
    }
  }

fun Project.safeArgument(key: String): String? =
  if (hasProperty(key)) {
    property(key) as? String
  } else {
    null
  }

val isMacOS get() = System.getProperty("os.name")?.contains("Mac OS") ?: false
