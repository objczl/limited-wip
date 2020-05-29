package limitedwip.common.vcs

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.VcsKey
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.changes.CommitExecutor
import com.intellij.openapi.vcs.changes.shelf.ShelveChangesCommitExecutor
import com.intellij.openapi.vcs.checkin.BeforeCheckinDialogHandler
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CheckinHandler.ReturnResult.CANCEL
import com.intellij.openapi.vcs.checkin.CheckinHandler.ReturnResult.COMMIT
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory
import com.intellij.openapi.vcs.checkin.VcsCheckinHandlerFactory
import com.intellij.openapi.vcs.impl.CheckinHandlersManager
import com.intellij.util.containers.MultiMap
import limitedwip.common.pluginId
import java.util.concurrent.CopyOnWriteArraySet

private val logger = Logger.getInstance(pluginId)

object AllowCommit: CheckinHandlerFactory() {
    private val listeners = CopyOnWriteArraySet<AllowCommitListener>()

    init {
        val wasRegistered = registerBeforeCommitListener(object: AllowCommitListener {
            override fun allowCommit(project: Project, changes: List<Change>): Boolean {
                return listeners.all { it.allowCommit(project, changes) }
            }
        })
        if (!wasRegistered) logger.warn("Failed to register commit listener.")
    }

    override fun createHandler(panel: CheckinProjectPanel, commitContext: CommitContext): CheckinHandler {
        return object : CheckinHandler() {
            override fun beforeCheckin(): ReturnResult {
                val canCommit = listeners.all { it.allowCommit(panel.project, panel.selectedChanges.toList()) }
                return if (canCommit) COMMIT else CANCEL
            }
        }
    }

    fun addListener(parentDisposable: Disposable, listener: AllowCommitListener) {
        Disposer.register(parentDisposable, Disposable {
            listeners.remove(listener)
        })
        listeners.add(listener)
    }
}

/**
 * This function is obsolete because:
 *  - the reflection trick doesn't work anymore since #IC-202.4357.23 or so
 *  - the new commit dialog is not in a separate window,
 *    and to cancel commits CheckinHandlerFactory must be used
 *
 * Not deleting this function for now to maintain compatibility with older IJ versions.
 */
private fun registerBeforeCommitListener(listener: AllowCommitListener): Boolean {
    // This is a hack caused by limitations of IntelliJ API.
    //  - cannot use CheckinHandlerFactory because:
    //		- CheckinHandler is used just before commit (and after displaying commit dialog)
    // 		- its CheckinHandlerFactory#createSystemReadyHandler() doesn't seem to be called
    //  - cannot use VcsCheckinHandlerFactory through extension points because need to register
    //    checkin handler for all VCSs available
    //  - cannot use CheckinHandlersManager#registerCheckinHandlerFactory() because it doesn't properly
    //    register VcsCheckinHandlerFactory
    //
    // Therefore, using reflection.

    return accessField<MultiMap<VcsKey, VcsCheckinHandlerFactory>>(CheckinHandlersManager.getInstance(), listOf("a", "b", "myVcsMap", "vcsFactories")) { multiMap ->
        for (key in multiMap.keySet()) {
            multiMap.putValue(key, DelegatingCheckinHandlerFactory(key as VcsKey, listener))
        }
    }
}

private inline fun <reified T> accessField(anObject: Any, possibleFieldNames: List<String>, f: (T) -> Unit): Boolean {
    for (field in anObject.javaClass.declaredFields) {
        if (possibleFieldNames.contains(field.name) && T::class.java.isAssignableFrom(field.type)) {
            field.isAccessible = true
            try {
                f.invoke(field.get(anObject) as T)
                return true
            } catch (ignored: Exception) {
            }
        }
    }
    return false
}


interface AllowCommitListener {
    fun allowCommit(project: Project, changes: List<Change>): Boolean
}


private class DelegatingCheckinHandlerFactory(key: VcsKey, private val listener: AllowCommitListener): VcsCheckinHandlerFactory(key) {
    override fun createSystemReadyHandler(project: Project): BeforeCheckinDialogHandler? {
        return object: BeforeCheckinDialogHandler() {
            override fun beforeCommitDialogShown(project: Project, changes: List<Change>, executors: Iterable<CommitExecutor>, showVcsCommit: Boolean): Boolean {
                return executors.all { it is ShelveChangesCommitExecutor } || listener.allowCommit(project, changes)
            }
        }
    }

    override fun createVcsHandler(panel: CheckinProjectPanel): CheckinHandler {
        return object: CheckinHandler() {}
    }
}
