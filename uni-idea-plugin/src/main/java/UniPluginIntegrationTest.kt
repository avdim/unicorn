import com.unicorn.Uni
import com.unicorn.plugin.action.cmd.openDialogFileManager

class UniPluginIntegrationTest {
  init {
    Uni.log.info { "init UniPluginIntegrationTest" }
    openDialogFileManager()
  }
}
