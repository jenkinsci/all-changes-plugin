package org.jenkinsci.plugins.all_changes;

import hudson.Extension;
import hudson.model.Action;
import jenkins.model.TransientActionFactory;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import java.util.Collection;
import java.util.Collections;

@Extension
public class AllChangesWorkflow extends TransientActionFactory<WorkflowJob> {

    @Override
    public Class<WorkflowJob> type() {
        return WorkflowJob.class;
    }

    @Override
    public Collection<? extends Action> createFor(WorkflowJob target) {
        return Collections.singleton(new AllChangesWorkflowAction(target));
    }
}
