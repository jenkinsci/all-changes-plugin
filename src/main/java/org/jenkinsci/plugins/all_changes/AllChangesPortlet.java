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
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
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
        return jenkinsJobName == null ? "" : jenkinsJobName.replace(" ", "%20");
    }

    public int getNumChanges() {
        return numChanges;
    }

    public Object getProjectAction() {
        return resolveProject(jenkinsJobName);
    }

    private Object resolveProject(String name) {
        Object project = Util.getInstance().getItem(name, Util.getInstance(), AbstractProject.class);
        if (project == null) {
            project = Util.getInstance().getItem(name, Util.getInstance(), WorkflowJob.class);
        }
        if (project instanceof AbstractProject) {
            return new AllChangesAction((AbstractProject) project, this.numChanges);
        } else if (project instanceof WorkflowJob) {
            return new AllChangesWorkflowAction((WorkflowJob) project, this.numChanges);
        }
        return null;
    }

    @Extension
    public static class AllChangesPortletDescriptor extends Descriptor<DashboardPortlet> {

        @Override
        public String getDisplayName() {
            return "All Changes Portlet";
        }
    }
}
