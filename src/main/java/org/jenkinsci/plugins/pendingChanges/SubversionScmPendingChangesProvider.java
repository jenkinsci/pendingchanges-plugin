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

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;
import hudson.scm.SubversionSCM;
import hudson.util.LogTaskListener;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationProvider;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Retrieve revision logs from subversion
 *
 * @author avogler
 */
@Extension
public class SubversionScmPendingChangesProvider implements ScmPendingChangesProvider {
    private static final Logger logger = Logger.getLogger(SubversionScmPendingChangesProvider.class.getName());

    /**
     * Check whether this ScmPendingChangesProvider supports this SCM implementation
     * @param scm the SCM to check
     * @return true if this SCM is supported
     */
    public boolean supports(SCM scm) {
        return scm instanceof SubversionSCM;
    }

    /**
     * Retrieve all changes made since last successful build.
     *
     * @param project use SCM config of this project to retrieve changes
     * @return
     */
    public ChangeLogSet getPendingChanges(AbstractProject project) {
        SubversionSCM scm = (SubversionSCM) project.getScm();
        SVNClientManager clientManager = getSVNClientManager(project);
        List<SSCLPChangeLogSet.Entry> logEntries = new ArrayList<SSCLPChangeLogSet.Entry>();

        SubversionSCM.ModuleLocation[] locations = scm.getLocations();
        for(int i=0; i < locations.length; i++) {
            SVNRepository svnRepository;
            try {
                svnRepository = clientManager.createRepository(locations[i].getSVNURL(), true);
            } catch (SVNException e) {
                logger.warning("invalid subversion url '" + locations[i].getURL() + "' skipped");
                continue;
            }

            // determine start revision
            long startRevision;
            try {
                String revisionKey;

                if(locations.length==1) {
                    revisionKey = "SVN_REVISION";
                } else {
                    revisionKey = "SVN_REVISION_" + i;
                }

                AbstractBuild lastSuccessfulBuild = (AbstractBuild) project.getLastSuccessfulBuild();
                if(lastSuccessfulBuild!=null) {
                    startRevision = Long.parseLong(project.getLastSuccessfulBuild().getEnvironment(new LogTaskListener(logger, Level.INFO)).get(revisionKey));
                } else {
                    logger.log(Level.WARNING, "could not determine start revision, skipping location '" + locations[i].getURL() + "'");
                    continue;
                }

            } catch (Exception e) {
                logger.log(Level.WARNING, "could not determine start revision, skipping location '" + locations[i].getURL() + "'", e);
                continue;
            }

            retrieveSubversionLogEntries(logEntries, svnRepository, startRevision+1, -1);
        }
        return new SSCLPChangeLogSet(project, logEntries);
    }

    /**
     * load log entries and convert to internal data structure
     *
     * @param logEntries add log enries to this list
     * @param svnRepository subversion respository to query
     * @param startRevision start log with revision
     * @param endRevision end log with revision
     */
    private void retrieveSubversionLogEntries(List<SSCLPChangeLogSet.Entry> logEntries, SVNRepository svnRepository, long startRevision, long endRevision) {
        Collection<SVNLogEntry> svnLogEntries;
        try {
            //noinspection unchecked
            svnLogEntries = svnRepository.log(new String[]{""}, null, startRevision, endRevision, true, true);

            for(SVNLogEntry svnLogEntry : svnLogEntries) {
                SSCLPChangeLogSet.Entry entry = new SSCLPChangeLogSet.Entry();
                entry.setCommitId(Long.toString(svnLogEntry.getRevision()));
                entry.setMsg(svnLogEntry.getMessage());
                entry.setAuthor(User.get(svnLogEntry.getAuthor()));
                logEntries.add(entry);
            }
        } catch (SVNException e) {
            logger.log(Level.WARNING, "retrieving logs failed", e);
        }
    }

    private SVNClientManager getSVNClientManager(AbstractProject project) {
        SubversionSCM scm = (SubversionSCM) project.getScm();
        ISVNAuthenticationProvider authProvider = scm.getDescriptor().createAuthenticationProvider(project);
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager();
        authManager.setAuthenticationProvider(authProvider);
        return SVNClientManager.newInstance(null, authManager);
    }

    public static class SSCLPChangeLogSet extends ChangeLogSet<SSCLPChangeLogSet.Entry> {

        private List<Entry> entries;

        protected SSCLPChangeLogSet(AbstractProject project, List<Entry> entries) {
            super((AbstractBuild<?,?>) project.getLastSuccessfulBuild());
            this.entries = entries;
            for(Entry entry : entries) {
                entry.setParent(this);
            }
        }

        public boolean isEmptySet() {
            return entries.isEmpty();
        }

        public Iterator<Entry> iterator() {
            return entries.iterator();
        }

        List<Entry> getEntriesList()
        {
            return entries;
        }

        public static class Entry extends ChangeLogSet.Entry
        {
            private String commitId;
            private String msg;
            private User author;

            protected void setParent(ChangeLogSet parent) {
                super.setParent(parent);
            }

            public String getCommitId() {
                return commitId;
            }

            public void setCommitId(String commitId) {
                this.commitId = commitId;
            }

            public String getMsg() {
                return msg;
            }

            public void setMsg(String msg) {
                this.msg = msg;
            }

            public User getAuthor() {
                return author;
            }

            public void setAuthor(User author) {
                this.author = author;
            }

            public Collection<String> getAffectedPaths() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Collection<? extends AffectedFile> getAffectedFiles() {
                return super.getAffectedFiles();    //To change body of overridden methods use File | Settings | File Templates.
            }
        }
    }
}
