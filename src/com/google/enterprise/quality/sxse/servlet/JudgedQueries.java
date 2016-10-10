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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.enterprise.quality.sxse.JudgmentDetails;
import com.google.enterprise.quality.sxse.ScoringPolicyProfile;
import com.google.enterprise.quality.sxse.storage.JudgmentStorage;
import com.google.enterprise.quality.sxse.storage.PreferencesStorage;
import com.google.enterprise.quality.sxse.storage.QueryStorage;
import com.google.enterprise.quality.sxse.storage.StorageManager;
import com.google.enterprise.quality.sxse.storage.SxseStorageException;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Class for recording what queries an assessor has judged across all query
 * sets.
 */
class JudgedQueries {
  // Maximum number of queries we keep queued for marking as judged later.
  public static final int MAX_UNKNOWN_QUERIES = 5;

  private static final Logger LOGGER = Logger.getLogger(
      JudgedQueries.class.getName());

  final String userName;
  final List<QuerySetJudgments> querySetJudgmentList;
  final List<String> unknownQueries;

  boolean forceInitAgain;

  /**
   * The details associated with a query, or what query set it belongs to, and
   * what index it occupies in the internal bit set.
   */
  static class ChosenQueryDetails {
    final String query;
    final String querySetName;
    final int bitIndex;

    /**
     * Constructor used if the query set and bit set index are unknown.
     * 
     * @param query the query
     */
    public ChosenQueryDetails(String query) {
      this(query, null, -1);
    }

    /**
     * Constructor used if the query set and bit set index are known.
     * 
     * @param query the query
     * @param querySetName the name of the query set
     * @param bitIndex the index of the bit set
     */
    public ChosenQueryDetails(String query, String querySetName, int bitIndex) {
      this.query = query;
      this.querySetName = querySetName;
      this.bitIndex = bitIndex;
    }

    /**
     * @return {@code true} if the details of the query are known, {@code false}
     *         otherwise
     */
    public boolean detailsKnown() {
      return ((querySetName != null) && (bitIndex >= 0));
    }
  }

  /**
   * Creates a new record of judged queries for the user, and populates it
   * using the given storage manager.
   * 
   * @param userName the name of the user
   * @param storageManager the storage manager through which all query sets and
   *        the user's judgments are retrieved
   * @throws SxseStorageException if an error occurs
   */
  public JudgedQueries(String userName, StorageManager storageManager)
      throws SxseStorageException {
    this.userName = userName;
    querySetJudgmentList = new LinkedList<QuerySetJudgments>();
    unknownQueries = new LinkedList<String>();
    forceInitAgain = false;

    // About to choose query, so populate judged queries.
    QueryStorage queryStorage = storageManager.getQueryStorage();
    Iterable<JudgmentDetails> allJudgments = makeAllJudgments(storageManager);
    for (String setName : queryStorage.getQuerySetNames()) {
      querySetJudgmentList.add(new QuerySetJudgments(setName, storageManager,
          allJudgments));
    }
  }

  private Iterable<JudgmentDetails> makeAllJudgments(
      StorageManager storageManager) throws SxseStorageException {
    PreferencesStorage prefsStorage = storageManager.getPreferencesStorage();
    ScoringPolicyProfile firstProfile = prefsStorage.getFirstProfile();
    ScoringPolicyProfile secondProfile = prefsStorage.getSecondProfile();
    // Return only those judgments made against the current policy profiles.
    JudgmentStorage judgmentStorage = storageManager.getJudgmentStorage();
    return Iterables.filter(
        judgmentStorage.getJudgments(userName),
        new ScoringPolicyFilter(firstProfile, secondProfile));
  }

  private void setUnknownQueries(StorageManager storageManager)
      throws SxseStorageException {
    // Find what query set each query belongs to, and mark the query as judged.
    QueryStorage queryStorage = storageManager.getQueryStorage();
    for (QuerySetJudgments querySetJudgments : querySetJudgmentList) {
      List<String> queries = queryStorage.getQuerySet(
          querySetJudgments.querySetName);

      for (Iterator<String> i = unknownQueries.iterator(); i.hasNext(); ) {
        String unknownQuery = i.next();

        int index = Collections.binarySearch(queries, unknownQuery);
        if (index >= 0) {
          // Query belongs to this query set, mark as judged.
          querySetJudgments.nowJudged(index);
          i.remove();
        }
      }
    }

    // Remove queries that do not belong to any query set.
    unknownQueries.clear();
  }

