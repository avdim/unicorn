package com.intellij.psi.impl.smartPointers

import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import com.unicorn.Uni

object DebugBlackFile {
  @JvmStatic
  fun doDebug(equalityObject: Any, psiElement: PsiElement?) {
    if (equalityObject is SmartPsiElementPointer/*Impl*/<*>) {
      if (psiElement == null) {
        Uni.log.warning { "equalityObject.element: ${equalityObject.element}" };
        //debug (equalityObject as SmartPsiElementPointerImpl<*>).let{it.myElementInfo.restoreElement(it.myManager)}
      }
      if (equalityObject is SmartPsiElementPointerImpl2<*>) {
        val restoreElement = equalityObject.myElementInfo.restoreElement(equalityObject.myManager)
        if (restoreElement == null) {
          Uni.log.error { "restoreElement == null" }
        }
      }
    }
  }
}
