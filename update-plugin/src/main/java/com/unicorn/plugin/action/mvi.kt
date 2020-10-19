package com.unicorn.plugin.action

import com.intellij.ide.plugins.PluginDescriptorLoader
import com.intellij.ide.plugins.PluginInstaller
import com.sample.Release
import com.sample.getGithubRepoReleases
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import ru.avdim.mvi.APP_SCOPE
import ru.avdim.mvi.ReducerResult
import ru.avdim.mvi.createStoreWithSideEffect
import java.io.File
import javax.swing.JComponent

data class InstalledPlugin(
    val currentInfo: String
)

data class Loading(
    val info:String
)

data class Loaded(
    val path: String
)

data class State(
    val releases: List<Release>? = null,
    val selection: Release? = null,
    val current: InstalledPlugin? = null,
    val loading: Loading? = null,
    val loaded: Loaded? = null
)

sealed class Action {
    class SetReleases(val list: List<Release>) : Action()
    class SetCurrent(val currentInfo: String) : Action()
    class StartLoading(val release: Release) : Action()
    class Install(val parentComponent: JComponent) : Action()
    class Remove(val parentComponent: JComponent) : Action()
    class Loaded(val path: String):Action()
    class LoadingInfo(val info:String) : Action()
    object LoadReleases:Action()
}

sealed class Effect {
    object CheckCurrent : Effect()
    object LoadReleases : Effect()
    class DownloadPlugin(val release:Release):Effect()
    class InstallPlugin(val path: String, val parentComponent: JComponent?) : Effect()
    class RemovePlugin(val parentComponent: JComponent?) : Effect()
}

fun createUpdateStore() = createStoreWithSideEffect(State(),
    { store, effect: Effect ->
        when (effect) {
            is Effect.CheckCurrent -> {
                //todo
            }
            is Effect.LoadReleases -> {
                val client = HttpClient(Apache)
                APP_SCOPE.launch {
                    val releases = client.getGithubRepoReleases("avdim", "unicorn")
                    store.send(
                        Action.SetReleases(releases)
                    )
                }
            }
            is Effect.DownloadPlugin -> {
                val downloadUrl = effect.release.assets.firstOrNull()?.browser_download_url
                if (downloadUrl != null) {
                    val client = HttpClient(Apache) {
                        followRedirects = true
                        engine {
                            followRedirects = true
                            connectTimeout = 300_000
                            socketTimeout = 300_000
                        }
                    }
                    APP_SCOPE.launch {
                        println("start loading $downloadUrl")
                        val url = Url(downloadUrl)
//                        val statement = client.request<HttpStatement>(url)
//                        val response = statement.execute()
//                        response.content
                        val name = url.encodedPath.split("/").last()
                        val file = createTempDir("plugin").resolve(name)
                        file.createNewFile()
                        println("download to file ${file.absolutePath}")
                        client.downloadFile(file, url)
                            .flowOn(Dispatchers.IO)
                            .collect { download ->
                            when(download) {
                                is DownloadResult.Error -> {
                                    store.send(
                                        Action.LoadingInfo(download.message)
                                    )
                                }
                                is DownloadResult.Progress -> {
                                    store.send(
                                        Action.LoadingInfo("progress: ${download.progress}")
                                    )
                                }
                                is DownloadResult.Success -> {
                                    store.send(Action.Loaded(file.absolutePath))

                                }
                            }
                        }
                    }
                }
            }
            is Effect.InstallPlugin -> {
                val file = File(effect.path)
                val descriptor = PluginDescriptorLoader.loadDescriptorFromArtifact(file.toPath(), null)
                @Suppress("MissingRecentApi")//todo suppress
                PluginInstaller.installAndLoadDynamicPlugin(
                    file.toPath(),
                    effect.parentComponent,
                    descriptor
                )
            }
            is Effect.RemovePlugin -> {
                //todo get descriptor from Idea
                val file = File("/Users/dim/Desktop/unicorn-0.11.0.zip")
                val descriptor = PluginDescriptorLoader.loadDescriptorFromArtifact(file.toPath(), null)
                PluginInstaller.uninstallDynamicPlugin(
                    effect.parentComponent,
                    descriptor,
                    true
                )
            }
        }
    }) { s, a: Action ->
    when (a) {
        is Action.LoadReleases -> {
            ReducerResult(
                s.copy(),
                listOf(Effect.LoadReleases)
            )
        }
        is Action.SetReleases -> {
            ReducerResult(
                s.copy(
                    releases = a.list
                )
            )
        }
        is Action.StartLoading -> {
            ReducerResult(
                s.copy(
                    loading = Loading(
                        info = "loading ${a.release.url}"
                    )
                ),
                listOf(Effect.DownloadPlugin(a.release))
            )
        }
        is Action.Loaded -> {
            ReducerResult(
                s.copy(
                    loaded = Loaded(a.path)
                )
            )
        }
        is Action.Install -> {
            ReducerResult(
                s.copy(),
                if(s.loaded != null) {
                    listOf(Effect.InstallPlugin(s.loaded.path, a.parentComponent))
                } else {
                    listOf()
                }

            )
        }
        is Action.SetCurrent -> {
            ReducerResult(
                s.copy(
                    current = InstalledPlugin(a.currentInfo)
                )
            )
        }
        is Action.Remove -> {
            ReducerResult(
                s.copy(

                ),
                listOf(Effect.RemovePlugin(a.parentComponent))
            )
        }
        is Action.LoadingInfo -> {
            ReducerResult(
                s.copy(
                    loading = Loading(a.info)
                )
            )
        }
    }
}
