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

package org.jenkinsci.plugins.all_changes.AllChangesAction

import com.google.common.collect.Multimap
import hudson.Functions
import hudson.model.AbstractBuild
import hudson.model.AbstractBuild.DependencyChange
import hudson.scm.ChangeLogSet
import java.text.DateFormat
import org.jvnet.localizer.LocaleProvider

f = namespace(lib.FormTagLib)
l = namespace(lib.LayoutTagLib)
t = namespace("/lib/hudson")
st = namespace("jelly:stapler")

l.main_panel() {
  def builds = Functions.filter(my.project.buildsAsMap, null, null).values()
  if (builds.empty) {
    text(_("No builds."))
  } else {
    showChanges(builds)
  }
}

private showChanges(Collection<AbstractBuild> builds) {
  def changedBuildCount = 1;
  boolean hadChanges = false;
  for (AbstractBuild build in builds) {
    Multimap<ChangeLogSet.Entry, AbstractBuild> changes = my.getAllChanges(build);
    if (changes.empty) {
      continue
    }
    if(changedBuildCount > my.numChanges && my.numChanges != 0)
    {
      break
    }
    hadChanges = true
    h2() {
      a(target: "_blank", href: "${my.project.absoluteUrl}/${build.number}/changes",
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

private def showEntry(entry, AbstractBuild build, Collection<AbstractBuild> builds) {
  showChangeSet(entry)
  boolean firstDrawn = false
  for (AbstractBuild b in builds) {
    if (b != build) {
      if (!firstDrawn) {
        text(" (")
        firstDrawn = true
      }
      else {
        text(", ")
      }
      a(target: "_blank", href: "${rootURL}/${b.project.url}") {text(b.project.displayName)}
      st.nbsp()
      a(target: "_blank", href: "${rootURL}/${b.url}") {
        text(b.displayName)
      }
    }
  }
  if (firstDrawn) {
    text(")")
  }
}

private def showChangeSet(ChangeLogSet.Entry c) {
  def build = c.parent.build
  def browser = build.project.scm.effectiveBrowser
  raw(c.getCommitId())
  raw(" &#187; ")
  raw(c.msgAnnotated)
  raw(" &#8212; ")
  if (browser?.getChangeSetLink(c)) {
    a(target: "_blank", href: browser.getChangeSetLink(c), _("detail"))
  } else {
    a(target: "_blank", href: "${build.absoluteUrl}changes", _("detail"))
  }
}

private def showDependencyChanges(DependencyChange dep) {
  a(target: "_blank", href: "${rootURL}/${dep.project.url}") {text(dep.project.displayName)}
  st.nbsp()
  a(target: "_blank", href: "${rootURL}/${dep.from.url}") {
    delegate.img(src: "${imagesURL}/16x16/${dep.from.buildStatusUrl}",
            alt: "${dep.from.iconColor.description}", height: "16", width: "16")
    text(dep.from.displayName)
  }

  raw("&#x2192;") // right arrow
  a(target: "_blank", href: "${rootURL}/${dep.to.url}") {
    delegate.img(src: "${imagesURL}/16x16/${dep.to.buildStatusUrl}",
            alt: "${dep.to.iconColor.description}", height: "16", width: "16")
    text(dep.to.displayName)
  }
}
