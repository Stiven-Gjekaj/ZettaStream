package io.github.stivengjekaj.zettastream

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import io.github.stivengjekaj.zettastream.addon.AddonClient
import io.github.stivengjekaj.zettastream.addon.AddonRepository
import io.github.stivengjekaj.zettastream.iptv.LiveRepository
import io.github.stivengjekaj.zettastream.library.Library
import io.github.stivengjekaj.zettastream.net.Http
import io.github.stivengjekaj.zettastream.source.SourceStore
import io.github.stivengjekaj.zettastream.update.UpdateChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/** Makes the objects that live as long as the app. */
class AppContainer(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val http = Http.client(app.cacheDir)
    val sources = SourceStore(File(app.filesDir, "sources.txt"))
    val addons = AddonRepository(AddonClient(http), sources.sources, scope)
    val live = LiveRepository(http, sources.sources, scope)
    val library = Library(File(app.filesDir, "library.json"))
    val updates = UpdateChecker(http)
}

class ZettaStreamApp : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .components { add(OkHttpNetworkFetcherFactory(callFactory = { container.http })) }
        .build()
}
