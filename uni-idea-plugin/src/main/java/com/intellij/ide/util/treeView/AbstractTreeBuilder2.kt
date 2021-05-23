package com.intellij.ide.util.treeView

import com.intellij.ide.projectView.impl.nodes.AbstractTreeNod2
import com.intellij.my.file.AbstractProjectTreeStructure2
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Progressive
import com.intellij.openapi.util.ActionCallback
import com.intellij.reference.SoftReference
import com.intellij.util.Consumer
import com.intellij.util.concurrency.EdtExecutorService
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.update.MergingUpdateQueue
import org.jetbrains.annotations.NonNls
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.rejectedPromise
import java.lang.ref.Reference
import java.lang.ref.WeakReference
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

@Deprecated("use {@link com.intellij.ui.tree.AsyncTreeModel} and {@link com.intellij.ui.tree.StructureTreeModel} instead.")
abstract class AbstractTreeBuilder2 protected constructor() : Disposable {
    private var myUi: AbstractTreeUi2? = null
    protected fun init(
        tree: JTree,
        treeModel: DefaultTreeModel,
        treeStructure: AbstractProjectTreeStructure2?
    ) {
        tree.putClientProperty(TREE_BUILDER, WeakReference(this))
        myUi = createUi()
        myUi!!.init(this, tree, treeModel, treeStructure)
        setPassthroughMode(isUnitTestingMode)
    }

    protected fun createUi(): AbstractTreeUi2 {
        return AbstractTreeUi2()
    }

    fun scrollTo(element: Any?, onDone: Runnable?) {
        val ui = ui
        ui?.userScrollTo(
            element,
            if (onDone == null) null else UserRunnable(onDone)
        )
    }

    fun expand(element: Any?, onDone: Runnable?) {
        val ui = ui
        ui?.expand(element, UserRunnable(onDone))
    }

    fun collapseChildren(element: Any?, onDone: Runnable?) {
        val ui = ui
        ui?.collapseChildren(element!!, UserRunnable(onDone))
    }

    fun createUpdater(): AbstractTreeUpdater2? {
        if (isDisposed) return null
        val updater = AbstractTreeUpdater2(this)
        updater.setModalityStateComponent(MergingUpdateQueue.ANY_COMPONENT)
        return updater
    }

    protected val updater: AbstractTreeUpdater2?
        protected get() {
            val ui = ui
            return ui?.updater
        }

    fun addSubtreeToUpdateByElement(element: Any): Boolean {
        val updater = updater
        return updater != null && updater.addSubtreeToUpdateByElement(element)
    }

    val rootNode: DefaultMutableTreeNode?
        get() {
            val ui = ui
            return ui?.rootNode
        }

    fun setNodeDescriptorComparator(nodeDescriptorComparator: Comparator<in AbstractTreeNod2<*>?>?) {
        val ui = ui
        ui?.setNodeDescriptorComparator(nodeDescriptorComparator)
    }

    /**
     * node descriptor getElement contract is as follows:
     * 1.TreeStructure always returns & receives "treeStructure" element returned by getTreeStructureElement
     * 2.Paths contain "model" element returned by getElement
     */
    fun getTreeStructureElement(nodeDescriptor: AbstractTreeNod2<*>?): AbstractTreeNod2<*>? {
        return nodeDescriptor?.element
    }

    open fun validateNode(child: Any): Boolean {
        val structure = treeStructure
        return structure != null && structure.isValid(child)
    }

    val tree: JTree?
        get() {
            val ui = ui
            return ui?.tree
        }
    val treeStructure: AbstractTreeStructure?
        get() {
            val ui = ui
            return ui?.treeStructure
        }
    val rootElement: Any?
        get() {
            val structure = treeStructure
            return structure?.rootElement
        }

    @Deprecated("use {@link #queueUpdate()}")
    fun updateFromRoot() {
        queueUpdate()
    }

    fun initRootNode() {
        val ui = ui
        ui?.initRootNode()
    }

    @JvmOverloads
    fun queueUpdate(withStructure: Boolean = true): ActionCallback {
        return queueUpdateFrom(rootElement, true, withStructure)
    }

    fun queueUpdateFrom(element: Any?, forceResort: Boolean) {
        queueUpdateFrom(element, forceResort, true)
    }

