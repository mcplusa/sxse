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
import com.google.common.collect.Maps;
import com.google.enterprise.quality.sxse.JudgmentDetails;
import com.google.enterprise.quality.sxse.ScoringPolicyProfile;
import com.google.enterprise.quality.sxse.JudgmentDetails.Judgment;
import com.google.enterprise.quality.sxse.storage.JudgmentStorage;
import com.google.enterprise.quality.sxse.storage.QueryStorage;
import com.google.enterprise.quality.sxse.storage.StorageManager;
import com.google.enterprise.quality.sxse.storage.SxseStorageException;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A class that computes the results of a set of metrics over judgments filtered
 * by a set of criteria.
 */
class AnalyticsFilter {
  private final ScoringPolicyProfile firstProfile;
  private final ScoringPolicyProfile secondProfile;
  private final Set<String> querySetNames;
  private final Set<String> users;
  private final boolean useMissingQueries;
  private final DateTime beginTime;
  private final DateTime endTime;
  private final StorageManager storageManager;

  protected final List<AnalyticsMetric> metrics;

  /**
   * Constructs a new filter that accepts the judgments for the given profiles
   * for all queries belonging to all query sets, for all users, across all
   * times, but does not include judgments for queries not in any query set.
   * 
   * @param firstProfile the first profile for tallying
   * @param secondProfile the second profile for tallying
   * @param storageManager the storage manager to get all query sets, all users,
   *        and judgments from
   * @throws SxseStorageException if an error occurs
   */
  public AnalyticsFilter(ScoringPolicyProfile firstProfile,
      ScoringPolicyProfile secondProfile, StorageManager storageManager)
      throws SxseStorageException {
    this(firstProfile, secondProfile,
        storageManager.getQueryStorage().getQuerySetNames(),
        storageManager.getJudgmentStorage().getUsers(), false, null, null,
        storageManager);
  }

  /**
   * Constructs a new filter that accepts the judgments for the given profiles
   * for the queries belonging to only the given query sets, for only the
   * given users, for only the given times, and includes judgments for queries
   * not in any query set if specified.
   * 
   * @param firstProfile the first profile for tallying
   * @param secondProfile the second profile for tallying
   * @param querySetNames the names of the query sets containing queries to
   *        tally judgments for
   * @param users the names of the users to tally judgments for
   * @param useMissingQueries {@code true} if judgments for queries not in any
   *        query set should be tallied, {@code false} otherwise
   * @param beginTime the date to exclude judgments before, or {@code null} if
   *        no minimum time is imposed
   * @param endTime the date to exclude judgments after, or {@code null} if no
   *        maximum time is imposed
   * @param storageManager the storage manager to get query sets, users, and
   *        judgments from
   */
  public AnalyticsFilter(
      ScoringPolicyProfile firstProfile, ScoringPolicyProfile secondProfile,
      Set<String> querySetNames, Set<String> users, boolean useMissingQueries,
      DateTime beginTime, DateTime endTime, StorageManager storageManager) {
    this.firstProfile = firstProfile;
    this.secondProfile = secondProfile;
    this.querySetNames = querySetNames;
    this.users = users;
    this.useMissingQueries = useMissingQueries;
    this.beginTime = beginTime;
    this.endTime = endTime;
    this.storageManager = storageManager;

    metrics = new LinkedList<AnalyticsMetric>();
  }

  /**
   * Adds this metric to the filter.
   * 
   * @param metric the metric to add to the filter
   */
  public void addMetric(AnalyticsMetric metric) {
    metrics.add(metric);
  }

  private static final class DetailsJudgmentPair {
    final JudgmentDetails details;
    final Judgment judgment;
    
    public DetailsJudgmentPair(JudgmentDetails details, Judgment judgment) {
      this.details = details;
      this.judgment = judgment;
    }
  }

