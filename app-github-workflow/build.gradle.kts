plugins {
  kotlin("jvm")
  application
  id("ru.tutu.github.token") version ("1.2.0")
  id("com.github.gmazzo.buildconfig") version GMAZZO_BUILDCONFIG_VERSION
}

gitHubToken {
  scope = "repo gist workflow read user:email"
  secretAES = "gh_token_aes_secret_1"
  id = "uni-token-id-1"
  storeTokenAtLocalProperties()
}

buildConfig {
  className("BuildConfig")   // forces the class name. Defaults to 'BuildConfig'
  packageName("org.sample.github")  // forces the package. Defaults to '${project.group}'
  useKotlinOutput()                               // forces the outputType to 'kotlin', generating an `object`
  buildConfigField("String", "SECRET_GITHUB_TOKEN", "\"${gitHubToken.getToken(project)}\"")
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

application {
  mainClass.set("org.sample.github.AppGithubWorkflowStarterKt")
//  mainClassName = "org.sample.github.AppGithubWorkflowStarterKt"
  applicationName = "app-github-workflows"
}

tasks.getByName<JavaExec>("run").workingDir = rootProject.projectDir
tasks.withType<Test> {
  testLogging {
    showStandardStreams = true
  }
}

group = "org.sample"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
  maven { setUrl("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") }
}

dependencies {
  implementation(project(":lib-github"))
  implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")
  implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$COROUTINE_VERSION")
  implementation("io.ktor:ktor-client-core:$KTOR_VERSION")
  implementation("io.ktor:ktor-client-apache:$KTOR_VERSION")
  testImplementation("org.jetbrains.kotlin:kotlin-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
//  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$COROUTINES_VERSION")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions.jvmTarget = "1.8"
}
