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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.enterprise.quality.sxse.JudgmentDetails;
import com.google.enterprise.quality.sxse.ScoringPolicyProfile;
import com.google.enterprise.quality.sxse.SearchResult;
import com.google.enterprise.quality.sxse.JudgmentDetails.Judgment;
import com.google.enterprise.quality.sxse.gxp.UserHistory;
import com.google.enterprise.quality.sxse.gxp.Users;
import com.google.enterprise.quality.sxse.gxp.JudgmentSnapshot;
import com.google.enterprise.quality.sxse.input.EnumInputParser;
import com.google.enterprise.quality.sxse.input.ListStringInputParser;
import com.google.enterprise.quality.sxse.input.StringInputParser;
import com.google.enterprise.quality.sxse.servlet.UsersServlet.QueryHistory.QueryJudgment;
import com.google.enterprise.quality.sxse.storage.JudgmentStorage;
import com.google.enterprise.quality.sxse.storage.PreferencesStorage;
import com.google.enterprise.quality.sxse.storage.StorageManager;
import com.google.enterprise.quality.sxse.storage.SxseStorageException;
import com.google.gxp.base.GxpContext;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet through which the administrator can delete users.
 */
public class UsersServlet extends HttpServlet {
  /**
   * The {@link BannerLink} for this servlet.
   */
  public static final BannerLink BANNER_LINK = new BannerLink() {
    public String getName() {
      return "Users";
    }

    public String getUrl() {
      return PATH;
    }
  };

  /**
   * The path for this servlet in the address.
   */
  public static final String PATH = "/users";

  private static final Logger LOGGER = Logger.getLogger(
      UsersServlet.class.getName());

  private final Banner banner;
  private final StorageManager storageManager;

  /**
   * The action to take on a user.
   */
  public static enum UserAction {
    /**
     * View the history of the user.
     */
    VIEW,

    /**
     * Delete the selected users.
     */
    DELETE,

    /**
     * View the results judged by the user.
     */
    VIEW_RESULTS,
  }

  public static final class CommonKeys {
    /**
     * The action to take; may be {@code null} if a GET request.
     */
    public static final String ACTION = "userAction";

    /**
     * If action is {@link UserAction#VIEW}, the user to view.
     */
    public static final String USER_NAME = "userName";

    /**
     * If action is {@link UserAction#VIEW_RESULTS}, the query.
     */
    public static final String QUERY = "query";

    /**
     * If action is {@link UserAction#VIEW_RESULTS}, the first profile name.
     */
    public static final String FIRST_PROFILE_NAME = "firstProfileName";

    /**
     * If action is {@link UserAction#VIEW_RESULTS}, the second profile name.
     */
    public static final String SECOND_PROFILE_NAME = "secondProfileName";

    /**
     * If action is {@link UserAction#VIEW_RESULTS}, the judgment.
     */
    public static final String JUDGMENT = "judgment";

    /**
     * If action is {@link UserAction#VIEW_RESULTS}, the hash of the results.
     */
    public static final String RESULTS_ID = "resultsId";

    /**
     * If action is {@link UserAction#DELETE}, the users to delete.
     */
    public static final String USERS_TO_DELETE = "usersToDelete";
  }

