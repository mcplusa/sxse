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

import com.google.common.collect.Iterables;
import com.google.enterprise.quality.sxse.JudgmentDetails;
import com.google.enterprise.quality.sxse.ScoringPolicyProfile;
import com.google.enterprise.quality.sxse.SearchResult;
import com.google.enterprise.quality.sxse.JudgmentDetails.Judgment;
import com.google.enterprise.quality.sxse.input.EnumInputParser;
import com.google.enterprise.quality.sxse.input.StringInputParser;
import com.google.enterprise.quality.sxse.storage.JudgmentStorage;
import com.google.enterprise.quality.sxse.storage.QueryStorage;
import com.google.enterprise.quality.sxse.storage.StorageManager;
import com.google.enterprise.quality.sxse.storage.SxseStorageException;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Skeletal implementation of {@link JudgmentServletStrategy}. This class
 * largely contains a collection of methods that concrete subclasses should
 * need, as well as most of the logic for handling GET requests.
 */
abstract class AbstractJudgmentStrategy implements JudgmentServletStrategy {
  protected final Banner banner;
  protected final StorageManager storageManager;
  protected final QueryChooser queryChooser;

  /**
   * Creates a new servlet through which an assessor makes judgments.
   * 
   * @param banner the banner to display across the top of the page
   * @param storageManager the storage from which this servlet can store
   *        judgments
   * @param queryChooser the chooser for the queries
   */
  public AbstractJudgmentStrategy(Banner banner,
      StorageManager storageManager, QueryChooser queryChooser) {
    this.banner = banner;
    this.storageManager = storageManager;
    this.queryChooser = queryChooser;
  }

  /**
   * The action the assessor selected.
   */
  public static enum JudgmentAction {
    /**
     * The assessor requested the results for a specific query.
     */
    COMPARE_QUERY,

    /**
     * The assessor requested the results for a random query from a specific
     * query set.
     */
    COMPARE_QUERY_SET,

    /**
     * The assessor requested to view his query history.
     */
    SHOW_HISTORY,

    /**
     * The assessor passed on rating the results.
     */
    PASS,

    /**
     * The assessor rated the results from policy A better.
     */
    RATE_A,

    /**
     * The assessor rated the results from both policies equal.
     */
    RATE_EQUAL,

    /**
     * The assessor rated the results from policy B better.
     */
    RATE_B,
  }

  public static class CommonKeys {
    /**
     * The action that the assessor would like to perform.
     */
    public static final String ACTION = "action";

    /**
     * The query to use for the next judgment if {@link #action} is equal to
     * {@code COMPARE_QUERY}.
     */
    public static final String NEXT_QUERY = "nextQuery";

    /**
     * The query set to draw the next query from for judgment if {@link #action}
     * is equal to {@code COMPARE_QUERY_SET}.
     */
    public static final String NEXT_QUERY_SET = "nextQuerySet";
  }

  public static final class PostKeys extends CommonKeys {
    /**
     * The UUID of the side-by-side results.
     */
    public static final String JUDGMENT_ID = "judgmentUuid";
  }

  protected void saveJudgment(HttpServletRequest req, Judgment judgment)
      throws SxseStorageException {
    // Abort if the UUID from user is not for the last results shown.
    SideBySideResults results = User.getSideBySideResults(req);
    if (results == null) {
      return;
    }

    StringInputParser.ParsedInput judgmentUuidInput = StringInputParser.allowAll().parse(
        PostKeys.JUDGMENT_ID, req.getParameterMap());
    String judgmentUuid = judgmentUuidInput.getResult();
    if ((judgmentUuid == null) ||
        !UUID.fromString(judgmentUuid).equals(results.getUuid())) {
      return;
    }

    String assessor = User.getUser(req).getAssessorName();
    // Create the details of the judgment.
    long timestamp = System.currentTimeMillis();
    // Create the search results judged. 
    JudgmentDetails judgmentDetails = new JudgmentDetails(
        results.getQuery(), judgment, timestamp,
        results.getFirstQueryFormatter(), results.getSecondQueryFormatter(),
        null); 

    JudgmentStorage judgmentStorage = storageManager.getJudgmentStorage();
    judgmentStorage.addJudgment(assessor, judgmentDetails,
        results.getFirstResults(), results.getSecondResults());
  }

