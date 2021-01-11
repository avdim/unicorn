package com.intellij.psi.impl.smartPointers

import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import com.unicorn.Uni

object DebugBlackFile {
  @JvmStatic
  fun doDebug(equalityObject: Any, psiElement: PsiElement?) {
    if (equalityObject is SmartPsiElementPointer/*Impl*/<*>) {
      val element = equalityObject.element
      if (psiElement == null || !psiElement.isValid()) {
        Uni.log.debug { "equalityObject.element: ${equalityObject.element}" };
        //debug (equalityObject as SmartPsiElementPointerImpl<*>).let{it.myElementInfo.restoreElement(it.myManager)}
      }
    }
  }
}
