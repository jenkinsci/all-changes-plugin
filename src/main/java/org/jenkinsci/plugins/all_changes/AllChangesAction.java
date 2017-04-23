/*
 * The MIT License
 *
 * Copyright (c) 2011, Stefan Wolf
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
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.scm.ChangeLogSet;

import java.util.List;
import java.util.Set;

/**
 * Action to calculate all changes for a build
 * It uses ChangesAggregators to to so.
 *
 * @author wolfs
 */
public class AllChangesAction implements Action {

    private AbstractProject<?, ?> project;
    private int numChanges = 0;
    transient
    List<ChangesAggregator> aggregators;

    AllChangesAction(AbstractProject<?, ?> project) {
        this.project = project;
    }

    AllChangesAction(AbstractProject<?, ?> project, int numChanges) {
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
    public Multimap<ChangeLogSet.Entry, AbstractBuild> getAllChanges(AbstractBuild build) {
        Set<AbstractBuild> builds = getContributingBuilds(build);
        Multimap<String, ChangeLogSet.Entry> changes = ArrayListMultimap.create();
        for (AbstractBuild changedBuild : builds) {
            ChangeLogSet<ChangeLogSet.Entry> changeSet = changedBuild.getChangeSet();
            for (ChangeLogSet.Entry entry : changeSet) {
                changes.put(entry.getCommitId() + entry.getMsgAnnotated() + entry.getTimestamp(), entry);
            }
        }
        Multimap<ChangeLogSet.Entry, AbstractBuild> change2Build = HashMultimap.create();
        for (String changeKey : changes.keySet()) {
            ChangeLogSet.Entry change = changes.get(changeKey).iterator().next();
            for (ChangeLogSet.Entry entry : changes.get(changeKey)) {
                change2Build.put(change, entry.getParent().build);
            }
        }
        return change2Build;
    }

    /**
     * Uses all ChangesAggregators to calculate the contributing builds
     *
     * @return all changes which contribute to the given build
     */
    public Set<AbstractBuild> getContributingBuilds(AbstractBuild build) {
        if (aggregators == null) {
            aggregators = ImmutableList.copyOf(ChangesAggregator.all());
        }
        Set<AbstractBuild> builds = Sets.newHashSet();
        builds.add(build);
        int size = 0;
        // Saturate the build Set
        do {
            size = builds.size();
            Set<AbstractBuild> newBuilds = Sets.newHashSet();
            for (ChangesAggregator aggregator : aggregators) {
                for (AbstractBuild depBuild : builds) {
                    newBuilds.addAll(aggregator.aggregateBuildsWithChanges(depBuild));
                }
            }
            builds.addAll(newBuilds);
        } while (size < builds.size());
        return builds;
    }

    public AbstractProject<?, ?> getProject() {
        return project;
    }

    public int getNumChanges() {
        return numChanges;
    }
}