  public void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    EnumInputParser.ParsedInput<JudgmentAction> actionInput =
      new EnumInputParser<JudgmentAction>(JudgmentAction.class).parse(
        CommonKeys.ACTION, req.getParameterMap());
    JudgmentAction action = actionInput.getResult();

    try {
      // If a query was rated, save judgment.
      switch (action) {
      case RATE_A:
        saveJudgment(req, Judgment.FIRST_BETTER);
        writeResults(req, res, chooseQuery(req));
        return;
      case RATE_B:
        saveJudgment(req, Judgment.SECOND_BETTER);
        writeResults(req, res, chooseQuery(req));
        return;
      case RATE_EQUAL:
        saveJudgment(req, Judgment.EQUAL);
        writeResults(req, res, chooseQuery(req));
        return;
      default:
        // It's okay to pass through.
      }
    } catch (SxseStorageException e) {
      throw new ServletException(e);
    }
    
    // Handle action common to both GET and POST.
    handleCommonAction(req, res, action);
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    EnumInputParser.ParsedInput<JudgmentAction> actionInput =
        new EnumInputParser<JudgmentAction>(JudgmentAction.class).parse(
          CommonKeys.ACTION, req.getParameterMap());

    // Handle action common to both GET and POST.
    handleCommonAction(req, res, actionInput.getResult());
  }

  /*
   * Handles action common to both GET and POST. Must not produce side-effects.
   */
  protected void handleCommonAction(
      HttpServletRequest req, HttpServletResponse res, JudgmentAction action)
      throws ServletException, IOException {
    if (action == JudgmentAction.SHOW_HISTORY) {
      res.sendRedirect(HistoryServlet.PATH);
      return;
    }

    try {
      String query = null;
      if (action == JudgmentAction.COMPARE_QUERY) {
        StringInputParser.ParsedInput queryInput = StringInputParser.allowAll().parse(
            CommonKeys.NEXT_QUERY, req.getParameterMap());
        query = queryInput.getResult();
      } else if (action == JudgmentAction.COMPARE_QUERY_SET) {
        StringInputParser.ParsedInput querySetInput = StringInputParser.allowAll().parse(
            CommonKeys.NEXT_QUERY_SET, req.getParameterMap());
        String querySet = querySetInput.getResult();

        if (querySet != null) {
          query = chooseQuery(req, querySet);
        }
      }

      if (query == null) {
        // Must ensure query is not null.
        query = chooseQuery(req);
      }
      writeResults(req, res, query);
    } catch (SxseStorageException e) {
      throw new ServletException(e);
    }
  }

  protected String chooseQuery(HttpServletRequest req)
      throws SxseStorageException {
    return queryChooser.choose(storageManager, User.getUser(req));
  }

  protected String chooseQuery(HttpServletRequest req, String querySetName)
      throws SxseStorageException {
    return queryChooser.choose(storageManager, User.getUser(req),
        querySetName);
  }

  protected Set<String> getDisplayedQuerySetNames()
      throws SxseStorageException {
    // Get the query set names to display, if allowed.
    Set<String> querySetNames = null;
    QueryStorage queryStorage = storageManager.getQueryStorage();
    if (queryStorage.isShowingQuerySets()) {
      querySetNames = new TreeSet<String>();
      for (String querySetName : queryStorage.getQuerySetNames()) {
        // Only display active query sets.
        if (queryStorage.isActive(querySetName)) {
          querySetNames.add(querySetName);
        }
      }
    }
    return querySetNames;
  }

  /**
   * Returns whether the servlet should allow judgment for the given pair of
   * scoring policy profiles and the given query.
   * 
   * @param firstProfile the first scoring policy profile
   * @param secondProfile the second scoring policy profile
   * @param query the query
   * @return {@code true} if should allow judgment, {@code false} otherwise
   */
  static boolean allowJudgment(ScoringPolicyProfile firstProfile,
      ScoringPolicyProfile secondProfile, String query) {
    return ((query != null) &&
        ((firstProfile != ScoringPolicyProfile.EMPTY_PROFILE) ||
          (secondProfile != ScoringPolicyProfile.EMPTY_PROFILE)));
  }

  /**
   * Returns whether the servlet should allow judgment for the given pair of
   * search results.
   * 
   * @param firstResults the first list of search results
   * @param secondResults the second list of search results
   * @return {@code true} if should allow judgment, {@code false} otherwise
   */
  static boolean allowJudgment(List<SearchResult> firstResults,
      List<SearchResult> secondResults) {
    return (firstResults != null) && (secondResults != null);
  }

  /*
   * Determines whether the results for Policy A and Policy B should be swapped.
   * If necessary, the results, associated hashes and previous rating associated
   * with the query are swapped before being displayed to the user. On disk,
   * however, the results, hashes, and ratings are saved as if swapping did not
   * occur. This is because if the user makes a judgment on a query on which
   * all data was swapped, we swap all the data (from the POST input) again,
   * thereby generating the original data before saving it to disk. So this
   * implementation can be changed without "corrupting" any judgments in
   * storage.
   */
  static boolean shouldSwap(JudgmentStorage judgmentStorage, String query)
      throws SxseStorageException {
    return (judgmentStorage.isRandomSwapping()
        && ((query.hashCode() & 0x1) == 0x1));
  }

  /*
   * Swaps the judgment from FIRST_BETTER to SECOND_BETTER, and vice versa.
   */
  static Judgment swapJudgment(Judgment judgment) {
    if (judgment == Judgment.FIRST_BETTER) {
      return Judgment.SECOND_BETTER;
    } else if (judgment == Judgment.SECOND_BETTER) {
      return Judgment.FIRST_BETTER;
    }
    // Here if EQUAL or null.
    return judgment;
  }

  /**
   * Returns the last judgment made by the assessor on the given query against
   * the given scoring policy profiles, or {@code null} if no such judgment
   * exists. The judgment is rewritten by a {@link JudgmentExtractor} if the
   * ordering of the scoring policy profiles is reversed.
   * 
   * @param assessor the assessor
   * @param query the query to return the last judgment for
   * @param firstProfile the first scoring policy profile
   * @param secondProfile the second scoring policy profile
   * @return the last judgment made
   * @throws SxseStorageException if an error occurs
   */
  protected Judgment getLastJudgment(String assessor, final String query,
      ScoringPolicyProfile firstProfile, ScoringPolicyProfile secondProfile)
      throws SxseStorageException {
    // Get all judgments made by this user for the given query.
    JudgmentStorage judgmentStorage = storageManager.getJudgmentStorage();
    Iterable<JudgmentDetails> judgmentDetails =
        judgmentStorage.getJudgments(assessor, query);

    // Filter out judgments for different profiles.
    ScoringPolicyFilter scoringPolicyFilter =
        new ScoringPolicyFilter(firstProfile, secondProfile);
    Iterable<JudgmentDetails> policyDetails =
        Iterables.filter(judgmentDetails, scoringPolicyFilter);

    // Get the JudgmentDetails instance with the most recent timestamp.
    JudgmentDetails lastJudgmentDetail = null;
    for (JudgmentDetails judgmentDetail : policyDetails) {
      if ((lastJudgmentDetail == null) ||
          (judgmentDetail.getTimestamp() > lastJudgmentDetail.getTimestamp())) {
        lastJudgmentDetail = judgmentDetail;
      }
    }

    // Return the judgment, swapping its value if necessary.
    if (lastJudgmentDetail == null) {
      return null;
    }
    JudgmentExtractor extractor = new JudgmentExtractor(scoringPolicyFilter);
    return extractor.apply(lastJudgmentDetail);
  }

  protected String quoteAction(JudgmentAction action) {
    return "\"" + action.toString() + "\"";
  }

  /**
   * Displays the results for the given query to the user.
   * 
   * @param req the request the client made of the servlet
   * @param res the response the servlet returns to the client
   * @param query the query to display results for
   * @throws SxseStorageException if the results could not be accessed from
   *         storage
   * @throws IOException if an output error occurs
   */
  protected abstract void writeResults(
      HttpServletRequest req, HttpServletResponse res, String query)
      throws SxseStorageException, IOException;
}
