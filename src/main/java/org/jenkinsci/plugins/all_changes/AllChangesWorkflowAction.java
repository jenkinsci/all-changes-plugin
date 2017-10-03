/*
 * The MIT License
 *
 * Copyright (c) 2017, Suresh
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.all_changes;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.Set;

public class AllChangesWorkflowAction implements Action {

    private WorkflowJob project;
    private int numChanges = 0;
    private DependencyChangesAggregator aggregator;

    AllChangesWorkflowAction(WorkflowJob project) {
        this.project = project;
    }

    AllChangesWorkflowAction(WorkflowJob project, int numChanges) {
        this.project = project;
        this.numChanges = numChanges;
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
     * Returns all changes which contribute to the given build.
     *
     * @param build the build from which to get dependency changes
     * @return a map of change log sets to builds or empty map if none were found
     */
    public Multimap<ChangeLogSet.Entry, Run> getAllChanges(WorkflowRun build) {

        Set<Run> builds = getContributingBuilds(build);

        Multimap<String, ChangeLogSet.Entry> changes = ArrayListMultimap.create();
        for (Run changedBuild : builds) {
            if (changedBuild instanceof WorkflowRun) {
                for (ChangeLogSet changeLogSet : ((WorkflowRun) changedBuild).getChangeSets()) {
                    ChangeLogSet<ChangeLogSet.Entry> changeSet = (ChangeLogSet<ChangeLogSet.Entry>)changeLogSet;
                    for (ChangeLogSet.Entry entry : changeSet) {
                        changes.put(entry.getCommitId() + entry.getMsgAnnotated() + entry.getTimestamp(), entry);
                    }
                }
            } else if (changedBuild instanceof AbstractBuild) {
                ChangeLogSet<ChangeLogSet.Entry> changeSet = ((AbstractBuild) changedBuild).getChangeSet();
                for (ChangeLogSet.Entry entry : changeSet) {
                    changes.put(entry.getCommitId() + entry.getMsgAnnotated() + entry.getTimestamp(), entry);
                }
            }
        }

        Multimap<ChangeLogSet.Entry, Run> change2Build = HashMultimap.create();
        for (String changeKey : changes.keySet()) {
            ChangeLogSet.Entry change = changes.get(changeKey).iterator().next();
            for (ChangeLogSet.Entry entry : changes.get(changeKey)) {
                change2Build.put(change, entry.getParent().getRun());
            }
        }

        return change2Build;
    }

    /**
     * Uses DependencyChangesAggregator to calculate the contributing builds.
     *
     * @param build the workflow build to get dependencies for
     * @return all changed builds which contribute to the given build or empty set if none were found
     */
    public Set<Run> getContributingBuilds(WorkflowRun build) {

        Set<Run> builds = Sets.newHashSet();
        builds.add(build);

        if (aggregator == null) {
            aggregator = new DependencyChangesAggregator();
        }

        int size = 0;
        // Saturate the build Set
        do {
            size = builds.size();
            Set<Run> newBuilds = Sets.newHashSet();
            for (Run depBuild : builds) {
                newBuilds.addAll(aggregator.aggregateBuildsWithChanges(depBuild));
            }
            builds.addAll(newBuilds);
        } while (size < builds.size());

        return builds;
    }

    public WorkflowJob getProject() {
        return project;
    }

    public int getNumChanges() {
        return numChanges;
    }

    public void setAggregator(DependencyChangesAggregator aggregator) {
        this.aggregator = aggregator;
    }
}
