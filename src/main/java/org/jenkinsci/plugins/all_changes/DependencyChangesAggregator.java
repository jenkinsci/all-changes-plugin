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
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.tasks.Fingerprinter;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wolfs
 */
@Extension
public class DependencyChangesAggregator extends ChangesAggregator<Run> {

    @Override
    public Collection<Run> aggregateBuildsWithChanges(Run build) {
        ImmutableList.Builder<Run> builder = ImmutableList.builder();
        Map<AbstractProject, AbstractBuild.DependencyChange> depChanges = getDependencyChanges(build, build.getPreviousBuild());
        for (AbstractBuild.DependencyChange depChange : depChanges.values()) {
            builder.addAll(depChange.getBuilds());
        }
        return builder.build();
    }

    /**
     * Gets the changes in the dependency between two given builds using {@link Fingerprinter.FingerprintAction}.
     *
     * <p>This implements the functionality from {@link AbstractBuild#getDependencyChanges(AbstractBuild)} using
     * {@link Run} instead of {@link AbstractBuild} as input build parameters, so that this aggregator can be used by
     * the {@link AllChangesWorkflowAction} as well.
     *
     * @param build the current build to find dependencies to
     * @param otherBuild another build to find dependencies from
     * @return a map of projects to dependency changes or an empty map if there are no fingerprint actions or the other job is null
     * @see AbstractBuild#getDependencyChanges(AbstractBuild)
     */
    private Map<AbstractProject, AbstractBuild.DependencyChange> getDependencyChanges(Run build, Run otherBuild) {

        if (otherBuild == null) {
            return Collections.emptyMap();
        }

        Fingerprinter.FingerprintAction currentBuildFingerprints = build.getAction(Fingerprinter.FingerprintAction.class);
        Fingerprinter.FingerprintAction previousBuildFingerprints = otherBuild.getAction(Fingerprinter.FingerprintAction.class);

        if (currentBuildFingerprints == null || previousBuildFingerprints == null) {
            return Collections.emptyMap();
        }

        Map<AbstractProject,Integer> currentBuildDependencies = currentBuildFingerprints.getDependencies(true);
        Map<AbstractProject,Integer> previousBuildDependencies = previousBuildFingerprints.getDependencies(true);

        Map<AbstractProject, AbstractBuild.DependencyChange> changes = new HashMap<>();

        for (Map.Entry<AbstractProject, Integer> entry : previousBuildDependencies.entrySet()) {
            AbstractProject dependencyProject = entry.getKey();
            Integer oldNumber = entry.getValue();
            Integer newNumber = currentBuildDependencies.get(dependencyProject);
            if (newNumber != null && oldNumber.compareTo(newNumber) < 0) {
                changes.put(dependencyProject, new AbstractBuild.DependencyChange(dependencyProject, oldNumber, newNumber));
            }
        }

        return changes;
    }
}
