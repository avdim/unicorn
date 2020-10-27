package com.unicorn.plugin.action

import com.sample.Release
import com.sample.getGithubRepoReleases
import com.unicorn.plugin.buildDistPlugins
import com.unicorn.plugin.installPlugin
import com.unicorn.plugin.removeUniPlugin
import com.unicorn.plugin.update.assertTrue
import com.unicorn.plugin.update.waitPlugin
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
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
    val buildDirZipFiles: List<String> = emptyList(),
    val loaded: Loaded? = null
)

sealed class Action {
    class SetReleases(val list: List<Release>) : Action()
    class SetCurrent(val currentInfo: String) : Action()
    class StartLoading(val release: Release) : Action()
    class Install(val zipFilePath: String, val parentComponent: JComponent) : Action()
    class Remove(val parentComponent: JComponent) : Action()
    class Loaded(val path: String) : Action()
    class LoadingInfo(val info: String) : Action()
    class SetBuildDirFiles(val zipFiles: List<String>) : Action()
    object Init : Action()
}

sealed class Effect {
    object CheckCurrent : Effect()
    object CheckBuildDir : Effect()
    object LoadReleases : Effect()
    class DownloadPlugin(val release:Release):Effect()
    class InstallPlugin(val path: String, val parentComponent: JComponent?) : Effect()
    class RemovePlugin(val parentComponent: JComponent?) : Effect()
}

fun createUpdateStore() = createStoreWithSideEffect(
    State(),
    { store, effect: Effect ->
        when (effect) {
            is Effect.CheckBuildDir -> {
                store.send(
                    Action.SetBuildDirFiles(
                        buildDistPlugins()
                    )
                )
            }
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
                                when (download) {
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
                                        store.send(
                                            Action.Loaded(file.absolutePath)
                                        )
                                    }
                                }
                            }
                    }
                }
            }
            is Effect.InstallPlugin -> {
                val file = File(effect.path)
                val parentComponent = effect.parentComponent
                installPlugin(file, parentComponent)
              MainScope().launch {
                val asyncPlugin = GlobalScope.async {
                  waitPlugin("UniCorn")
                }
                installPlugin(file, parentComponent)
                /**
                 * Переменная classLoader должна иметь маленькую область видимости.
                 * Если будет держаться ссылка, то плагин не получится динамически выгрузить.
                 */
                val classLoader = asyncPlugin.await()
                val className = /*com.package...*/"UniPluginDynamicInit"
                val loadedClass: Class<*> = classLoader.loadClass(className)
                loadedClass.constructors[0].newInstance()
              }
            }
            is Effect.RemovePlugin -> {
                val parentComponent = effect.parentComponent
                removeUniPlugin(parentComponent)
            }
        }
    }) { s, a: Action ->
    when (a) {
        is Action.Init -> {
            ReducerResult(
                s.copy(),
                listOf(
                    Effect.LoadReleases,
                    Effect.CheckCurrent,
                    Effect.CheckBuildDir
                )
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
                listOf(
                    Effect.InstallPlugin(a.zipFilePath, a.parentComponent)
                )
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
        is Action.SetBuildDirFiles -> {
            ReducerResult(
                s.copy(
                    buildDirZipFiles = a.zipFiles
                )
            )
        }
    }
}.apply {
    send(Action.Init)
}