  private static final class WeightedChooser<T> {
    private final int[] weightTotals;
    private final List<T> options;

    private WeightedChooser(int[] weightTotals, List<T> options) {
      this.weightTotals = weightTotals;
      this.options = options;
    }

    private T choose(Random rng) {
      int value = rng.nextInt(weightTotals[weightTotals.length - 1]);
      int index = Arrays.binarySearch(weightTotals, value);
      if (index < 0) {
        index = -index - 1;
      } else {
        ++index;
      }
      return options.get(index);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("{weightTotals=").append(Arrays.toString(weightTotals));
      sb.append(",options=").append(options).append('}');
      return sb.toString();
    }
    
    private static final class Builder<T> {
      List<T> options;
      List<Integer> weights;
      
      public Builder() {
        options = new LinkedList<T>();
        weights = new LinkedList<Integer>();
      }
      
      private void add(T option, int weight) {
        options.add(option);
        weights.add(weight);
      }

      private WeightedChooser<T> build() {
        int total = 0;
        int[] weightTotals = new int[weights.size()];
        for (int i = 0; i < weights.size(); ++i) {
          total += weights.get(i);
          weightTotals[i] = total;
        }
        
        return new WeightedChooser<T>(weightTotals,
            ImmutableList.copyOf(options)); 
      }
    }
  }

  /**
   * Attempts to choose a random, unjudged query from among all query sets. If
   * no unjudged queries exist, any query is chosen at random.
   * 
   * @param storageManager the storage manager through which all query sets and
   *        queries are retrieved
   * @return the details of the query chosen
   * @throws SxseStorageException if an error occurs
   */
  public ChosenQueryDetails chooseQuery(StorageManager storageManager)
      throws SxseStorageException {
    // If needed, initialize by reading all user judgments again.
    initialize(storageManager);

    // Now initialized, set queries belonging to unknown query sets as judged.
    setUnknownQueries(storageManager);

    WeightedChooser.Builder<QuerySetJudgments> builder =
        new WeightedChooser.Builder<QuerySetJudgments>();
    boolean addedAny = false;
    QueryStorage queryStorage = storageManager.getQueryStorage();
    for (QuerySetJudgments querySetJudgements : querySetJudgmentList) {
      final int numUnjudged = querySetJudgements.getNumUnjudged();
      if (numUnjudged == 0) {
        // All queries in this query set are already judged, so move on.
        continue;
      } else if (!queryStorage.isActive(querySetJudgements.querySetName)) {
        // This query set is not active, so do not choose queries from it.
        continue;
      }

      // Weight of this query set is equal to how many queries are not judged.
      builder.add(querySetJudgements, numUnjudged);
      addedAny = true;
    }
    if (!addedAny) {
      // The user has evaluated all queries, so choose a random query.
      return new ChosenQueryDetails(RandomQueryChooser.choose(storageManager));
    }

    // Pick a random query set.
    WeightedChooser<QuerySetJudgments> chooser = builder.build();
    QuerySetJudgments querySetJudgments = chooser.choose(SxseServletUtil.RNG);
    // Now pick a random query from the chosen query set.
    return querySetJudgments.chooseQuery(storageManager.getQueryStorage());
  }

  public ChosenQueryDetails chooseQuery(StorageManager storageManager,
      String querySetName) throws SxseStorageException {
    // If needed, initialize by reading all user judgments again.
    initialize(storageManager);

    // Now initialized, set queries belonging to unknown query sets as judged.
    setUnknownQueries(storageManager);

    QuerySetJudgments querySetJudgments = getQuerySetJudgments(querySetName);
    if (querySetJudgments == null) {
      // Set does not exist.
      LOGGER.info("choose could not find query set " + querySetName);
      return null;
    } else if (querySetJudgments.getNumUnjudged() == 0) {
      // The user has evaluated all queries in set, so choose a random query.
      return new ChosenQueryDetails(RandomQueryChooser.choose(storageManager));
    }

    // Pick a random query from the query set.
    return querySetJudgments.chooseQuery(storageManager.getQueryStorage());
  }

