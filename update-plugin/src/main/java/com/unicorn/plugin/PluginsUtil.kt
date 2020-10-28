package com.unicorn.plugin

import com.intellij.ide.plugins.PluginDescriptorLoader
import com.intellij.ide.plugins.PluginInstaller
import com.unicorn.update.BuildConfig
import java.io.File
import java.io.FileFilter
import javax.swing.JComponent

fun installPlugin(file: File, parentComponent: JComponent?):Boolean {
    val descriptor = PluginDescriptorLoader.loadDescriptorFromArtifact(file.toPath(), null)

    @Suppress("MissingRecentApi")//todo suppress
    val result = PluginInstaller.installAndLoadDynamicPlugin(
        file.toPath(),
        parentComponent,
        descriptor
    )

    println("install plugin result: $result, path: ${file.absolutePath}")
    return result
}

fun removeUniPlugin(parentComponent: JComponent?):Boolean {
    //todo get descriptor from Idea
    val file = File("/Users/dim/Desktop/unicorn-0.11.0.zip")
    val descriptor = PluginDescriptorLoader.loadDescriptorFromArtifact(file.toPath(), null)
    val result = PluginInstaller.uninstallDynamicPlugin(
        parentComponent,
        descriptor!!,
        true
    )

    println("uninstall uni-plugin result: $result")
    return result
}

fun buildDistPlugins(): List<String> {
    val distDir = File(BuildConfig.UNI_ZIP_BUILD_DIST)
    val zipFiles =
        if (distDir.exists()) {
          distDir
            .listFiles(FileFilter { it.extension == "zip" })
            .sortedByDescending { it.lastModified() }
            .map { it.absolutePath }
        } else {
            emptyList()
        }
    return zipFiles
}
