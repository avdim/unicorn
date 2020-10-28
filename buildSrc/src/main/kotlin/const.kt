import java.text.SimpleDateFormat
import java.util.*

val BUILD_TIME_STR = SimpleDateFormat("yyyy-MM-dd_HH:mm", Locale("ru", "RU")).format(Date())
//val BUILD_TIME_STR = Date().toString()

/**
 * Отладочная версия содержит расширенное логирование (tool-log), меряет скорость исполнения разных кусков кода (tool-measure).
 * Показывает dashboard в браузере.
 */
val DEBUG_JS = true//todo false

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
 * Формат websocket для клиента. Для debug удобно использовать Json. Для production бинарный полегче.
 * Серверу сразу работает со всеми возможными вариантами, и ему формат указывать не нужно.
 */
val CLIENT_WEB_SOCKET: WebSocketType = WebSocketType.JSON

/**
 * Предполагается что эта опция будет включать отладочный режим для JS через webpack, но я пока это не сделал...
 * В идеале если собирать debug версию, то можно подсоединяться отладчиком и расставлять breakpoint-ы в Kotlin коде.
 * Будет собираться source-map JS-бандла к Kotlin файлам.
 *
 * Поначалу я пользовался отладчиком, но когда прокачал систему логов, и научился запускать JS тесты, - то необходимость отпала.
 * Тем более что общий код удобно дебажить под JVM.
 * А ещё в коде всегда можно расставить js("debugger;") и браузер остановится в этом месте.
 */
val DEBUG_WEBPACK_TODO = false

/**
 * Если хочется потестировать EAP или DEV версии kotlin.
 * Dev релизы на свой страх и риск: https://dl.bintray.com/kotlin/kotlin-dev/org/jetbrains/kotlin/kotlin-gradle-plugin/
 */
val USE_KOTLIN_DEV_REPOSITORY = true

// Версии библиотек:
//val KOTLIN_VERSION = "1.3.72"
val KOTLIN_VERSION = "1.4.10"
val SERIALIZATION_VERSION = "1.0.0"
//val COROUTINE_VERSION = "1.3.3"
val COROUTINE_VERSION = "1.3.9"
//val KTOR_VERSION = "1.2.6"
val KTOR_VERSION = "1.4.0"
val LOG_MAVEN_ARTIFACT = if (DEBUG_JVM) "ch.qos.logback:logback-classic:1.2.3" else "org.slf4j:slf4j-simple:1.7.28"
val LANGUAGE_FEATURES = listOf("InlineClasses")

//https://github.com/Kotlin/kotlinx.coroutines/commit/e37aec4edd09bfb7f622e113553aa88a0a5bd27c
val COMPILER_ARGS = listOf<String>()
//val COMPILER_ARGS = listOf<String>("-Xir-produce-js", "-Xgenerate-dts", "-XXLanguage:+NewInference")
//tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
//        kotlinOptions {
//            freeCompilerArgs += COMPILER_ARGS
//or in gradle.properties: kotlin.js.compiler=ir

val USE_ANDROID = false

/**
 * https://github.com/JetBrains/gradle-intellij-plugin
 */
val INTELLIJ_GRADLE = "0.5.1"

//https://www.jetbrains.com/intellij-repository/snapshots/
//val IDEA_VERSION = "2020.1.2"
//val IDEA_VERSION = "2020.2.3"
val IDEA_VERSION = "203.4818-EAP-CANDIDATE-SNAPSHOT"//2020.3-eap3
//val IDEA_VERSION = "203.5251-EAP-CANDIDATE-SNAPSHOT"//2020.3-eap4
//val IDEA_VERSION = "203.5419-EAP-CANDIDATE-SNAPSHOT"//2020.3-eap5
//val IDEA_VERSION = "LATEST-EAP-SNAPSHOT"
