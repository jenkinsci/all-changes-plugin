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
    private int numChanges = 0;

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

    public int getNumChanges() {
        return numChanges;
    }
}
