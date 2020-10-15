package com.unicorn.plugin.mvi

import com.unicorn.Uni

data class UniWindowState(
  val columns: List<Column>,
  val renderFiles: Boolean = !Uni.DYNAMIC_UNLOAD || Uni.buildConfig.OPEN_FILE_MANAGER_AT_START,
  val forceUpdate: Int = 0
)

data class Column(
  val paths: List<String>
)

