package ru.tutu.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.merge.MergeStrategy
import ru.tutu.log.TutuLog

fun GitDir.mergeWithBranch(branch: Branch) {
    TutuLog.info("Merging ${contentDir.name} with ${branch.name}")
    git.merge()
            .include(git.repository.resolve(branch.jGitRef.name))
            .setStrategy(MergeStrategy.RECURSIVE)
            .call().apply {
                if (conflicts != null && conflicts.isNotEmpty()) {
                    TutuLog.fatalError("Conflicts in: " + conflicts?.map { it.key }?.joinToString(", "))
                }
                TutuLog.debug("Merge-Results: $this")
            }
}

@Deprecated(message = "Use GitDir.branches instead")
val Git.branchNames
    get() = branchList().setListMode(ListBranchCommand.ListMode.ALL).call().map { it.name }