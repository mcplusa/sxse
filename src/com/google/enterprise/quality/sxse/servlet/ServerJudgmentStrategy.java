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

import com.google.enterprise.quality.sxse.QueryFormatter;
import com.google.enterprise.quality.sxse.ScoringPolicyProfile;
import com.google.enterprise.quality.sxse.SearchResult;
import com.google.enterprise.quality.sxse.JudgmentDetails.Judgment;
import com.google.enterprise.quality.sxse.QueryFormatter.QueryOptions;
import com.google.enterprise.quality.sxse.gxp.Judge;
import com.google.enterprise.quality.sxse.gxp.JudgeHidden;
import com.google.enterprise.quality.sxse.gxp.JudgeServerResults;
import com.google.enterprise.quality.sxse.storage.JudgmentStorage;
import com.google.enterprise.quality.sxse.storage.PreferencesStorage;
import com.google.enterprise.quality.sxse.storage.StorageManager;
import com.google.enterprise.quality.sxse.storage.SxseStorageException;
import com.google.gxp.base.GxpContext;
import com.google.gxp.html.HtmlClosure;
import com.google.gxp.html.HtmlClosures;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A {@link JudgmentServletStrategy} implementation that retrieves the results
 * in XML format from the hosts specified in the profiles, and saves a copy of
 * the results to disk when the judgment is made. This allows the administrator
 * to recall the exact results judged, even if they vary over time.
 */
public final class ServerJudgmentStrategy extends AbstractJudgmentStrategy {
  private static final Logger LOGGER = Logger.getLogger(
      ServerJudgmentStrategy.class.getName());

  /*
   * A thread pool for collecting results from both scoring policy profiles in
   * parallel.
   */
  private static final ExecutorService threadPool =
      Executors.newCachedThreadPool();

  /**
   * Creates a new servlet through which an assessor makes judgments.
   * 
   * @param banner the banner to display across the top of the page
   * @param storageManager the storage from which this servlet can store
   *        judgments
   * @param queryChooser the chooser for the queries
   */
  public ServerJudgmentStrategy(Banner banner,
      StorageManager storageManager, QueryChooser queryChooser) {
    super(banner, storageManager, queryChooser);
  }

  protected void writeResults(HttpServletRequest req, HttpServletResponse res,
      String query) throws SxseStorageException, IOException {
    JudgmentStorage judgmentStorage = storageManager.getJudgmentStorage();
    PreferencesStorage prefsStorage = storageManager.getPreferencesStorage();
    ScoringPolicyProfile firstProfile = prefsStorage.getFirstProfile();
    ScoringPolicyProfile secondProfile = prefsStorage.getSecondProfile();

    Judgment lastJudgment = null;
    List<SearchResult> firstResults = null;
    List<SearchResult> secondResults = null;
    String nextAction = null;

    SideBySideResults results = null;
    if (allowJudgment(firstProfile, secondProfile, query)) {
      if (shouldSwap(judgmentStorage, query)) {
        // Swap the profiles so they are displayed in reverse order.
        ScoringPolicyProfile temp = firstProfile;
        firstProfile = secondProfile;
        secondProfile = temp;
      }

      // Submit tasks to retrieve results to the thread pool.
      int maxResults = judgmentStorage.getMaxResults();
      long abortTime = System.currentTimeMillis() +
          judgmentStorage.getResultRetrievalTimeout();
      Future<List<SearchResult>> firstResultsFuture =
          issueQuery(firstProfile, query, maxResults);
      Future<List<SearchResult>> secondResultsFuture =
          issueQuery(secondProfile, query, maxResults);

      // Get the previous judgment for this query if one exists.
      lastJudgment = getLastJudgment(User.getUser(req).getAssessorName(),
          query, firstProfile, secondProfile);

      // Get the results.
      firstResults = getSearchResults(firstResultsFuture, abortTime);
      secondResults = getSearchResults(secondResultsFuture, abortTime);

      if (allowJudgment(firstResults, secondResults)) {
        results = new SideBySideResults(query,
            firstProfile, secondProfile, firstResults, secondResults);
        User.setSideBySideResults(req, results);
        // Automatically submit judgment if results are equal.
        if (judgmentStorage.isSubmittingAutomatically() &&
            firstResults.equals(secondResults)) {
          nextAction = quoteAction(JudgmentAction.RATE_EQUAL);
        }
      } else {
        // Have invalid results, so we cannot judge this query.
        User.setSideBySideResults(req, null);
        // Automatically pass on judgment since invalid.
        if (judgmentStorage.isSubmittingAutomatically()) {
          nextAction = quoteAction(JudgmentAction.PASS);
        }
      }
    } else {
      // Both profiles are empty, so we cannot judge this query.
      User.setSideBySideResults(req, null);
      // Automatically pass on judgment since cannot judge.
      if (judgmentStorage.isSubmittingAutomatically()) {
        nextAction = quoteAction(JudgmentAction.PASS);
      }
    }

    // Get the query set names to display, if allowed.
    Set<String> querySetNames = getDisplayedQuerySetNames();
    ServletDetails servletDetails = new ServletDetails(results,
        firstResults, secondResults);

    // Display results for judgment.
    PreparePage.write(res);
    Writer writer = res.getWriter();
    
    Judge.write(writer, new GxpContext(req.getLocale()),
        User.getUser(req), banner, query, lastJudgment, servletDetails,
        querySetNames, nextAction);
  }

