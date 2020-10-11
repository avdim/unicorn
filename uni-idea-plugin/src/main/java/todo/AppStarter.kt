package todo

import com.intellij.openapi.application.ApplicationStarter
import com.unicorn.Uni

class AppStarter : ApplicationStarter {
  //com.unicorn.plugin.todo not working
  override fun getCommandName(): String {
    return "my_command_name"
  }

  override fun main(p0: Array<out String>) {
    Uni.log.debug { "AppStarter" }
  }

}