  /**
   * Computes the results for each metric added to this filter. This method
   * resets each metric via {@link AnalyticsMetric#reset()}, reads each judgment
   * from storage, filters them according to the criteria passed into the
   * constructor, notifies each added metric of each judgment passing all
   * filters via
   * {@link AnalyticsMetric#readJudgment(String, JudgmentDetails, Judgment)},
   * and then returns the results of each metric via
   * {@link AnalyticsMetric#getResult()}.
   * 
   * @return a list of the results generated by all metrics
   * @throws SxseStorageException if an error occurs
   */
  public List<AnalyticsResult> computeResults() throws SxseStorageException {
    // Reset the state of all metrics to prepare them for computation.
    resetMetrics();

    // The queries of the selected query sets.
    TreeSet<String> selectedQueries = new TreeSet<String>();
    // If tallying missing queries, the queries of all other query sets.
    TreeSet<String> otherQueries = useMissingQueries ?
        new TreeSet<String>() : null;
    QueryStorage queryStorage = storageManager.getQueryStorage();
    for (String querySetName : queryStorage.getQuerySetNames()) {
      if (querySetNames.contains(querySetName)) {
        // Query set was selected by the user.
        List<String> querySet = queryStorage.getQuerySet(querySetName);
        selectedQueries.addAll(querySet);
      } else if (useMissingQueries) {
        // Query set was not selected, but tallying missing queries, so add.
        List<String> querySet = queryStorage.getQuerySet(querySetName);
        otherQueries.addAll(querySet);
      }
    }

    ScoringPolicyFilter scoringPolicyFilter = new ScoringPolicyFilter(
        firstProfile, secondProfile);
    JudgmentExtractor extractor = new JudgmentExtractor(scoringPolicyFilter);

    JudgmentStorage judgmentStorage = storageManager.getJudgmentStorage();
    Map<String, DetailsJudgmentPair> queryJudgmentMap = Maps.newTreeMap();

    for (String user : users) {
      // Use a filter to identify judgments against the current profiles.
      Iterable<JudgmentDetails> detailsList = Iterables.filter(
          judgmentStorage.getJudgments(user), scoringPolicyFilter);

      for (JudgmentDetails details : detailsList) {
        String query = details.getQuery();
        if (!selectedQueries.contains(query)) {
          // Query does not belong to a selected query set.
          if (!useMissingQueries || otherQueries.contains(query)) {
            // If using missing queries, pass if query is in another query set.
            continue;
          }
        }

        if ((beginTime != null) &&
            (beginTime.getMillis() > details.getTimestamp())) {
          continue;
        } else if ((endTime != null) &&
            (endTime.getMillis() < details.getTimestamp())) {
          continue;
        }

        // Replace the previous judgment associated with the query if it exists.
        Judgment judgment = extractor.apply(details);
        queryJudgmentMap.put(details.getQuery(),
            new DetailsJudgmentPair(details, judgment));
      }

      if (!queryJudgmentMap.isEmpty()) {
        // Notify each metric of each filtered judgment.
        for (DetailsJudgmentPair pair : queryJudgmentMap.values()) {
          notifyMetrics(user, pair.details, pair.judgment);
        }
        // Clear all judgments associated with queries for the next user.
        queryJudgmentMap.clear();
      }
    }

    // Collect and return the results from each metric.
    return getMetricResults();
  }

  private void resetMetrics() {
    for (AnalyticsMetric metric : metrics) {
      metric.reset();
    }
  }

  private void notifyMetrics(String user, JudgmentDetails details,
      Judgment judgment) {
    for (AnalyticsMetric metric : metrics) {
      metric.readJudgment(user, details, judgment);
    }
  }

  private List<AnalyticsResult> getMetricResults() {
    List<AnalyticsResult> results =
        new ArrayList<AnalyticsResult>(metrics.size());
    for (AnalyticsMetric metric : metrics) {
      results.add(metric.getResult());
    }
    return results;
  }
}
