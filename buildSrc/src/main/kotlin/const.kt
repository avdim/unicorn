import org.gradle.api.Project
import java.text.SimpleDateFormat
import java.util.*

val UNI_VERSION = "0.13.0"
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
val MIN_JDK_VERSION: JdkVersion = if (false/*DEBUG_JVM*/) JdkVersion.JDK8 else JdkVersion.JDK11

/**
 * Если хочется потестировать EAP или DEV версии kotlin.
 * Dev релизы на свой страх и риск: https://dl.bintray.com/kotlin/kotlin-dev/org/jetbrains/kotlin/kotlin-gradle-plugin/
 */
val USE_KOTLIN_DEV_REPOSITORY = true

//val KOTLIN_VERSION = "1.4.21-2"
val KOTLIN_VERSION = "1.4.32"
val SERIALIZATION_VERSION = "1.0.1"
val COROUTINE_VERSION = "1.4.2"
val KTOR_VERSION = "1.5.0"
val LOG_MAVEN_ARTIFACT = if (DEBUG_JVM) "ch.qos.logback:logback-classic:1.2.3" else "org.slf4j:slf4j-simple:1.7.28"

//https://github.com/Kotlin/kotlinx.coroutines/commit/e37aec4edd09bfb7f622e113553aa88a0a5bd27c
val COMPILER_ARGS = listOf<String>()
//val COMPILER_ARGS = listOf<String>("-Xir-produce-js", "-Xgenerate-dts", "-XXLanguage:+NewInference")
//tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
//        kotlinOptions {
//            freeCompilerArgs += COMPILER_ARGS
//or in gradle.properties: kotlin.js.compiler=ir

// https://www.jetbrains.com/intellij-repository/snapshots/
//val LAST_IDEA_STR = "2020.3.3"
//val LAST_IDEA_STR = "2021.1"
//val LAST_IDEA_STR = "211.7142.13-EAP-SNAPSHOT"
//val LAST_IDEA_STR = "2021.1.1"
//val LAST_IDEA_STR = "211.7442.9-EAP-SNAPSHOT"
val LAST_IDEA_STR = "211.7442-EAP-CANDIDATE-SNAPSHOT"
//val LAST_IDEA_STR = "2021.1.2"
//val LAST_IDEA_STR = "212.3116.29-EAP-SNAPSHOT"
//val LAST_IDEA_STR = "212.3724-EAP-CANDIDATE-SNAPSHOT"

val LAST_COMMUNITY = IdeaVersion.Download(LAST_IDEA_STR, "IC")
val LAST_ULTIMATE = IdeaVersion.Download(LAST_IDEA_STR, "IU")

// https://github.com/JetBrains/gradle-intellij-plugin
//val INTELLIJ_GRADLE = "0.7.2"
//val INTELLIJ_GRADLE = "0.7.3"
val INTELLIJ_GRADLE = "1.0"
// https://maven.pkg.jetbrains.space/public/p/compose/dev/org/jetbrains/compose/org.jetbrains.compose.gradle.plugin/
//val DESKTOP_COMPOSE = "0.3.0-build152"
//val DESKTOP_COMPOSE = "0.3.2"
//val DESKTOP_COMPOSE = "0.4.0-build183"
val DESKTOP_COMPOSE = "0.4.0-build185"
//val DESKTOP_COMPOSE = "0.4.0"
val COMPOSE_WORKAROUND = true

//val asMac = "/Users/dim/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-0/203.7185775/Android Studio Preview.app/Contents"
//val asMac = "/Users/dim/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-0/203.7360992/Android Studio Preview.app/Contents"
val asMac = "/Users/dim/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-1/203.7717.56.2111.7361063/Android Studio Preview.app/Contents"
//val asLinux = "/home/dim/Desktop/programs/android-studio-4.2/2020.3.1.8"
//val asLinux = "/home/dim/Desktop/android_studio/2020.3.1.1_canary10/"
//val asLinux = "/home/dim/Desktop/android_studio/2020.3_alpha12/extracted"
//val asLinux = "/home/dim/Desktop/android_studio/2020.3_beta1/android-studio"
val asLinux = "/home/dim/Desktop/android_studio/2021_alpha1/android-studio"

val Project.UNI_BUILD_TYPE: BuildType get() =
  when (safeArgument("uniBuildType")) {
    "release" -> BuildType.Release
    "as" -> if (isMacOS) {
      BuildType.UseLocal(asMac)
    } else {
      BuildType.UseLocal(asLinux)
    }
    "integration-test" -> BuildType.IntegrationTest
    "hand-test" -> BuildType.HandTest
    else -> BuildType.Debug
  }

val Project.myIdeaSandboxDir: String
  get() = UNI_BUILD_TYPE.let { buildType ->
    when (buildType) {
      BuildType.Release, BuildType.Debug, BuildType.HandTest -> {
        //HOME_DIR.resolve("Desktop/uni_release_system").absolutePath
        //tmpDir()//"/tmp/idea_sandbox"
        val file = projectDir.resolve(".exclude/.idea_system_${IDEA_VERSION.postfixName}")
        file.mkdirs()
        file.absolutePath
      }
      is BuildType.UseLocal -> {
        val file = projectDir.resolve(".exclude/.idea_system_local_${buildType.path.hashCode()}")
        file.mkdirs()
        file.absolutePath
      }
      BuildType.IntegrationTest -> tmpDir()
    }
  }

val Project.IDEA_VERSION: IdeaVersion get() = UNI_BUILD_TYPE.let {buildType->
  when (buildType) {
    is BuildType.Debug, BuildType.HandTest -> {
      LAST_COMMUNITY
    }
    is BuildType.Release -> {
      LAST_ULTIMATE
    }
    is BuildType.IntegrationTest -> {
      LAST_COMMUNITY
    }
    is BuildType.UseLocal -> {
      IdeaVersion.Local(buildType.path)
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
