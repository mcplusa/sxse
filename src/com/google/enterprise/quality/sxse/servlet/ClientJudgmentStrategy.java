// Copyright 2009 Google Inc.
// 
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
//      http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.enterprise.quality.sxse.servlet;

import com.google.common.collect.Lists;
import com.google.enterprise.quality.sxse.JudgmentDetails;
import com.google.enterprise.quality.sxse.QueryFormatter;
import com.google.enterprise.quality.sxse.ScoringPolicyProfile;
import com.google.enterprise.quality.sxse.JudgmentDetails.Judgment;
import com.google.enterprise.quality.sxse.QueryFormatter.QueryOptions;
import com.google.enterprise.quality.sxse.gxp.JudgeClientResults;
import com.google.enterprise.quality.sxse.gxp.JudgeHidden;
import com.google.enterprise.quality.sxse.gxp.Judge;
import com.google.enterprise.quality.sxse.storage.JudgmentStorage;
import com.google.enterprise.quality.sxse.storage.PreferencesStorage;
import com.google.enterprise.quality.sxse.storage.StorageManager;
import com.google.enterprise.quality.sxse.storage.SxseStorageException;
import com.google.gxp.base.GxpClosure;
import com.google.gxp.base.GxpContext;
import com.google.gxp.html.HtmlClosure;
import com.google.gxp.html.HtmlClosures;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A {@link JudgmentServletStrategy} implementation that, given a query, creates
 * appropriate URLs from each scoring policy profile such that the results from
 * each can be displayed in iframes in the assessor's web browser. When a
 * judgment is made, the results judged are not stored to disk. This approach
 * may not be ideal if the results for the query vary over time, but they allow
 * the results displayed to reflect the access privileges of the assessor, and
 * not the host running SxSE.
 */
final class ClientJudgmentStrategy extends AbstractJudgmentStrategy {
  private static final Logger LOGGER = Logger.getLogger(
      ClientJudgmentStrategy.class.getName());

  /**
   * Creates a new servlet through which an assessor makes judgments.
   * 
   * @param banner the banner to display across the top of the page
   * @param storageManager the storage from which this servlet can store
   *        judgments
   * @param queryChooser the chooser for the queries
   */
  public ClientJudgmentStrategy(Banner banner,
      StorageManager storageManager, QueryChooser queryChooser) {
    super(banner, storageManager, queryChooser);
  }

  /**
   * Returns the URI for the iframe containing results for the given scoring
   * policy profile, or {@code null} if no results can be displayed.
   * 
   * @param profile the scoring policy profile returning the results
   * @param query the query
   * @param maxResults the maximum number of results to display
   * @return the frame URI
   */
  private String getFrameUri(ScoringPolicyProfile profile, String query,
      int maxResults) {
    if (profile == ScoringPolicyProfile.EMPTY_PROFILE) {
      // No profile was set in preferences, so display a blank page.
      return "about:blank";
    } else if (query != null) {
      // Profile and query specified, so construct the query URL.
      QueryFormatter queryFormatter = profile.getQueryFormatter();
      URI url = queryFormatter.createQueryUri(new QueryOptions(query, maxResults));
      return url.toString();
    }
    return null;
  }

  protected void writeResults(HttpServletRequest req, HttpServletResponse res,
      String query) throws SxseStorageException, IOException {
    JudgmentStorage judgmentStorage = storageManager.getJudgmentStorage();
    PreferencesStorage prefsStorage = storageManager.getPreferencesStorage();
    ScoringPolicyProfile firstProfile = prefsStorage.getFirstProfile();
    ScoringPolicyProfile secondProfile = prefsStorage.getSecondProfile();

    Judgment lastJudgment = null;
    SideBySideResults results = null;
    if (allowJudgment(firstProfile, secondProfile, query)) {
      if (shouldSwap(judgmentStorage, query)) {
        // Swap the profiles so they are displayed in reverse order.
        ScoringPolicyProfile temp = firstProfile;
        firstProfile = secondProfile;
        secondProfile = temp;
      }

      // Get the previous judgment for this query if one exists.
      lastJudgment = getLastJudgment(User.getUser(req).getAssessorName(),
          query, firstProfile, secondProfile);
      results = new SideBySideResults(query, firstProfile, secondProfile,
          null, null);
      User.setSideBySideResults(req, results);
    } else {
      User.setSideBySideResults(req, null);
    }

    int maxResults = judgmentStorage.getMaxResults();
    String firstFrameUri = getFrameUri(firstProfile, query, maxResults);
    String secondFrameUri = getFrameUri(secondProfile, query, maxResults);

    // Get the query set names to display, if allowed.
    Set<String> querySetNames = getDisplayedQuerySetNames();
    ServletDetails servletDetails = new ServletDetails(results,
        firstFrameUri, secondFrameUri);

    // Display results for judgment.
    PreparePage.write(res);
    Writer writer = res.getWriter();
    Judge.write(writer, new GxpContext(req.getLocale()),
        User.getUser(req), banner, query, lastJudgment, servletDetails,
        querySetNames, null);
  }

  private static class ServletDetails implements JudgmentServletDetails {
    private final String judgmentUuid;
    private final String firstFrameUri;
    private final String secondFrameUri;

    private ServletDetails(SideBySideResults results,
        String firstFrameUri, String secondFrameUri) {
      this.judgmentUuid = (results == null) ?
          null : results.getUuid().toString();
      this.firstFrameUri = firstFrameUri;
      this.secondFrameUri = secondFrameUri;
    }

    public boolean allowJudgment() {
      return (judgmentUuid != null); 
    }

    public HtmlClosure writeHeader() {
      return JudgeHidden.getGxpClosure(judgmentUuid);
    }

    public HtmlClosure writeFooter() {
      return HtmlClosures.EMPTY;
    }

    public HtmlClosure writeResults() {
      return JudgeClientResults.getGxpClosure(allowJudgment(),
          firstFrameUri, secondFrameUri);
    }
  }
}
