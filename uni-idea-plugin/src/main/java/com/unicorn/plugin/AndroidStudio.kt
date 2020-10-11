package com.unicorn.plugin

import com.intellij.openapi.application.ApplicationInfo

fun checkAndroidStudio():Boolean {
  val appInfo = ApplicationInfo.getInstance()
  val build = appInfo.build
  appInfo.fullVersion
  return build.productCode == "AI"//IC
}