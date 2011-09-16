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

l.layout(title: _("all.changes.title", my.project.name)) {
  st.include(page: "sidepanel.jelly", it: my.project)
  l.main_panel() {
    def from = buildNumber(request.getParameter('from'));
    def to = buildNumber(request.getParameter('to'));

    h1(_("All Changes"))
    def builds = Functions.filter(my.project.buildsAsMap, from, to).values()
    if (builds.empty) {
      text(_("No builds."))
    } else {
      showChanges(builds)
    }
  }
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

private showChanges(Collection<AbstractBuild> builds) {
  boolean hadChanges = false;
  for (AbstractBuild build in builds) {
    Multimap<ChangeLogSet.Entry, AbstractBuild> changes = my.getAllChanges(build);
    if (changes.empty) {
      continue
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
    ol() {
      for (entry in changes.keySet()) {
        li() {
          showEntry(entry, build, changes.get(entry))
        }
      }
    }
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
  def build = c.parent.build
  def browser = build.project.scm.effectiveBrowser
  raw(c.msgAnnotated)
  raw(" &#8212; ")
  if (browser?.getChangeSetLink(c)) {
    a(href: browser.getChangeSetLink(c), _("detail"))
  } else {
    a(href: "${build.absoluteUrl}changes", _("detail"))
  }
}

private def showDependencyChanges(DependencyChange dep) {
  a(href: "${rootURL}/${dep.project.url}") {text(dep.project.displayName)}
  st.nbsp()
  a(href: "${rootURL}/${dep.from.url}") {
    delegate.img(src: "${imagesURL}/16x16/${dep.from.buildStatusUrl}",
            alt: "${dep.from.iconColor.description}", height: "16", width: "16")
    text(dep.from.displayName)
  }

  raw("&#x2192;") // right arrow
  a(href: "${rootURL}/${dep.to.url}") {
    delegate.img(src: "${imagesURL}/16x16/${dep.to.buildStatusUrl}",
            alt: "${dep.to.iconColor.description}", height: "16", width: "16")
    text(dep.to.displayName)
  }
}