  /**
   * Creates a new {@code AdminUsersServlet} that uses the given
   * {@link StorageManager}.
   * 
   * @param banner the banner to display across the top of the page
   * @param storageManager the storage manager
   */
  public UsersServlet(Banner banner, StorageManager storageManager) {
    this.banner = banner;
    this.storageManager = storageManager;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException {
    EnumInputParser.ParsedInput<UserAction> actionInput =
      new EnumInputParser<UserAction>(UserAction.class).parse(
        CommonKeys.ACTION, req.getParameterMap());
    UserAction action = actionInput.getResult();

    try {
      // Handle action common to both GET and POST.
      handleCommonAction(req, res, action);
    } catch (SxseStorageException e) {
      throw new ServletException(e);
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    EnumInputParser.ParsedInput<UserAction> actionInput =
        new EnumInputParser<UserAction>(UserAction.class).parse(
          CommonKeys.ACTION, req.getParameterMap());
    UserAction action = actionInput.getResult();

    try {
      if (action == UserAction.DELETE) {
        ListStringInputParser.ParsedInput usersToDeleteInput = ListStringInputParser.INSTANCE.parse(
            CommonKeys.USERS_TO_DELETE, req.getParameterMap());
        JudgmentStorage judgmentStorage = storageManager.getJudgmentStorage();
        judgmentStorage.removeUsers(
            ImmutableSet.copyOf(usersToDeleteInput.getResult()));
        write(req, res);
        return;
      }
  
      // Handle action common to both GET and POST.
      handleCommonAction(req, res, action);
    } catch (SxseStorageException e) {
      throw new ServletException(e);
    }
  }

  private void handleCommonAction(HttpServletRequest req,
      HttpServletResponse res, UserAction action)
      throws IOException, SxseStorageException {
    if (action == UserAction.VIEW) {
      StringInputParser.ParsedInput userNameInput = StringInputParser.allowAll().parse(
          CommonKeys.USER_NAME, req.getParameterMap());
      String userName = userNameInput.getResult();
      if (userName != null) {
        // Write the history of the user.
        writeUserInfo(req, res, userName);
        return;
      }
    } else if (action == UserAction.VIEW_RESULTS) {
      StringInputParser.ParsedInput userNameInput = StringInputParser.allowAll().parse(
          CommonKeys.USER_NAME, req.getParameterMap());
      StringInputParser.ParsedInput queryInput = StringInputParser.allowAll().parse(
          CommonKeys.QUERY, req.getParameterMap());
      StringInputParser.ParsedInput firstProfileNameInput = StringInputParser.allowAll().parse(
          CommonKeys.FIRST_PROFILE_NAME, req.getParameterMap());
      StringInputParser.ParsedInput secondProfileNameInput = StringInputParser.allowAll().parse(
          CommonKeys.SECOND_PROFILE_NAME, req.getParameterMap());
      EnumInputParser.ParsedInput<Judgment> judgmentInput =
          new EnumInputParser<Judgment>(Judgment.class).parse(
            CommonKeys.JUDGMENT, req.getParameterMap());
      StringInputParser.ParsedInput resultsIdInput = StringInputParser.allowAll().parse(
          CommonKeys.RESULTS_ID, req.getParameterMap());

      if (userNameInput.hasResult() && queryInput.hasResult() &&
          firstProfileNameInput.hasResult() && secondProfileNameInput.hasResult() &&
          judgmentInput.hasResult() && resultsIdInput.hasResult()) {
        // Write the results evaluated by the user.
        writeJudgmentSnapshot(req, res,
            userNameInput.getResult(), queryInput.getResult(),
            firstProfileNameInput.getResult(), secondProfileNameInput.getResult(),
            judgmentInput.getResult(), resultsIdInput.getResult());
        return;
      }
    }

    // No action provided, write the default user page.
    write(req, res);
  }

  private String getProfileName(ScoringPolicyProfile profile) {
    return (profile != ScoringPolicyProfile.EMPTY_PROFILE) ? profile.getName()
        : "No Results";
  }

  private void write(HttpServletRequest req, HttpServletResponse res)
      throws SxseStorageException, IOException {
    // Get the list of all users
    JudgmentStorage judgmentStorage = storageManager.getJudgmentStorage();
    Set<String> users = judgmentStorage.getUsers();

    PreparePage.write(res);
    Writer writer = res.getWriter();

    Users.write(writer, new GxpContext(req.getLocale()),
        User.getUser(req), banner, users);
  }

  private void writeUserInfo(HttpServletRequest req, HttpServletResponse res,
      String userName) throws SxseStorageException, IOException {
    PreferencesStorage prefsStorage = storageManager.getPreferencesStorage();
    JudgmentStorage judgmentStorage = storageManager.getJudgmentStorage();
    // Get the judgments for this user.
    List<JudgmentDetails> judgments = judgmentStorage.getJudgments(userName);
    if (judgments == null) {
      // Invalid username, write the default user page.
      write(req, res);
      return;
    }

    // Get the current scoring policy profiles and their names.
    ScoringPolicyProfile firstProfile = prefsStorage.getFirstProfile();
    ScoringPolicyProfile secondProfile = prefsStorage.getSecondProfile();

    // Create all state used to generate user information.
    UserStatistics globalStats = new UserStatistics();
    UserStatistics profileStats = new UserStatistics();
    Map<String, QueryHistory> detailsMap = new TreeMap<String, QueryHistory>();
    Set<String> otherQueries = new TreeSet<String>();

    // Use a filter to identify judgments against the current profiles. 
    ScoringPolicyFilter scoringPolicyFilter = new ScoringPolicyFilter(
        firstProfile, secondProfile);
    for (JudgmentDetails details : judgments) {
      String query = details.getQuery();
      long timestamp = details.getTimestamp();
      // Update the statistics for all profiles.
      globalStats.update(timestamp);

      if (scoringPolicyFilter.apply(details)) {
        String firstProfileName = null;
        String secondProfileName = null;
        if (firstProfile.getQueryFormatter().equals(
            details.getFirstQueryFormatter())) {
          // Profiles in judgment are in same order as current profiles.
          firstProfileName = firstProfile.getName();
          secondProfileName = secondProfile.getName();
        } else {
          // Profiles in judgment are in reverse order from current profiles.
          firstProfileName = secondProfile.getName();
          secondProfileName = firstProfile.getName();
        }

        // Update the statistics for the current profiles.
        profileStats.update(timestamp);

        // Get the list of judgments for this query.
        QueryHistory judgmentDetails = detailsMap.get(query);
        if (judgmentDetails == null) {
          // If no list exists, create one.
          judgmentDetails = new QueryHistory(query);
          detailsMap.put(query, judgmentDetails);

          // Remove from queries judged not against the current profiles.
          otherQueries.remove(query);
        }

        // Add the details of this judgment to the list.
        QueryJudgment queryDetails = new QueryJudgment(details.getTimestamp(),
            firstProfileName, secondProfileName, details.getJudgment(),
            details.getResultsId());
        judgmentDetails.addDetails(queryDetails);
      } else if (!detailsMap.containsKey(query)) {
        // Add to queries judged not against the current profiles.
        otherQueries.add(query);
      }
    }

    // Generate history and finalize statistics, then display to user.
    List<QueryHistory> queryHistory = new LinkedList<QueryHistory>(
        detailsMap.values());
    profileStats.setNumQueries(detailsMap.size());
    globalStats.setNumQueries(profileStats.getNumQueries()
        + otherQueries.size());
    writeUserInfo(req, res, userName, queryHistory, globalStats,
        profileStats);
  }

  private void writeUserInfo(HttpServletRequest req, HttpServletResponse res,
      String userName, List<QueryHistory> queryHistory,
      UserStatistics globalStats, UserStatistics profileStats)
      throws IOException {
    PreparePage.write(res);
    Writer writer = res.getWriter();

    UserHistory.write(writer, new GxpContext(req.getLocale()),
        User.getUser(req), banner, SxseServletUtil.getDateTimeFormat(req.getLocale()),
        userName, queryHistory, globalStats, profileStats);
  }

  private void writeJudgmentSnapshot(
      HttpServletRequest req, HttpServletResponse res,
      String userName, String query,
      String firstProfileName, String secondProfileName, Judgment judgment,
      String resultsId) throws IOException, SxseStorageException {
    // Retrieve the results having the given identifier from storage.
    JudgmentStorage judgmentStorage = storageManager.getJudgmentStorage();
    List<SearchResult> firstResults = new LinkedList<SearchResult>();
    List<SearchResult> secondResults = new LinkedList<SearchResult>();
    judgmentStorage.getResults(resultsId, firstResults, secondResults);

    PreferencesStorage prefsStorage = storageManager.getPreferencesStorage();
    ScoringPolicyProfile firstProfile = prefsStorage.getFirstProfile();
    ScoringPolicyProfile secondProfile = prefsStorage.getSecondProfile();

    // Write the snapshot of the judgment.
    writeJudgmentSnapshot(req, res, userName, query,
        firstProfileName, secondProfileName, judgment,
        firstResults, secondResults);
  }

  private void writeJudgmentSnapshot(HttpServletRequest req,
      HttpServletResponse res, String userName, String query,
      String firstProfileName, String secondProfileName, Judgment judgment,
      List<SearchResult> firstResults, List<SearchResult> secondResults)
      throws IOException {
    PreparePage.write(res);
    Writer writer = res.getWriter();

    JudgmentSnapshot.write(writer, new GxpContext(req.getLocale()),
        User.getUser(req), banner, userName, query,
        firstProfileName, secondProfileName,
        getChosenProfileName(judgment, firstProfileName, secondProfileName),
        firstResults, secondResults);
  }

  private static String getChosenProfileName(Judgment judgment,
      String firstProfileName, String secondProfileName) {
    if (judgment == Judgment.EQUAL) {
      return "Equal";
    }
    String chosenProfileName = (judgment == Judgment.FIRST_BETTER) ?
        firstProfileName : secondProfileName;
    return (chosenProfileName == null) ? "Blank profile" : chosenProfileName;
  }

  /**
   * The history of judgments by a user for a given query.
   */
  public static class QueryHistory {
    private final String query;
    private final List<QueryJudgment> judgments;

    /*
     * Prohibit instantiation outside this class.
     */
    private QueryHistory(String query) {
      this.query = query;
      this.judgments = new LinkedList<QueryJudgment>();
    }

    /*
     * Appends the given judgment to the history of this query.
     */
    private void addDetails(QueryJudgment details) {
      judgments.add(details);
    }

    /**
     * @return the query judged
     */
    public String getQuery() {
      return query;
    }

    /**
     * @return the judgments made by the user for the query
     */
    public List<QueryJudgment> getDetails() {
      return judgments;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append('{');
      sb.append("query=").append(query);
      sb.append(", judgments=").append(judgments);
      sb.append('}');
      return sb.toString();
    }

    /**
     * A judgment made by a user against a given query.
     */
    public static class QueryJudgment {
      private final long timestamp;
      private final String firstProfileName;
      private final String secondProfileName;
      private final Judgment judgment;
      private final String resultsId;

      /*
       * Prohibit instantiation outside this class.
       */
      private QueryJudgment(long timestamp,
          String firstProfileName, String secondProfileName,
          Judgment judgment, String resultsId) {
        this.timestamp = timestamp;
        this.firstProfileName = firstProfileName;
        this.secondProfileName = secondProfileName;
        this.judgment = judgment;
        this.resultsId = resultsId;
      }

      /**
       * @return the timestamp of the judgment
       */
      public long getTimestamp() {
        return timestamp;
      }

      /**
       * @return the name of the second profile
       */
      public String getFirstProfileName() {
        return firstProfileName;
      }

      /**
       * @return the name of the first profile
       */
      public String getSecondProfileName() {
        return secondProfileName;
      }

      /**
       * @return the judgment made by the assessor
       */
      public String getJudgment() {
        return judgment.toString();
      }

      /**
       * @return the name of the chosen profile
       */
      public String getChosenProfileName() {
        return UsersServlet.getChosenProfileName(judgment,
            firstProfileName, secondProfileName);
      }

      /**
       * @return the identifier of the side-by-side results, or {@code null} if
       *         the results were not saved
       */
      public String getResultsId() {
        return resultsId;
      }
      
      @Override
      public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("timestamp=").append(timestamp);
        sb.append(", firstProfileName=").append(firstProfileName);
        sb.append(", secondProfielName=").append(secondProfileName);
        sb.append(", judgemnt=").append(judgment);
        sb.append(", resultsId=").append(resultsId);
        sb.append('}');
        return sb.toString();
      }
    }
  }

  /**
   * A class that contains basic statistics about a user.
   */
  public static class UserStatistics {
    private int numJudgments;
    private int numQueries;
    private long minTimestamp;
    private long maxTimestamp;

    /*
     * Prohibit instantiation outside this class.
     */
    private UserStatistics() {
      numJudgments = 0;
      minTimestamp = Long.MAX_VALUE;
      maxTimestamp = Long.MIN_VALUE;
    }

    /*
     * Updates the statistics given the timestamp of a new judgment.
     */
    private void update(long timestamp) {
      ++numJudgments;
      if (timestamp < minTimestamp) {
        minTimestamp = timestamp;
      }
      if (timestamp > maxTimestamp) {
        maxTimestamp = timestamp;
      }
    }

    /*
     * Sets the number of queries judged
     */
    private void setNumQueries(int numQueries) {
      this.numQueries = numQueries;
    }

    /**
     * @return the number of queries judged
     */
    public int getNumQueries() {
      return numQueries;
    }

    /**
     * @return the number of judgments issued
     */
    public int getNumJudgments() {
      return numJudgments;
    }

    /**
     * @return the timestamp of the first judgment issued by the user
     */
    public long getMinTimestamp() {
      return (numJudgments == 0) ? -1 : minTimestamp;
    }

    /**
     * @return the timestamp of the last judgment issued by the user
     */
    public long getMaxTimestamp() {
      return (numJudgments == 0) ? -1 : maxTimestamp;
    }
  }
}