  private void initialize(StorageManager storageManager)
      throws SxseStorageException {
    Iterable<JudgmentDetails> allJudgments = null;

    for (QuerySetJudgments querySetJudgements : querySetJudgmentList) {
      if (forceInitAgain || !querySetJudgements.initialized) {
        // Query set is not initialized, so queries not judged are unknown.
        if (allJudgments == null) {
          allJudgments = makeAllJudgments(storageManager);
        }

        // Initialize the query set, noting which queries are not judged.
        querySetJudgements.initialize(storageManager, allJudgments);
      }
    }

    // Do not force re-initialization next time.
    forceInitAgain = false;
  }

  /**
   * @return {@code true} if the state of any query set is maintained,
   *         {@code false} otherwise
   */
  public boolean isEmpty() {
    return querySetJudgmentList.isEmpty();
  }

  /**
   * Invoked when a new query set has been added.
   * 
   * @param setName the name of the new query set
   * @param setSize the size of the new query set
   */
  public void addQuerySet(String setName, int setSize) {
    querySetJudgmentList.add(new QuerySetJudgments(setName, setSize));
  }

  /**
   * Removes the given query set.
   * 
   * @param setName the query set to rename
   */
  public void removeQuerySet(String setName) {
    for (Iterator<QuerySetJudgments> i = querySetJudgmentList.iterator();
        i.hasNext(); ) {
      QuerySetJudgments querySetJudgments = i.next();
      if (setName.equals(querySetJudgments.querySetName)) {
        // Found query set having given name, now remove.
        i.remove();
        break;
      }
    }
  }

  /**
   * Renames the given query set.
   * 
   * @param prevName the query set to rename
   * @param newName the new name of the query set
   */
  public void renameQuerySet(String prevName, String newName) {
    QuerySetJudgments querySetJudgments = getQuerySetJudgments(prevName);
    if (querySetJudgments != null) {
      // Found query set having given name, now rename.
      querySetJudgments.querySetName = newName;
    }
  }

  /**
   * Using the query set and internal bit set index in {@code details}, marks
   * this query as judged.
   * 
   * @param details the details of the query judged
   */
  public void nowJudged(ChosenQueryDetails details) {
    QuerySetJudgments querySetJudgments =
        getQuerySetJudgments(details.querySetName);
    if ((querySetJudgments == null) || !querySetJudgments.initialized) {
      // If could not find judgments, or if uninitialized, queue unknown query.
      nowJudged(details.query);
    } else {
      querySetJudgments.nowJudged(details.bitIndex);
    }
  }

  /*
   * Returns the QuerySetJudgments instance for the given query set name.
   */
  private QuerySetJudgments getQuerySetJudgments(String querySetName) {
    for (QuerySetJudgments querySetJudgments : querySetJudgmentList) {
      if (querySetName.equals(querySetJudgments.querySetName)) {
        return querySetJudgments;
      }
    }
    return null;
  }

  /**
   * Queues this query for marking as judged later. Upon the next call to
   * {@link #chooseQuery(StorageManager)}, when we have access to the storage
   * manager to retrieve all query sets, for each query in the queue we will
   * find what query set it belongs to and what index it occupies in the
   * internal bit set, and mark the query as judged. The queue is then cleared.
   * This should be done after every judgment is made, and thus the queue should
   * never grow too large. This method is invoked when the
   * {@link UnjudgedStorageManager} can not find a {@link ChosenQueryDetails}
   * for this query, typically because it is a query that the user typed in and
   * was not pre-selected from a query set, but not necessarily.
   * 
   * @param query the query judged
   */
  public void nowJudged(String query) {
    if (forceInitAgain) {
      /*
       * We're going to read in all judgments from disk again anyway; will find
       * this query was judged then.
       */
      return;
    }

    unknownQueries.add(query);
    if (unknownQueries.size() > MAX_UNKNOWN_QUERIES) {
      // Too many queries accumulating in memory, force initialization again.
      forceInitAgain = true;
      unknownQueries.clear();
    }
  }

