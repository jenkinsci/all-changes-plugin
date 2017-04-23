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

import com.google.common.collect.Multimap
import hudson.Functions
import hudson.scm.ChangeLogSet
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jvnet.localizer.LocaleProvider

import java.text.DateFormat

f = namespace(lib.FormTagLib)
l = namespace(lib.LayoutTagLib)
t = namespace("/lib/hudson")
st = namespace("jelly:stapler")


def from = buildNumber(request.getParameter('from'));
def to = buildNumber(request.getParameter('to'));

def builds = Functions.filter(my.project.buildsAsMap, from, to).values()
if (builds.empty) {
    text(_("No builds."))
} else {
    showChanges(builds)
}


private buildNumber(String build) {
    if (build?.isInteger()) {
        return build
    } else {
        def permaLink = my.project.getPermalinks().get(build)
        def run = permaLink?.resolve(my.project)
        return run?.number?.toString()
    }
}

private showChanges(Collection<WorkflowRun> builds) {
    def changedBuildCount = 1;
    boolean hadChanges = false;
    for (WorkflowRun build in builds) {
        Multimap<ChangeLogSet.Entry, WorkflowRun> changes = my.getAllChanges(build);
        if (changes.empty) {
            continue
        }
        if(changedBuildCount > my.numChanges && my.numChanges != 0)
        {
            break
        }
        hadChanges = true
        h2() {
            a(href: "${my.project.absoluteUrl}/${build.number}/changes",
                    """${build.displayName}  (${
                        DateFormat.getDateTimeInstance(
                                DateFormat.MEDIUM,
                                DateFormat.MEDIUM,
                                LocaleProvider.locale).format(build.timestamp.time)})""")
        }
        ul() {
            for (entry in changes.keySet()) {
                li() {
                    showEntry(entry, build, changes.get(entry))
                }
            }
        }
        changedBuildCount++;
    }
    if (!hadChanges) {
        text(_("No changes in any of the builds."))
    }
}

private def showEntry(entry, WorkflowRun build, Collection<WorkflowRun> builds) {
    showChangeSet(entry)
    boolean firstDrawn = false
    for (WorkflowRun b in builds) {
        if (b != build) {
            if (!firstDrawn) {
                text(" (")
                firstDrawn = true
            }
            else {
                text(", ")
            }
            a(href: "${rootURL}/${b.project.url}") {text(b.project.displayName)}
            st.nbsp()
            a(href: "${rootURL}/${b.url}") {
                text(b.displayName)
            }
        }
    }
    if (firstDrawn) {
        text(")")
    }
}

private def showChangeSet(ChangeLogSet.Entry c) {
    def build = c.parent.run
    def browser = c.parent.browser
    raw(c.getCommitId())
    raw(" &#187; ")
    raw(c.msgAnnotated)
    raw(" &#8212; ")
    if (browser?.getChangeSetLink(c)) {
        a(href: browser.getChangeSetLink(c), _("detail"))
    } else {
        a(href: "${build.absoluteUrl}changes", _("detail"))
    }
}