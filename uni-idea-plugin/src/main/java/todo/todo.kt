package todo

import com.intellij.ide.actions.ActivateToolWindowAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.WindowManager
import java.io.File

fun todo() {

  val findFileByIoFile: VirtualFile? = LocalFileSystem.getInstance().findFileByIoFile(File("/"))

  val fs: LocalFileSystem = TODO()
  fs.refreshAndFindFileByPath("file path")

  val file: VirtualFile? = FileDocumentManager.getInstance().getFile(TODO("Document"))

//    VfsUtilCore.iterateChildrenRecursively()

  VirtualFileManager.getInstance().asyncRefresh { }


}

fun todoAction() {
  /**
  final AnAction menuItemAction = myAction.getAction();
  if (ActionUtil.lastUpdateAndCheckDumb(menuItemAction, event, false)) {
  ActionManagerEx actionManager = ActionManagerEx.getInstanceEx();
  actionManager.fireBeforeActionPerformed(menuItemAction, myContext, event);
  focusManager.doWhenFocusSettlesDown(typeAhead::setDone);
  ActionUtil.performActionDumbAware(menuItemAction, event);
  actionManager.queueActionPerformedEvent(menuItemAction, myContext, event);
  }
  else {
  typeAhead.setDone();
  }
   */
}

fun todoOldHelpCode(e: AnActionEvent) {
  WindowManager.getInstance()
//        val toolWindow = ToolWindowManager.getInstance(e.getProject()!!).getToolWindow("MyPlugin")
//        val consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(e.getProject()!!).console
//        val content = toolWindow.getContentManager().getFactory()
//            .createContent(consoleView.getComponent(), "MyPlugin Output", false)
//        toolWindow.getContentManager().addContent(content)
//        consoleView.print("Hello from MyPlugin!", ConsoleViewContentType.NORMAL_OUTPUT)

}
