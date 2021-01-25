package com.intellij.my.file

import com.intellij.openapi.vfs.VirtualFile
import com.unicorn.plugin.virtualFile
import java.io.File

object ConfUniFiles {

  val HOME_DIR: File = File(System.getProperty("user.home") ?: "/")
  val GITHUB_DIR = HOME_DIR.resolve("Desktop/github")//todo move to config with Linux/MacOS
  const val UNI_WINDOW_ID: String = "uni-tool-window"//synchronize with plugin.xml

  val DEFAULT_PATHS: List<File> =
    listOf(
      GITHUB_DIR,
      HOME_DIR,
      File("/tmp")
    )
      .filter { it.exists() }
      .take(3)

  val DEFAULT_NEW_PATH = "/"
  val ROOT_DIRS: List<VirtualFile> = DEFAULT_PATHS.map {
    virtualFile(it.absolutePath)
  }
}
