package com.unicorn.plugin.mvi

import com.unicorn.BuildConfig

data class UniWindowState(
  val columns: List<Column>,
  val renderFiles: Boolean = !BuildConfig.DYNAMIC_UNLOAD,
  val forceUpdate: Int = 0
)

data class Column(
  val paths: List<String>
)