  /**
   * Class for all queries the assessor has judged in a single query set.
   */
  class QuerySetJudgments {
    String querySetName;
    final BitSet unjudged;
    final int numQueries;

    boolean initialized;

    /**
     * Creates a new instance and initializes it, populating the internal bit
     * set and determining which queries are already judged.
     * 
     * @param name the name of the query set
     * @param storageManager the storage manager through which the query set
     *        is retrieved
     * @param allJudgments all judgments made by this user
     * @throws SxseStorageException if an error occurs
     */
    public QuerySetJudgments(String name, StorageManager storageManager,
        Iterable<JudgmentDetails> allJudgments) throws SxseStorageException {
      this.querySetName = name;
      QueryStorage queryStorage = storageManager.getQueryStorage();
      List<String> queries = queryStorage.getQuerySet(name);
      numQueries = queries.size();
      unjudged = new BitSet(numQueries);

      initialize(allJudgments, queries);
    }

    /**
     * Creates a new instance but does not initialize it, therefore the internal
     * bit set is not populated and it cannot be determined which queries are
     * judged.
     * 
     * @param name the name of the query set
     * @param setSize the size of the query set
     */
    public QuerySetJudgments(String name, int setSize) {
      this.querySetName = name;
      numQueries = setSize;
      unjudged = new BitSet(setSize);

      // Initialize later.
      initialized = false;
    }

    /**
     * Initializes this instance, populating the internal bit set and
     * determining which queries are already judged. This is invoked if the
     * constructor {@link #QuerySetJudgments(String, int)} is used, or if
     * re-initialization is forced.
     * 
     * @param storageManager the storage manager through which the query set
     *        is retrieved
     * @param allJudgments all judgments made by this user
     * @throws SxseStorageException if an error occurs
     */
    public void initialize(StorageManager storageManager,
        Iterable<JudgmentDetails> allJudgments) throws SxseStorageException {
      QueryStorage queryStorage = storageManager.getQueryStorage();
      initialize(allJudgments, queryStorage.getQuerySet(querySetName));
    }

    private void initialize(Iterable<JudgmentDetails> allJudgments,
        List<String> queries) {
      // Assume all queries have not been judged initially.
      unjudged.set(0, numQueries);
      for (JudgmentDetails jd : allJudgments) {
        String query = jd.getQuery();
        int index = Collections.binarySearch(queries, query);
        if (index >= 0) {
          // Query is judged, so clear bit.
          unjudged.clear(index);
        }
      }

      // Now initialized.
      initialized = true;
    }

    /**
     * Chooses a random, unjudged query from this query set.
     * 
     * @param acmRng the random number generator
     * @param queryStorage the query storage element through which all queries
     *        in this set are retrieved
     * @return a random, unjudged query
     * @throws SxseStorageException if an error occurs
     */
    public ChosenQueryDetails chooseQuery(QueryStorage queryStorage)
        throws SxseStorageException {
      WeightedChooser.Builder<Integer> builder =
          new WeightedChooser.Builder<Integer>();
      for (int unjudgedIndex = unjudged.nextSetBit(0); unjudgedIndex >= 0;
          unjudgedIndex = unjudged.nextSetBit(unjudgedIndex + 1)) {
        // Find index of every unjudged query.
        builder.add(unjudgedIndex, 1);
      }

      // Pick a random index, and return the associated unjudged query.
      WeightedChooser<Integer> chooser = builder.build();
      int unjudgedIndex = chooser.choose(SxseServletUtil.RNG);
      List<String> queries = queryStorage.getQuerySet(querySetName);
      String query = queries.get(unjudgedIndex);
      return new ChosenQueryDetails(query, querySetName, unjudgedIndex);
    }

    /**
     * @return the number of unjudged queries in this query set
     */
    public int getNumUnjudged() {
      return unjudged.cardinality();
    }

    /**
     * Sets the query corresponding to the given index of the internal bit
     * set as judged.
     * 
     * @param index the index of the judged query
     */
    public void nowJudged(int index) {
      unjudged.clear(index);
    }
  }
}
