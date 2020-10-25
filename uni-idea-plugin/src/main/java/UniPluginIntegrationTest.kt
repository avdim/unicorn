import com.unicorn.Uni
import com.unicorn.plugin.action.id.openDialogFileManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UniPluginIntegrationTest(callback: (Boolean) -> Unit) {
  init {
    Uni.log.info { "init UniPluginIntegrationTest" }
    MainScope().launch {
      openDialogFileManager()
      delay(5000)
      callback(true)
    }
  }
}