    fun queueUpdateFrom(element: Any?, forceResort: Boolean, updateStructure: Boolean): ActionCallback {
        val ui = ui ?: return ActionCallback.REJECTED
        val result = ActionCallback()
        ui.invokeLaterIfNeeded(false, object : TreeRunnable2("AbstractTreeBuilder.queueUpdateFrom") {
            public override fun perform() {
              if (updateStructure && forceResort) {
                    ui.incComparatorStamp()
                }
                ui.queueUpdate(element, updateStructure).notify(result)
            }
        })
        return result
    }

    @Deprecated("")
    fun getNodeForElement(element: Any): DefaultMutableTreeNode? {
        val ui = ui
        return ui?.getNodeForElement(element, false)
    }

    fun cleanUp() {
        val ui = ui
        ui?.doCleanUp()
    }

    open fun createProgressIndicator(): ProgressIndicator? {
        return null
    }

    open fun expandNodeChildren(node: DefaultMutableTreeNode) {
        val ui = ui
        ui?.doExpandNodeChildren(node)
    }

    abstract fun isAlwaysShowPlus(descriptor: AbstractTreeNod2<*>?): Boolean
    val isDisposed: Boolean
        get() = ui == null

    fun findNodeByElement(element: Any): Any? {
        val ui = ui
        return ui?.findNodeByElement(element)
    }

    fun runOnYieldingDone(onDone: Runnable) {
        val ui = ui ?: return
        if (ui.isPassthroughMode || SwingUtilities.isEventDispatchThread()) {
            onDone.run()
        } else {
            EdtExecutorService.getInstance().execute { if (!isDisposed) onDone.run() }
        }
    }

    fun yieldToEDT(runnable: Runnable) {
        val ui = ui ?: return
        if (ui.isPassthroughMode) {
            runnable.run()
        } else {
            EdtExecutorService.getInstance().execute { if (!isDisposed) runnable.run() }
        }
    }

    fun runBackgroundLoading(runnable: Runnable) {
        if (isDisposed) return
        val app = ApplicationManager.getApplication()
        if (app != null) {
            app.runReadAction(object : TreeRunnable2("AbstractTreeBuilder.runBackgroundLoading") {
                public override fun perform() {
                    runnable.run()
                }
            })
        } else {
            runnable.run()
        }
    }

    fun updateAfterLoadedInBackground(runnable: Runnable) {
        val ui = ui ?: return
        if (ui.isPassthroughMode) {
            runnable.run()
        } else {
            UIUtil.invokeLaterIfNeeded(runnable)
        }
    }

    fun getReady(requestor: Any?): ActionCallback {
        val ui = ui
        return if (ui == null) ActionCallback.REJECTED else ui.getReady(requestor!!)
    }

    fun setPassthroughMode(passthrough: Boolean) {
        val ui = ui
        if (ui != null) ui.isPassthroughMode = passthrough
    }

    fun batch(progressive: Progressive) {
        val ui = ui
        ui?.batch(progressive)
    }

    open fun revalidateElement(element: Any): Promise<Any?> {
        val structure = treeStructure ?: return rejectedPromise()
        val promise = AsyncPromise<Any?>()
        structure
            .revalidateElement(element)
            .doWhenDone(Consumer { o: Any? -> promise.setResult(o) })
            .doWhenRejected { s: String? -> promise.setError(s!!) }
        return promise
    }

    val ui: AbstractTreeUi2?
        get() {
            val ui = myUi
            return if (ui == null || ui.isReleaseRequested) null else ui
        }

    override fun dispose() {
        val ui = ui
        ui?.requestRelease()
    }

    fun releaseUi() {
        myUi = null
    }

    fun updateNodeDescriptor(descriptor: AbstractTreeNod2<*>): Boolean {
        val ui = ui
        return ui != null && descriptor.update()
    }

    fun setCanYieldUpdate(yield2: Boolean) {
        val ui = ui
        ui?.setCanYield(yield2)
    }

    internal inner class UserRunnable(private val myRunnable: Runnable?) : Runnable {
        override fun run() {
            if (myRunnable != null) {
                val ui = ui
                if (ui != null) {
                    ui.executeUserRunnable(myRunnable)
                } else {
                    myRunnable.run()
                }
            }
        }
    }

    companion object {
        @NonNls
        private val TREE_BUILDER = "TreeBuilder"
        const val DEFAULT_UPDATE_INACTIVE = true

      private val isUnitTestingMode: Boolean
            private get() {
                val app = ApplicationManager.getApplication()
                return app != null && app.isUnitTestMode
            }
    }
}