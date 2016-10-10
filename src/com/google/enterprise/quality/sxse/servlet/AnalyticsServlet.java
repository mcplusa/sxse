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

import com.google.enterprise.quality.sxse.ScoringPolicyProfile;
import com.google.enterprise.quality.sxse.gxp.Analytics;
import com.google.enterprise.quality.sxse.input.BooleanInputParser;
import com.google.enterprise.quality.sxse.input.DateTimeInputParser;
import com.google.enterprise.quality.sxse.input.InputErrors;
import com.google.enterprise.quality.sxse.input.ListStringInputParser;
import com.google.enterprise.quality.sxse.input.StringInputParser;
import com.google.enterprise.quality.sxse.storage.JudgmentStorage;
import com.google.enterprise.quality.sxse.storage.PreferencesStorage;
import com.google.enterprise.quality.sxse.storage.QueryStorage;
import com.google.enterprise.quality.sxse.storage.StorageManager;
import com.google.enterprise.quality.sxse.storage.SxseStorageException;
import com.google.gxp.base.GxpContext;

import org.joda.time.DateTime;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet through which the administrator can compute analytics results.
 */
public final class AnalyticsServlet extends HttpServlet {
  /**
   * The {@link BannerLink} for this servlet.
   */
  public static final BannerLink BANNER_LINK = new BannerLink() {
    public String getName() {
      return "Analytics";
    }

    public String getUrl() {
      return PATH;
    }
  };
  
  /**
   * The path for this servlet in the address.
   */
  public static final String PATH = "/analytics";

  private static final PolicyProbability.ConfidenceLevel[]
  CONFIDENCE_LEVELS = { PolicyProbability.ConfidenceLevel.CONFIDENCE_68,
                        PolicyProbability.ConfidenceLevel.CONFIDENCE_90,
                        PolicyProbability.ConfidenceLevel.CONFIDENCE_95 };

  private final Banner banner;
  private final StorageManager storageManager;

  public AnalyticsServlet(Banner banner, StorageManager storageManager) {
    this.banner = banner;
    this.storageManager = storageManager;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    try {
      write(req, res);
    } catch (SxseStorageException e) {
      throw new ServletException(e);
    }
  }

  public static final class PostKeys {
    /**
     * The first scoring policy profile.
     */
    public static final String FIRST_PROFILE = "firstProfile";

    /**
     * The second scoring policy profile.
     */
    public static final String SECOND_PROFILE = "secondProfile";

    /**
     * The names of the query sets to include judgments for.
     */
    public static final String QUERY_SET_NAMES = "querySetNames";

    /**
     * The names of the users to include judgments for.
     */
    public static final String USER_NAMES = "userNames";

    /**
     * True if we should use judgments for queries not in any query set.
     */
    public static final String USE_MISSING_QUERIES = "useMissingQueries";

    /**
     * True if we should count the number of judgments for each policy.
     */
    public static final String COMPUTE_TALLY = "computeTally";

    /**
     * True if we should calculate probabilities.
     */
    public static final String COMPUTE_PROBABILITY = "computeProbability";

    /**
     * True if we should only use judgments after the date
     * {@link #BEGIN_DATE_TIME}.
     */
    public static final String USE_BEGIN_DATE_TIME = "useBeginDateTime";

    /**
     * True if we should only use judgments before the date
     * {@link #END_DATE_TIME}.
     */
    public static final String USE_END_DATE_TIME = "useEndDateTime";

    /**
     * The date before which judgments are excluded.
     */
    public static final String BEGIN_DATE_TIME = "beginDateTime";