  private Future<List<SearchResult>> issueQuery(
      ScoringPolicyProfile profile, String query, int maxResults) {
    if (profile == ScoringPolicyProfile.EMPTY_PROFILE) {
      // Do not issue query against the blank profile.
      return null;
    }
    // Issue a query against the given scoring policy profile.
    return threadPool.submit(new ResultListFuture(query, profile, maxResults));
  }

  private List<SearchResult> getSearchResults(
      Future<List<SearchResult>> resultsFuture, long abortTime) {
    if (resultsFuture == null) {
      // Corresponding profile is the empty profile and returns no results.
      return Collections.emptyList();
    }

    long timeRemaining = abortTime - System.currentTimeMillis();
    if (timeRemaining < 0L) {
      // Will throw TimeoutException if results are not already available.
      timeRemaining = 0L;
    }

    try {
      return resultsFuture.get(timeRemaining, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      LOGGER.log(Level.SEVERE, "getResults caught InterruptedException", e);
    } catch (ExecutionException e) {
      LOGGER.log(Level.SEVERE, "getResults caught ExecutionException", e);
    } catch (TimeoutException e) {
      // Interrupt if the thread if necessary; the GSA can handle it.
      resultsFuture.cancel(true);
    }

    // Exception was thrown and so could not retrieve results.
    return null;
  }

  // Maximum length for URLs in displayed results.
  private static final int MAX_URL_LENGTH = 65;

  /**
   * Trims an URL so that it fits when displayed in a side-by-side fashion.
   * 
   * @param url the URL to trim
   * @return a trimmed URL
   */
  public static String trimUrl(String url) {
    if (url.length() > MAX_URL_LENGTH) {
      // URL is too long, trim and append ellipsis.
      StringBuilder sb = new StringBuilder(MAX_URL_LENGTH);
      sb.append(url, 0, MAX_URL_LENGTH - 3);
      sb.append("...");
      return sb.toString();
    }
    return url;
  }

  /*
   * Class implementing Callable, allowing results to be retrieved.
   */
  private static class ResultListFuture
      implements Callable<List<SearchResult>> {
    private final String query;
    private final ScoringPolicyProfile profile;
    private final int maxResults;

    public ResultListFuture(
        String query, ScoringPolicyProfile profile, int numResults) {
      this.query = query;
      this.profile = profile;
      this.maxResults = numResults;
    }

    public List<SearchResult> call() throws Exception {
      QueryFormatter queryFormatter = profile.getQueryFormatter();
      return queryFormatter.getSearchResults(
          new QueryOptions(query, maxResults));
    }
  }

  private static class ServletDetails implements JudgmentServletDetails {
    private final String judgmentUuid;
    private final List<SearchResult> firstResults;
    private final List<SearchResult> secondResults;

    private ServletDetails(SideBySideResults results,
        List<SearchResult> firstResults, List<SearchResult> secondResults) {
      this.judgmentUuid = (results == null) ?
          null : results.getUuid().toString();
      this.firstResults = firstResults;
      this.secondResults = secondResults;
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
      return JudgeServerResults.getGxpClosure(allowJudgment(),
          firstResults, secondResults);
    }
  }
}
