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

import hudson.ExtensionPoint;
import hudson.model.AbstractProject;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;

/**
 * Provide pluggable access to the change logs of a SCM.
 *
 * @author avogler
 */
public interface ScmPendingChangesProvider extends ExtensionPoint {

    /**
     * Check whether this ScmPendingChangesProvider supports this SCM implementation
     * @param scm the SCM to check
     * @return true if this SCM is supported
     */
    boolean supports(SCM scm);

    /**
     * Retrieve all changes made since last successful build.
     *
     * @param project use SCM config of this project to retrieve changes
     * @return
     */
    ChangeLogSet getPendingChanges(AbstractProject project);
}
