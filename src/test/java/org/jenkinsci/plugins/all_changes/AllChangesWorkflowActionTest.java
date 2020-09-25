/*
 * The MIT License
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
import com.google.common.collect.ImmutableSet;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        WorkflowRun.class
})
public class AllChangesWorkflowActionTest {

    @Test
    public void getContributingBuildsShouldWorkTransitively() throws Exception {

        DependencyChangesAggregator aggregatorMock = mock(DependencyChangesAggregator.class);
        WorkflowRun build = PowerMockito.mock(WorkflowRun.class);
        AbstractBuild build2 = mock(AbstractBuild.class);
        AbstractBuild build3 = mock(AbstractBuild.class);
        when(aggregatorMock.aggregateBuildsWithChanges(build)).thenReturn(ImmutableList.<Run>of(build2));
        when(aggregatorMock.aggregateBuildsWithChanges(build2)).thenReturn(ImmutableList.<Run>of(build3));

        AllChangesWorkflowAction changesAction = new AllChangesWorkflowAction(null);
        changesAction.setAggregator(aggregatorMock);

        Set<Run> foundBuilds = changesAction.getContributingBuilds(build);

        assertTrue(foundBuilds.equals(ImmutableSet.of(build, build2, build3)));
    }

    @Test
    public void getContributingBuildsShouldWorkHandleCycles() throws Exception {

        DependencyChangesAggregator aggregatorMock = mock(DependencyChangesAggregator.class);
        WorkflowRun build = PowerMockito.mock(WorkflowRun.class);
        AbstractBuild build2 = mock(AbstractBuild.class);
        AbstractBuild build3 = mock(AbstractBuild.class);
        when(aggregatorMock.aggregateBuildsWithChanges(build)).thenReturn(ImmutableList.<Run>of(build2));
        when(aggregatorMock.aggregateBuildsWithChanges(build2)).thenReturn(ImmutableList.<Run>of(build3));
        when(aggregatorMock.aggregateBuildsWithChanges(build3)).thenReturn(ImmutableList.<Run>of(build));

        AllChangesWorkflowAction changesAction = new AllChangesWorkflowAction(null);
        changesAction.setAggregator(aggregatorMock);

        Set<Run> foundBuilds = changesAction.getContributingBuilds(build);

        assertTrue(foundBuilds.equals(ImmutableSet.of(build, build2, build3)));
    }
}