    /**
     * The date after which judgments are excluded.
     */
    public static final String END_DATE_TIME = "endDateTime";
  }
  
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    try {
      InputErrors.Builder<AnalyticsFormContext.Error> errorsBuilder =
          InputErrors.builder(AnalyticsFormContext.Error.class);
      PreferencesStorage prefsStorage = storageManager.getPreferencesStorage();

      // Get the names of the first and second profiles.
      StringInputParser.ParsedInput firstProfileInput = StringInputParser.allowAll().parse(
          PostKeys.FIRST_PROFILE, req.getParameterMap());
      String firstProfileName = firstProfileInput.getResult();
      StringInputParser.ParsedInput secondProfileInput = StringInputParser.allowAll().parse(
          PostKeys.SECOND_PROFILE, req.getParameterMap());
      String secondProfileName = secondProfileInput.getResult();
      
      // Ensure that profile selected by user has not been deleted concurrently.
      ScoringPolicyProfile firstProfile = (firstProfileName != null) ?
          prefsStorage.getProfile(firstProfileName) :
            ScoringPolicyProfile.EMPTY_PROFILE;
      ScoringPolicyProfile secondProfile = (secondProfileName != null) ?
          prefsStorage.getProfile(secondProfileName) :
            ScoringPolicyProfile.EMPTY_PROFILE;
      if (firstProfile == null) {
        errorsBuilder.setError(AnalyticsFormContext.Error.FIRST_PROFILE_MISSING,
            "Invalid profile name: " + firstProfileName);
      }
      if (secondProfile == null) {
        errorsBuilder.setError(AnalyticsFormContext.Error.SECOND_PROFILE_MISSING,
            "Invalid profile name: " + secondProfileName);
      }

      // Get whether queries not belonging to any query set should be used.
      BooleanInputParser.ParsedInput useMissingQueriesInput = BooleanInputParser.INSTANCE.parse(
          PostKeys.USE_MISSING_QUERIES, req.getParameterMap());
      boolean useMissingQueries = useMissingQueriesInput.getResult();

      // Alphabetize displayed query set and user names.
      ListStringInputParser.ParsedInput querySetNamesInput = ListStringInputParser.INSTANCE.parse(
          PostKeys.QUERY_SET_NAMES, req.getParameterMap());
      Set<String> currQuerySetNames = Collections.unmodifiableSet(
          new TreeSet<String>(querySetNamesInput.getResult()));
      ListStringInputParser.ParsedInput userNamesInput = ListStringInputParser.INSTANCE.parse(
          PostKeys.USER_NAMES, req.getParameterMap());
      Set<String> currUserNames = Collections.unmodifiableSet(
          new TreeSet<String>(userNamesInput.getResult()));

      if (!useMissingQueries && currQuerySetNames.isEmpty()) {
        // If not using missing queries, must select at least one query set.
        errorsBuilder.setError(AnalyticsFormContext.Error.NO_QUERY_SETS_SELECTED,
            "Must select one or more query sets");
      }
      if (currUserNames.isEmpty()) {
        errorsBuilder.setError(AnalyticsFormContext.Error.NO_USERS_SELECTED,
            "Must select one or more user names");
      }

      // Get the beginning date and time.
      BooleanInputParser.ParsedInput useBeginDateTimeInput = BooleanInputParser.INSTANCE.parse(
          PostKeys.USE_BEGIN_DATE_TIME, req.getParameterMap());
      boolean useBeginDateTime = useBeginDateTimeInput.getResult();
      DateTimeInputParser.ParsedInput filterBeginInput = null;
      if (useBeginDateTime) {
        filterBeginInput = DateTimeInputParser.allowAll().parse(
            PostKeys.BEGIN_DATE_TIME, req.getParameterMap());
        if (!filterBeginInput.hasResult()) {
          errorsBuilder.setError(AnalyticsFormContext.Error.BAD_BEGIN_DATE,
              "Start date is not a valid date");
        }
      }
      // Get the end date and time.
      BooleanInputParser.ParsedInput useEndDateTimeInput = BooleanInputParser.INSTANCE.parse(
          PostKeys.USE_END_DATE_TIME, req.getParameterMap());
      boolean useEndDateTime = useEndDateTimeInput.getResult(); 
      DateTimeInputParser.ParsedInput filterEndInput = null;
      if (useEndDateTime) {
        filterEndInput = DateTimeInputParser.allowAll().parse(
            PostKeys.END_DATE_TIME, req.getParameterMap());
        if (!filterEndInput.hasResult()) {
          errorsBuilder.setError(AnalyticsFormContext.Error.BAD_END_DATE,
              "End date is not a valid date");
        }
      }

      if (useBeginDateTime && useEndDateTime &&
          filterBeginInput.hasResult() && filterEndInput.hasResult()) {
        DateTime filterBegin = filterBeginInput.getResult();
        DateTime filterEnd = filterEndInput.getResult();
        // Only compare the dates and times for consistency if they were valid.
        if (filterBegin.compareTo(filterEnd) > 0) {
          errorsBuilder.setError(AnalyticsFormContext.Error.BAD_INTERVAL,
              "End date precedes start date");
        }
      }

      BooleanInputParser.ParsedInput computeTallyInput = BooleanInputParser.INSTANCE.parse(
          PostKeys.COMPUTE_TALLY, req.getParameterMap());
      boolean computeTally = computeTallyInput.getResult();
      BooleanInputParser.ParsedInput computeProbabilityInput = BooleanInputParser.INSTANCE.parse(
          PostKeys.COMPUTE_PROBABILITY, req.getParameterMap());
      boolean computeProbability = computeProbabilityInput.getResult();

      InputErrors<AnalyticsFormContext.Error> errors = errorsBuilder.build();

      List<AnalyticsResult> results = null;
      if (!errors.isEmpty()) {
        // Input had errors, so do not proceed with computing analytics.
        results = Collections.emptyList();
      } else {
        AnalyticsFilter filter = new AnalyticsFilter(
            firstProfile, secondProfile,
            currQuerySetNames, currUserNames, useMissingQueries,
            useBeginDateTime ? filterBeginInput.getResult() : null,
            useEndDateTime ? filterEndInput.getResult() : null,
            storageManager);
        if (computeTally) {
          filter.addMetric(new PolicyTally(firstProfile, secondProfile));
        }
        if (computeProbability) {
          filter.addMetric(new PolicyProbability(
                               firstProfile, secondProfile, false,
                               CONFIDENCE_LEVELS));
        }
        results = filter.computeResults();
      }
      
      AnalyticsFormContext fc = new AnalyticsFormContext(
          firstProfileName, secondProfileName, currQuerySetNames, currUserNames,
          useMissingQueries, useBeginDateTime, useEndDateTime,
          filterBeginInput, filterEndInput,
          computeTally, computeProbability, results, errors);

      write(req, res, fc);
    } catch (SxseStorageException e) {
      throw new ServletException(e);
    }
  }

  /**
   * Structure for holding all data to initialize the Analytics page and forms.
   */
  public static class AnalyticsFormContext {
    // The components of a timestamp.
    public static class DateTimeFields {
      public final int year;
      public final int month;
      public final int day;
      public final int hour;
      public final int minute;

      public DateTimeFields(DateTimeInputParser.ParsedInput input) {
        this.year = Integer.valueOf(input.getYear());
        this.month = Integer.valueOf(input.getMonth());
        this.day = Integer.valueOf(input.getDay());
        this.hour = Integer.valueOf(input.getHour());
        this.minute = Integer.valueOf(input.getMinute());
      }

      public DateTimeFields() {
        DateTime currDateTime = new DateTime();
        year = currDateTime.getYear();
        month = currDateTime.getMonthOfYear();
        day = currDateTime.getDayOfMonth();
        hour = currDateTime.getHourOfDay();
        minute = currDateTime.getMinuteOfHour();
      }

      @Override
      public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("year=").append(year);
        sb.append(", month=").append(month);
        sb.append(", day=").append(day);
        sb.append(", hour=").append(hour);
        sb.append(", minute=").append(minute);
        sb.append('}');
        return sb.toString();
      }
    }

    public final String firstProfile;
    public final String secondProfile;
    
    public final Set<String> querySetNames;
    public final Set<String> userNames;

    public final boolean useMissingQueries;
    public final boolean useBeginDateTime;
    public final boolean useEndDateTime;
    public final DateTimeFields beginDateTime;
    public final DateTimeFields endDateTime;

    public final boolean computeTally;
    public final boolean computeProbability;
    public final List<AnalyticsResult> results;

    public final InputErrors<Error> errors;

    public static enum Error {
      FIRST_PROFILE_MISSING,
      SECOND_PROFILE_MISSING,
      NO_QUERY_SETS_SELECTED,
      NO_USERS_SELECTED,
      BAD_BEGIN_DATE,
      BAD_END_DATE,
      BAD_INTERVAL,
    }

    // Uses the default values, retrieved from storage.
    public AnalyticsFormContext(PreferencesStorage prefsStorage)
        throws SxseStorageException {
      ScoringPolicyProfile firstPolicy = prefsStorage.getFirstProfile();
      firstProfile = (firstPolicy != null) ? firstPolicy.getName() : null;
      ScoringPolicyProfile secondPolicy = prefsStorage.getSecondProfile();
      secondProfile = (secondPolicy != null) ? secondPolicy.getName() : null;

      querySetNames = Collections.emptySet();
      userNames = Collections.emptySet();

      useMissingQueries = false;
      useBeginDateTime = false;
      useEndDateTime = false;
      DateTimeFields currDateTimeFields = new DateTimeFields();
      beginDateTime = currDateTimeFields;
      endDateTime = currDateTimeFields;

      computeTally = false;
      computeProbability = false;
      results = Collections.emptyList();

      errors = InputErrors.getEmpty();
    }

    // Uses the values submitted from the form.
    public AnalyticsFormContext(String firstProfile, String secondProfile,
        Set<String> querySetNames, Set<String> userNames,
        boolean useMissingQueries,
        boolean useBeginDateTime, boolean useEndDateTime,
        DateTimeInputParser.ParsedInput beginInput, DateTimeInputParser.ParsedInput endInput,
        boolean computeTally, boolean computeProbability,
        List<AnalyticsResult> results, InputErrors<Error> errors) {
      this.firstProfile = firstProfile;
      this.secondProfile = secondProfile;
      
      this.querySetNames = querySetNames;
      this.userNames = userNames;
      
      this.useMissingQueries = useMissingQueries;
      this.useBeginDateTime = useBeginDateTime;
      this.useEndDateTime = useEndDateTime;
      DateTimeFields currDateTimeFields = new DateTimeFields();
      this.beginDateTime = (beginInput != null) ?
          new DateTimeFields(beginInput) : currDateTimeFields;
      this.endDateTime = (endInput != null) ?
          new DateTimeFields(endInput) : currDateTimeFields;

      this.computeTally = computeTally;
      this.computeProbability = computeProbability;
      this.results = results;
      
      this.errors = errors;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append('{');
      sb.append("firstProfile=").append(firstProfile);
      sb.append(", secondProfile=").append(secondProfile);
      sb.append(", querySetNames=").append(querySetNames);
      sb.append(", userNames=").append(userNames);
      sb.append(", useMissingQueries=").append(useMissingQueries);
      sb.append(", useBeginDateTime").append(useBeginDateTime);
      sb.append(", useEndDateTime=").append(useEndDateTime);
      sb.append(", beginDateTime=").append(beginDateTime);
      sb.append(", endDateTime=").append(endDateTime);
      sb.append(", computeTally=").append(computeTally);
      sb.append(", computeProbability=").append(computeProbability);
      sb.append(", results=").append(results);
      sb.append(", errors=").append(errors);
      sb.append('}');
      return sb.toString();
    }
  }

  /*
   * Write the given results, and display the analytics setup with the active
   * scoring policy profiles selected, no query sets selected, and no users
   * selected.
   */
  private void write(HttpServletRequest req, HttpServletResponse res)
      throws IOException, SxseStorageException {
    PreferencesStorage prefsStorage = storageManager.getPreferencesStorage();
    AnalyticsFormContext formContext = new AnalyticsFormContext(prefsStorage);

    write(req, res, formContext);
  }

  /*
   * Write the given results, and display the analytics setup with the given
   * policy profiles, query sets, and users all selected.
   */
  private void write(
      HttpServletRequest req, HttpServletResponse res,
      AnalyticsFormContext formContext) throws SxseStorageException, IOException {
    PreparePage.write(res);
    Writer writer = res.getWriter();

    PreferencesStorage prefsStorage = storageManager.getPreferencesStorage();
    List<ScoringPolicyProfile> policyProfiles = prefsStorage.getProfiles();
    QueryStorage queryStorage = storageManager.getQueryStorage();
    Set<String> querySetNames = queryStorage.getQuerySetNames();
    JudgmentStorage judgmentStorage = storageManager.getJudgmentStorage();
    Set<String> userNames = judgmentStorage.getUsers();

    Analytics.write(writer, new GxpContext(req.getLocale()),
        User.getUser(req), banner,
        policyProfiles, querySetNames, userNames, formContext);
  }
}
