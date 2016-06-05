/*
 * The MIT License
 *
 * Copyright (c) 2016, Suresh
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
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.plugins.view.dashboard.DashboardPortlet;
import hudson.scm.ChangeLogSet;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;
import java.util.Set;

/**
 * Portlet to calculate all changes for a build
 * It uses ChangesAggregators to do so.
 *
 * @author suresh
 */
public class AllChangesPortlet extends DashboardPortlet {

    private final String jenkinsJobName;
    private final int numChanges;

    private transient List<ChangesAggregator> aggregators;

    @DataBoundConstructor
    public AllChangesPortlet(String name, String jenkinsJobName, int numChanges) {
        super(name);
        this.jenkinsJobName = jenkinsJobName;
        this.numChanges = numChanges;
    }

    public String getJenkinsJobName() {
        return jenkinsJobName;
    }

    public String getJenkinsJobNameForUrl() {
        return jenkinsJobName==null?"":jenkinsJobName.replace(" ", "%20");
    }

    public int getNumChanges() {
        return numChanges;
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
        return resolveProject(jenkinsJobName);
    }

    private AbstractProject<?, ?> resolveProject(String name) {
        return Util.getInstance().getItem(name, Util.getInstance(), AbstractProject.class);
    }

    @Extension
    public static class AllChangesPortletDescriptor extends Descriptor<DashboardPortlet> {

        @Override
        public String getDisplayName() {
            return "All Changes Portlet";
        }
    }
}
