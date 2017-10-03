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

import hudson.FilePath;
import hudson.model.*;
import hudson.tasks.Fingerprinter;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.CreateFileBuilder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.*;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        Fingerprinter.FingerprintAction.class
})
// needed to prevent PowerMock from changing the crypto things
// (see "Troubleshooting" in https://wiki.jenkins.io/display/JENKINS/Mocking+in+Unit+Tests)
@PowerMockIgnore({"javax.crypto.*" })
public class DependencyChangesAggregatorTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private BuildListener testBuildListener;
    private ByteArrayOutputStream out;

    @Before
    public void setup() {
        out = new ByteArrayOutputStream();
        testBuildListener = mock(BuildListener.class);
        when(testBuildListener.getLogger()).thenReturn(new PrintStream(out));
    }

    @Test
    public void returnsAllChangedDependencyBuilds() throws Exception {

        FreeStyleProject proj1 = jenkins.createFreeStyleProject("proj1");
        FreeStyleProject proj2 = jenkins.createFreeStyleProject("proj2");
        FreeStyleProject proj3 = jenkins.createFreeStyleProject("proj3");
        FreeStyleProject proj4 = jenkins.createFreeStyleProject("proj4");

        jenkins.buildAndAssertSuccess(proj1);
        createFileWithRandomString(proj1.getLastBuild(), "file1");
        fingerprintFile(proj1.getLastBuild(), proj1.getLastBuild().getWorkspace(), "file1");

        jenkins.buildAndAssertSuccess(proj2);
        createFileWithRandomString(proj2.getLastBuild(), "file2");
        fingerprintFile(proj2.getLastBuild(), proj2.getLastBuild().getWorkspace(), "file2");

        jenkins.buildAndAssertSuccess(proj3);
        createFileWithRandomString(proj3.getBuildByNumber(1), "file3");
        fingerprintFile(proj3.getLastBuild(), proj3.getLastBuild().getWorkspace(), "file3");

        jenkins.buildAndAssertSuccess(proj4);
        fingerprintFile(proj4.getLastBuild(), proj1.getBuildByNumber(1).getWorkspace(), "file1");
        fingerprintFile(proj4.getLastBuild(), proj2.getBuildByNumber(1).getWorkspace(), "file2");
        fingerprintFile(proj4.getLastBuild(), proj3.getBuildByNumber(1).getWorkspace(), "file3");

        jenkins.buildAndAssertSuccess(proj1);
        createFileWithRandomString(proj1.getLastBuild(), "file1");
        fingerprintFile(proj1.getLastBuild(), proj1.getLastBuild().getWorkspace(), "file1");

        jenkins.buildAndAssertSuccess(proj1);
        createFileWithRandomString(proj1.getLastBuild(), "file1");
        fingerprintFile(proj1.getLastBuild(), proj1.getLastBuild().getWorkspace(), "file1");

        jenkins.buildAndAssertSuccess(proj2);
        createFileWithRandomString(proj2.getLastBuild(), "file2");
        fingerprintFile(proj2.getLastBuild(), proj2.getLastBuild().getWorkspace(), "file2");

        jenkins.buildAndAssertSuccess(proj4);
        fingerprintFile(proj4.getLastBuild(), proj1.getBuildByNumber(2).getWorkspace(), "file1");
        fingerprintFile(proj4.getLastBuild(), proj1.getBuildByNumber(3).getWorkspace(), "file1");
        fingerprintFile(proj4.getLastBuild(), proj2.getBuildByNumber(2).getWorkspace(), "file2");

        Collection<Run> res = (new DependencyChangesAggregator()).aggregateBuildsWithChanges(proj4.getBuildByNumber(2));

        assertThat(res, Matchers.<Run>containsInAnyOrder(
                proj1.getBuildByNumber(2),
                proj1.getBuildByNumber(3),
                proj2.getBuildByNumber(2)));
    }

    @Test
    @WithoutJenkins
    public void returnsEmptyCollectionIfBuildHasNoPreviousBuild() {

        Run build = mock(Run.class);
        when(build.getPreviousBuild()).thenReturn(null);

        Collection<Run> res = new DependencyChangesAggregator().aggregateBuildsWithChanges(build);

        assertThat(res, Matchers.<Run>empty());
    }

    @Test
    @WithoutJenkins
    public void returnsEmptyCollectionIfBuildHasNoFingerprintAction() {

        Run build1 = mock(Run.class);
        when(build1.getAction(Fingerprinter.FingerprintAction.class)).thenReturn(mock(Fingerprinter.FingerprintAction.class));

        Run build2 = mock(Run.class);
        when(build2.getPreviousBuild()).thenReturn(build1);
        when(build2.getAction(Fingerprinter.FingerprintAction.class)).thenReturn(null);

        Collection<Run> res = new DependencyChangesAggregator().aggregateBuildsWithChanges(build2);

        assertThat(res, Matchers.<Run>empty());
    }

    @Test
    @WithoutJenkins
    public void returnsEmptyCollectionIfPreviousBuildHasNoFingerprintAction() {

        Run build1 = mock(Run.class);
        when(build1.getAction(Fingerprinter.FingerprintAction.class)).thenReturn(null);

        Run build2 = mock(Run.class);
        when(build2.getPreviousBuild()).thenReturn(build1);
        when(build2.getAction(Fingerprinter.FingerprintAction.class)).thenReturn(mock(Fingerprinter.FingerprintAction.class));

        Collection<Run> res = new DependencyChangesAggregator().aggregateBuildsWithChanges(build2);

        assertThat(res, Matchers.<Run>empty());
    }

    private void createFileWithRandomString(AbstractBuild build, String filename) throws IOException, InterruptedException {
        CreateFileBuilder fileBuilder = new CreateFileBuilder(filename, UUID.randomUUID().toString());
        fileBuilder.perform(build, null, testBuildListener);
    }

    private void fingerprintFile(Run build, FilePath directory, String filename) throws InterruptedException {
        Fingerprinter fingerprinter1 = new Fingerprinter(filename);
        fingerprinter1.perform(build, directory, null, testBuildListener);
    }
}
