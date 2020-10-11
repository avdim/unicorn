package ru.tutu.idea.file

import com.intellij.openapi.vfs.VirtualFile
import com.unicorn.plugin.virtualFile
import java.io.File

val HOME_DIR_PATH: String = System.getenv("HOME") ?: "/"

object ConfUniFiles {
  const val UNI_WINDOW_ID: String = "uni-tool-window"//synchronize with plugin.xml

  val DEFAULT_PATHS:List<String> = listOf("$HOME_DIR_PATH/Desktop", "$HOME_DIR_PATH/Desktop/github", HOME_DIR_PATH, "/")
    .filter { File(it).exists() }
    .take(2)

  val DEFAULT_NEW_PATH = "/"
  val ROOT_DIRS: List<VirtualFile> = DEFAULT_PATHS.map {
    virtualFile(it)
  }
}

fun main() {//todo
  val homeDir = System.getenv("HOME")
  println("home: $homeDir")
}
