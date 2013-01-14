/*
 * The MIT License
 *
 * Copyright (c) 2013, Andreas Vogler
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

package org.jenkinsci.plugins.pendingChanges;

import hudson.model.AbstractItem;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;
import jenkins.model.Jenkins;

import java.util.logging.Logger;

/**
 *
 * Check for pending SCM changes since last successful build
 *
 * @author avogler
 */
public class PendingChangesAction implements Action
{
    private static final Logger logger = Logger.getLogger(PendingChangesAction.class.getName());

    private AbstractProject project;
    private ScmPendingChangesProvider scmPendingChangesProvider;

    public PendingChangesAction(AbstractProject target) {
        this.project = target;

        SCM scm = project.getScm();
        if(scm!=null) {
           for(ScmPendingChangesProvider logProvider : Jenkins.getInstance().getExtensionList(ScmPendingChangesProvider.class)) {
               if(logProvider.supports(scm)) {
                   this.scmPendingChangesProvider = logProvider;
               }
           }
        }

    }

    public String getIconFileName() {
        return project.hasPermission(AbstractProject.CONFIGURE) ? "clipboard.png" : null;
    }

    public String getDisplayName() {
        return Messages.displayName();
    }

    public String getUrlName() {
        return "pendingChanges";
    }

    public final AbstractItem getProject() {
        return project;
    }

    public final Iterable<ChangeLogSet.Entry> getPendingChanges() {

        if(scmPendingChangesProvider ==null) {
            logger.warning("no log provider found");
            return null;
        }

        //noinspection unchecked
        return scmPendingChangesProvider.getPendingChanges(project);
    }
}
