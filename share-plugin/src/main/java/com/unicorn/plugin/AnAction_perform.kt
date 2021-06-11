package com.unicorn.plugin

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.ActionUtil
//import com.intellij.testFramework.TestActionEvent

fun AnAction.perform(
  context: DataContext = DataContext.EMPTY_CONTEXT,
  event: AnActionEvent = TestActionEvent2()
) {
  //https://intellij-support.jetbrains.com/hc/en-us/community/posts/206130119-Triggering-AnAction-instances-
//    val actionToolbar = ComponentUtil.getParentOfType(
//        ActionToolbar::class.java as Class<out ActionToolbar?>,
//        checkBox as Component?
//    )
//    val dataContext =
//        actionToolbar?.toolbarDataContext ?: DataManager.getInstance()
//            .getDataContext(checkBox)
//    val inputEvent: InputEvent = KeyEvent(
//        checkBox,
//        KeyEvent.KEY_PRESSED,
//        System.currentTimeMillis(),
//        0,
//        KeyEvent.VK_SPACE,
//        ' '
//    )
//    val event2 = AnActionEvent.createFromAnAction(this, inputEvent, ActionPlaces.UNKNOWN, dataContext)
  ActionUtil.performActionDumbAwareWithCallbacks(
    this,
    event,//todo Unicorn event
    context
  )
}

fun performActionById(id: String) {
  ActionManager.getInstance().getAction(id).perform()
}
