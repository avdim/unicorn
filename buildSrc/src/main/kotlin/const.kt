import org.gradle.api.Project
import java.text.SimpleDateFormat
import java.util.*

val UNI_VERSION = "0.17.0"
val BUILD_TIME_STR = SimpleDateFormat("yyyy-MM-dd_HH:mm", Locale("ru", "RU")).format(Date())
val DEBUG_JVM = true
val MIN_JDK_VERSION: JdkVersion = JdkVersion.JDK17
val JVM_TARGET = MIN_JDK_VERSION.kotlinTarget

val KOTLIN_VERSION = "1.9.10"
val COMPOSE_VERSION = "1.5.10-beta01"
val COMPOSE_WORKAROUND = false //todo delete
val SERIALIZATION_VERSION = "1.4.1"
val COROUTINE_VERSION = "1.6.4"
val KTOR_VERSION = "2.3.4"
val LOG_MAVEN_ARTIFACT = if (DEBUG_JVM) "ch.qos.logback:logback-classic:1.2.3" else "org.slf4j:slf4j-simple:1.7.28"
val GMAZZO_BUILDCONFIG_VERSION = "3.0.1"

//https://github.com/Kotlin/kotlinx.coroutines/commit/e37aec4edd09bfb7f622e113553aa88a0a5bd27c
val COMPILER_ARGS = listOf<String>()
//val COMPILER_ARGS = listOf<String>("-Xir-produce-js", "-Xgenerate-dts", "-XXLanguage:+NewInference")
//tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
//        kotlinOptions {
//            freeCompilerArgs += COMPILER_ARGS
//or in gradle.properties: kotlin.js.compiler=ir

// https://www.jetbrains.com/intellij-repository/snapshots/
// https://www.jetbrains.com/intellij-repository/releases/
//val LAST_IDEA_STR = "213.5744.223"
//val LAST_IDEA_STR = "2021.3.2"
//val LAST_IDEA_STR = "221.4501-EAP-CANDIDATE-SNAPSHOT"
//val LAST_IDEA_STR = "2022.1"
//val LAST_IDEA_STR = "2022.2.3"
//val LAST_IDEA_STR = "223.7401-EAP-CANDIDATE-SNAPSHOT"
//val LAST_IDEA_STR = "232.5150-EAP-CANDIDATE-SNAPSHOT"
//val LAST_IDEA_STR = "2023.1.2"
//val LAST_IDEA_STR = "232.7754-EAP-CANDIDATE-SNAPSHOT"
val LAST_IDEA_STR = "2023.2.1"
//val LAST_IDEA_STR = "2023.2.2"
//val LAST_IDEA_STR = "2023.2.3"
//val LAST_IDEA_STR = "233.6745-EAP-CANDIDATE-SNAPSHOT"
//val LAST_IDEA_STR = "233.9802-EAP-CANDIDATE-SNAPSHOT"

val ideaCommunityMac = "/Users/dim/Library/Application Support/JetBrains/Toolbox/apps/IDEA-C/ch-1/221.5080.210/IntelliJ IDEA CE.app/Contents"

val LAST_COMMUNITY = IdeaVersion.Download(LAST_IDEA_STR, "IC")
//val LAST_COMMUNITY = IdeaVersion.Local(ideaCommunityMac, null)
val LAST_ULTIMATE = IdeaVersion.Download(LAST_IDEA_STR, "IU")

// https://github.com/JetBrains/gradle-intellij-plugin
//val INTELLIJ_GRADLE = "1.4.0"
//val INTELLIJ_GRADLE = "1.5.2"
//val INTELLIJ_GRADLE = "1.5.3"
//val INTELLIJ_GRADLE = "1.6.0"
//val INTELLIJ_GRADLE = "1.8.0"
//val INTELLIJ_GRADLE = "1.9.0"
val INTELLIJ_GRADLE = "1.14.1"

val home = System.getProperty("user.home")
//val asMac = "/Users/dim/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-0/203.7185775/Android Studio Preview.app/Contents"
//val asMac = "/Users/dim/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-0/203.7360992/Android Studio Preview.app/Contents"
//val asMac = "/Users/dim/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-1/203.7717.56.2111.7361063/Android Studio Preview.app/Contents"
//val asMac = "/Users/dim/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-0/212.4037.9.2112.7818732/Android Studio Preview.app/Contents"
//val asMac = "/Users/dim/Desktop/android-studio/2021.2/android-studio-2021.2.1.1-mac-canary1/Contents"
//val asMac = "/Users/dim/Desktop/android-studio/2021.2/2021.2-canary5/Android Studio Preview.app/Contents"
//val asMac = "/Users/dim/Desktop/android-studio/2021.2/2021.2-canary6/unzipped/Contents"
//val asMac = "$home/Desktop/android-studio/2021.2.1_beta2/Android Studio Preview.app/Contents"
//val asMac = "$home/Desktop/android-studio/2021.1.1-canary8/Android Studio Preview.app/Contents"
val asMac = "$home/Desktop/android-studio/2021.3.1.7-canary/Android Studio Preview.app/Contents"
//val asLinux = "/home/dim/Desktop/android_studio/2021.2_canary7/android-studio/"
val asLinux = "/home/dim/Desktop/android_studio/2021.2.1_beta2/android-studio/"
//val asLinux = "/home/dim/Desktop/android_studio/2021.3.1_canary2/android-studio/"

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

val Project.myIdeaDependencyCachePath: String
  get() =
    rootProject.projectDir.resolve(".exclude").resolve("my_idea_dependency_cache_path").absolutePath

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
      IdeaVersion.Local(buildType.path, null /*LAST_IDEA_STR*/)
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
