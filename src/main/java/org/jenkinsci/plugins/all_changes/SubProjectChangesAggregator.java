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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import hudson.Extension;
import hudson.Plugin;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;
import hudson.tasks.Builder;
import hudson.util.RunList;
import jenkins.model.Jenkins;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author wolfs
 */
@Extension
public class SubProjectChangesAggregator extends ChangesAggregator<AbstractBuild> {

    @Override
    public Collection<AbstractBuild> aggregateBuildsWithChanges(AbstractBuild build) {
        Plugin parameterizedTrigger = Util.getInstance().getPlugin("parameterized-trigger");
        if (parameterizedTrigger == null) {
            return ImmutableList.of();
        }

        AbstractProject project = build.getProject();
        Set<AbstractProject<?, ?>> subProjects = Sets.newHashSet();
        Set<AbstractBuild> builds = Sets.newHashSet();
        if (project instanceof FreeStyleProject) {
            subProjects.addAll(getSubProjects((FreeStyleProject) project));
            builds.addAll(getTriggeredBuilds(build, subProjects));
        }

        return builds;
    }

    private List<AbstractBuild> getTriggeredBuilds(AbstractBuild build, Collection<AbstractProject<?, ?>> subProjects) {
        List<AbstractBuild> builds = Lists.newArrayList();
        for (AbstractProject<?, ?> subProject : subProjects) {
            RunList<? extends AbstractBuild<?, ?>> subBuildsDuringBuild = subProject.getBuilds().byTimestamp(build.getTimeInMillis(), build.getTimeInMillis() + build.getDuration());
            for (AbstractBuild<?, ?> candidate : subBuildsDuringBuild) {
                if (isSubBuild(build, candidate)) {
                    builds.add(candidate);
                }
            }
        }
        return builds;
    }

    private List<AbstractProject<?, ?>> getSubProjects(FreeStyleProject project) {
        List<AbstractProject<?, ?>> subProjects = Lists.newArrayList();
        List<Builder> builders = project.getBuilders();
        for (Builder builder : builders) {
            if (builder instanceof TriggerBuilder) {
                TriggerBuilder tBuilder = (TriggerBuilder) builder;
                for (BlockableBuildTriggerConfig config : tBuilder.getConfigs()) {
                    for (AbstractProject<?, ?> abstractProject : config.getProjectList(null)) {
                        if (config.getBlock() != null) {
                            subProjects.add(abstractProject);
                        }
                    }
                }
            }
        }
        return subProjects;
    }

    private boolean isSubBuild(AbstractBuild build, AbstractBuild<?, ?> subBuild) {
        List<Cause> causes = subBuild.getCauses();
        for (Cause cause : causes) {
            if (cause instanceof Cause.UpstreamCause) {
                Cause.UpstreamCause upstreamCause = (Cause.UpstreamCause) cause;
                if (upstreamCause.pointsTo(build)) {
                    return true;
                }
            }
        }
        return false;
    }
}
