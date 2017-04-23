package org.jenkinsci.plugins.all_changes;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import hudson.model.Action;
import hudson.scm.ChangeLogSet;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.Set;

public class AllChangesWorkflowAction implements Action {

    private WorkflowJob project;

    AllChangesWorkflowAction(WorkflowJob project) {
        this.project = project;
    }

    public String getIconFileName() {
        return "notepad.png";
    }

    public String getDisplayName() {
        return Messages.AllChanges_allChanges();
    }

    public String getUrlName() {
        return "all-changes";
    }

    /**
     * Returns all changes which contribute to a build.
     *
     * @param build
     * @return
     */
    public Multimap<ChangeLogSet.Entry, WorkflowRun> getAllChanges(WorkflowRun build) {
        Set<WorkflowRun> builds = getContributingBuilds(build);
        Multimap<String, ChangeLogSet.Entry> changes = ArrayListMultimap.create();
        for (WorkflowRun changedBuild : builds) {
            for (ChangeLogSet changeLogSet : changedBuild.getChangeSets()) {
                ChangeLogSet<ChangeLogSet.Entry> changeSet = (ChangeLogSet<ChangeLogSet.Entry>)changeLogSet;
                for (ChangeLogSet.Entry entry : changeSet) {
                    changes.put(entry.getCommitId() + entry.getMsgAnnotated() + entry.getTimestamp(), entry);
                }
            }
        }
        Multimap<ChangeLogSet.Entry, WorkflowRun> change2Build = HashMultimap.create();
        for (String changeKey : changes.keySet()) {
            ChangeLogSet.Entry change = changes.get(changeKey).iterator().next();
            for (ChangeLogSet.Entry entry : changes.get(changeKey)) {
                change2Build.put(change, (WorkflowRun) entry.getParent().getRun());
            }
        }
        return change2Build;
    }

    /**
     * Uses all ChangesAggregators to calculate the contributing builds
     *
     * @return all changes which contribute to the given build
     */
    public Set<WorkflowRun> getContributingBuilds(WorkflowRun build) {
        Set<WorkflowRun> builds = Sets.newHashSet();
        builds.add(build);
        return builds;
    }

    public WorkflowJob getProject() {
        return project;
    }
}
