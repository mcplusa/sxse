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

package com.google.enterprise.quality.sxse.storage;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.enterprise.quality.sxse.JudgmentDetails;
import com.google.enterprise.quality.sxse.ScoringPolicyProfile;
import com.google.enterprise.quality.sxse.SearchResult;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * A wrapper class for a {@link StorageManager} instance that provides a
 * write-through cache for data. For its {@link PreferencesStorage} module, if
 * caching is enabled, the two active scoring policy profiles are cached, as are
 * the set of all administrators and the password hint. For its
 * {@link QueryStorage} module, if caching is enabled, up to the specified
 * number of query sets and their corresponding queries are cached, as are all
 * preferences represented as a primitive type. For its {@link JudgmentStorage}
 * module, if caching is enabled, the judgments for up to the specified number
 * of users are cached, and also all preferences represented as a primitive
 * type.
 * 
 * If caching for {@link QueryStorage} is enabled, unless overriden up to
 * {@link #DEFAULT_NUM_CACHED_QUERY_SETS} query sets will be cached. If caching
 * for {@link JudgmentStorage} is enabled, unless overriden judmgents for up to
 * {@link #DEFAULT_NUM_CACHED_USERS} users will be cached.
 */
public class CachingStorageManager implements StorageManager {
  private static final class LruCache<K, V> {
    private final LinkedHashMap<K, V> map;
    
    public LruCache(final int capacity) {
      map = new LinkedHashMap<K, V>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
          return size() > capacity;
        }
      };
    }

    public void put(K key, V value) {
      map.put(key, value);
    }

    public V get(K key) {
      return map.get(key);
    }

    public V remove(K key) {
      return map.remove(key);
    }
  }

  /**
   * The default number of query sets to cache in memory.
   */
  public static final int DEFAULT_NUM_CACHED_QUERY_SETS = 10;

  /**
   * The default number of users to cache in memory.
   */
  public static final int DEFAULT_NUM_CACHED_USERS = 10;

  private final StorageManager storageManager;
  private final JudgmentStorage judgmentStorage;
  private final PreferencesStorage prefsStorage;
  private final QueryStorage queryStorage;

  /**
   * Creates a cache atop the specified storage manager, using the default
   * number of users, query sets, and result hashes to cache in memory.
   * 
   * @param storageManager the storage manager to wrap
   * @throws SxseStorageException if the cache could not initialize from the
   *         backing storage
   */
  public CachingStorageManager(StorageManager storageManager)
      throws SxseStorageException {
    this(storageManager, true, true, true, DEFAULT_NUM_CACHED_QUERY_SETS,
        DEFAULT_NUM_CACHED_USERS);
  }

  /**
   * Creates a cache atop the specified storage manager, using the given
   * number of users, query sets, and result hashes to cache in memory.
   * 
   * @param storageManager the storage manager to wrap
   * @param cachePrefs {@code true} if we should cache 
   * @param queryCount the number of query sets to cache in memory
   * @param userCount the number of users to cache in memory
   * @throws SxseStorageException if the cache could not initialize from the
   *         backing storage
   */
  public CachingStorageManager(StorageManager storageManager,
      boolean cachePrefs, boolean cacheQueries, boolean cacheJudgments,
      int queryCount, int userCount) throws SxseStorageException {
    this.storageManager = storageManager;

    prefsStorage = cachePrefs ? new CachingPreferencesStorage(
        storageManager.getPreferencesStorage()) :
        storageManager.getPreferencesStorage();
    queryStorage = cacheQueries ? new CachingQueryStorage(
        storageManager.getQueryStorage(), queryCount) :
        storageManager.getQueryStorage();
    judgmentStorage = cacheJudgments ?
        new CachingJudgmentStorage(
          storageManager.getJudgmentStorage(), userCount) :
        storageManager.getJudgmentStorage();
  }

  /**
   * Wrapper for a {@link PreferencesStorage} instance that provides caching.
   */
  private static final class CachingPreferencesStorage implements PreferencesStorage {
    private final PreferencesStorage prefsStorage;

    private ScoringPolicyProfile firstProfile;
    private ScoringPolicyProfile secondProfile;
    
    private Set<String> administrators;
    private String passwordHint;

    private CachingPreferencesStorage(PreferencesStorage prefsStorage)
        throws SxseStorageException {
      this.prefsStorage = Preconditions.checkNotNull(prefsStorage,
          "PreferencesStorage must not be null");

      firstProfile = prefsStorage.getFirstProfile();
      secondProfile = prefsStorage.getSecondProfile();

      administrators = prefsStorage.getAdministrators();
      passwordHint = prefsStorage.getPasswordHint();
    }

    public boolean addProfile(ScoringPolicyProfile profile)
        throws SxseStorageException {
      return prefsStorage.addProfile(profile);
    }

    public Set<String> getAdministrators() throws SxseStorageException {
      return administrators;
    }

    public ScoringPolicyProfile getFirstProfile() throws SxseStorageException {
      return firstProfile;
    }

    public String getPasswordHint() throws SxseStorageException {
      return passwordHint;
    }

    public ScoringPolicyProfile getProfile(String name)
        throws SxseStorageException {
      if (Objects.equal(name, firstProfile.getName())) {
        return firstProfile;
      } else if (Objects.equal(name, secondProfile.getName())) {
        return secondProfile;
      }
      return prefsStorage.getProfile(name);
    }

    public List<ScoringPolicyProfile> getProfiles() throws SxseStorageException {
      return prefsStorage.getProfiles();
    }

    public ScoringPolicyProfile getSecondProfile() throws SxseStorageException {
      return secondProfile;
    }

    public boolean isPasswordCorrect(String password)
        throws SxseStorageException {
      return prefsStorage.isPasswordCorrect(password);
    }

    public boolean removeProfile(String name) throws SxseStorageException {
      boolean removedProfile = prefsStorage.removeProfile(name);
      if (removedProfile) {
        if (Objects.equal(firstProfile.getName(), name)) {
          firstProfile = ScoringPolicyProfile.EMPTY_PROFILE;
        }
        if (Objects.equal(secondProfile.getName(), name)) {
          secondProfile = ScoringPolicyProfile.EMPTY_PROFILE;
        }
      }
      return removedProfile;
    }

    public void setAdministrators(Set<String> administrators)
        throws SxseStorageException {
      prefsStorage.setAdministrators(administrators);
      this.administrators = ImmutableSortedSet.copyOf(administrators);
    }

    public boolean setFirstProfile(String name) throws SxseStorageException {
      if (Objects.equal(firstProfile.getName(), name)) {
        return true;
      }

      boolean changedProfile = prefsStorage.setFirstProfile(name);
      if (changedProfile) {
        firstProfile = prefsStorage.getFirstProfile();
      }
      return changedProfile;
    }

    public void setNewPassword(String password) throws SxseStorageException {
      prefsStorage.setNewPassword(password);
    }

    public void setPasswordHint(String passwordHint)
        throws SxseStorageException {
      prefsStorage.setPasswordHint(passwordHint);
      this.passwordHint = passwordHint;
    }

    public boolean setSecondProfile(String name) throws SxseStorageException {
      if (Objects.equal(secondProfile.getName(), name)) {
        return true;
      }

      boolean changedProfile = prefsStorage.setSecondProfile(name);
      if (changedProfile) {
        secondProfile = prefsStorage.getSecondProfile();
      }
      return changedProfile;
    }
  }

  /**
   * Wrapper for a {@link QueryStorage} instance that provides caching.
   */
  private final static class CachingQueryStorage implements QueryStorage {
    private final QueryStorage queryStorage;
    private final LruCache<String, List<String>> cache;

    private boolean isPreferringUnjudged;
    private boolean isShowingQuerySets;

    protected CachingQueryStorage(QueryStorage queryStorage, int capacity)
        throws SxseStorageException {
      this.queryStorage = queryStorage;
      cache = new LruCache<String, List<String>>(capacity);

      isPreferringUnjudged = queryStorage.isPreferringUnjudged();
      isShowingQuerySets = queryStorage.isShowingQuerySets();
    }

    public boolean addQuerySet(String setName, SortedSet<String> queries)
        throws SxseStorageException {
      return queryStorage.addQuerySet(setName, queries);
    }

    public List<String> getQuerySet(String setName)
        throws SxseStorageException {
      List<String> queries = cache.get(setName);
      if (queries != null) {
        // Query sets from cache are immutable, do not copy or wrap them.
        return queries;
      }

      queries = queryStorage.getQuerySet(setName);
      if (queries != null) {
        // Query sets from storage are immutable, do not copy or wrap them.
        cache.put(setName, queries);
      }
      return queries;
    }

    public Set<String> getQuerySetNames()
        throws SxseStorageException {
      return queryStorage.getQuerySetNames();
    }

    public int getQuerySetSize(String setName)
        throws SxseStorageException {
      List<String> cachedQueries = cache.get(setName);
      return (cachedQueries != null) ?
          cachedQueries.size() : queryStorage.getQuerySetSize(setName);
    }

    public boolean removeQuerySet(String setName)
        throws SxseStorageException {
      if (!queryStorage.removeQuerySet(setName)) {
        return false;
      }

      cache.remove(setName);
      return true;
    }

    public boolean renameQuerySet(String prevName, String newName)
        throws SxseStorageException {
      if (!queryStorage.renameQuerySet(prevName, newName)) {
        return false;
      }

      List<String> cachedQueries = cache.remove(prevName);
      if (cachedQueries != null) {
        cache.put(newName, cachedQueries);
      }
      return true;
    }

    public boolean isActive(String setName) throws SxseStorageException {
      return queryStorage.isActive(setName);
    }

    public boolean setActive(String setName, boolean isActive)
        throws SxseStorageException {
      return queryStorage.setActive(setName, isActive);
    }

    public boolean isPreferringUnjudged() throws SxseStorageException {
      return isPreferringUnjudged;
    }

    public void setPreferringUnjudged(boolean preferUnjudged)
        throws SxseStorageException {
      if (isPreferringUnjudged != preferUnjudged) {
        queryStorage.setPreferringUnjudged(preferUnjudged);
        isPreferringUnjudged = preferUnjudged;
      }
    }

    public boolean isShowingQuerySets() throws SxseStorageException {
      return isShowingQuerySets;
    }

    public void setShowingQuerySets(boolean showQuerySets)
        throws SxseStorageException {
      if (isShowingQuerySets != showQuerySets) {
        queryStorage.setShowingQuerySets(showQuerySets);
        isShowingQuerySets = showQuerySets;
      }
    }
  }

  /**
   * Wrapper for a {@link JudgmentStorage} instance that provides caching.
   */
  private static final class CachingJudgmentStorage implements JudgmentStorage {
    /**
     * A {@link Predicate} that returns {@code true} for {@link JudgmentDetails}
     * instances having a given query.
     */
    private static final class QueryFilter implements Predicate<JudgmentDetails> {
      private final String query;

      private QueryFilter(String query) {
        this.query = query;
      }

      public boolean apply(JudgmentDetails judgmentDetails) {
        return query.equals(judgmentDetails.getQuery());
      }
    }

    private final JudgmentStorage judgmentStorage;
    private final Set<String> allUsers;
    private final LruCache<String, List<JudgmentDetails>> judgmentCache;

    private boolean randomSwapping;
    private boolean storingResults;
    private boolean submittingAutomatically;
    private int maxResults;
    private int retrievalTimeout;

    private CachingJudgmentStorage(JudgmentStorage judgmentStorage,
        int judgmentCapacity) throws SxseStorageException {
      this.judgmentStorage = judgmentStorage;
      allUsers = Sets.newTreeSet();
      judgmentCache = new LruCache<String, List<JudgmentDetails>>(judgmentCapacity);

      allUsers.addAll(judgmentStorage.getUsers());
      randomSwapping = judgmentStorage.isRandomSwapping();
      storingResults = judgmentStorage.isStoringResults();
      submittingAutomatically = judgmentStorage.isSubmittingAutomatically();
      maxResults = judgmentStorage.getMaxResults();
      retrievalTimeout = judgmentStorage.getResultRetrievalTimeout();
    }

    public JudgmentDetails addJudgment(
        String userName, JudgmentDetails judgment,
        List<SearchResult> firstResults, List<SearchResult> secondResults)
        throws SxseStorageException {
      JudgmentDetails updatedDetails = judgmentStorage.addJudgment(
          userName, judgment, firstResults, secondResults);
      allUsers.add(userName);
      List<JudgmentDetails> judgmentDetails = judgmentCache.get(userName);
      if (judgmentDetails != null) {
        judgmentDetails.add(updatedDetails);
      }
      return updatedDetails;
    }

    public List<JudgmentDetails> getJudgments(String user)
        throws SxseStorageException {
      List<JudgmentDetails> judgmentDetails = judgmentCache.get(user);
      if (judgmentDetails != null) {
        return ImmutableList.copyOf(judgmentDetails);
      }

      judgmentDetails = judgmentStorage.getJudgments(user);
      if (judgmentDetails != null) {
        // The list from storage is immutable, so copy it to a linked list.
        judgmentCache.put(user, Lists.newLinkedList(judgmentDetails));
      }
      return judgmentDetails;
    }

    public List<JudgmentDetails> getJudgments(String user, String query)
        throws SxseStorageException {
      List<JudgmentDetails> judgmentDetails = judgmentCache.get(user);
      if (judgmentDetails != null) {
        return ImmutableList.copyOf(
            Iterables.filter(judgmentDetails, new QueryFilter(query)));
      }

      // We get all judgments because the user may call this method again soon
      // with a different query.
      judgmentDetails = judgmentStorage.getJudgments(user);
      // The list from storage is immutable, so copy it to a linked list.
      judgmentCache.put(user, Lists.newLinkedList(judgmentDetails));
      return ImmutableList.copyOf(
          Iterables.filter(judgmentDetails, new QueryFilter(query)));
    }

    public boolean hasResult(String resultsId) throws SxseStorageException {
      return judgmentStorage.hasResult(resultsId);
    }

    public boolean getResults(String resultsId,
        List<SearchResult> firstResults, List<SearchResult> secondResults)
        throws SxseStorageException {
      return judgmentStorage.getResults(resultsId, firstResults, secondResults);
    }

    public Set<String> getUsers() throws SxseStorageException {
      return ImmutableSortedSet.copyOf(allUsers);
    }

    public boolean removeUsers(Set<String> users) throws SxseStorageException {
      if (allUsers.removeAll(users)) {
        judgmentStorage.removeUsers(users);
        for (String user : users) {
          judgmentCache.remove(user);
        }
        return true;
      }
      return false;
    }

    public int getMaxResults() throws SxseStorageException {
      return maxResults;
    }

    public int getResultRetrievalTimeout() throws SxseStorageException {
      return retrievalTimeout;
    }

    public boolean isRandomSwapping() throws SxseStorageException {
      return randomSwapping;
    }

    public boolean isStoringResults() throws SxseStorageException {
      return storingResults;
    }

    public boolean isSubmittingAutomatically() throws SxseStorageException {
      return submittingAutomatically;
    }

    public void setMaxResults(int maxResults) throws SxseStorageException {
      if (this.maxResults != maxResults) {
        judgmentStorage.setMaxResults(maxResults);
        this.maxResults = maxResults;
      }
    }

    public void setRandomSwapping(boolean random) throws SxseStorageException {
      if (randomSwapping != random) {
        judgmentStorage.setRandomSwapping(random);
        this.randomSwapping = random;
      }
    }

    public void setResultRetrievalTimeout(int timeout)
        throws SxseStorageException {
      if (retrievalTimeout != timeout) {
        judgmentStorage.setResultRetrievalTimeout(timeout);
        this.retrievalTimeout = timeout;
      }
    }

    public void setStoringResults(boolean store) throws SxseStorageException {
      if (storingResults != store) {
        judgmentStorage.setStoringResults(store);
        this.storingResults = store;
      }
    }

    public void setSubmittingAutomatically(boolean submitAutomatically)
        throws SxseStorageException {
      if (submittingAutomatically != submitAutomatically) {
        judgmentStorage.setSubmittingAutomatically(submitAutomatically);
        this.submittingAutomatically = submitAutomatically;
      }
    }
  }
  
  public File getRootDirectory() {
    return storageManager.getRootDirectory();
  }

  public void tryDeleteAll() {
    storageManager.tryDeleteAll();
  }
  
  public JudgmentStorage getJudgmentStorage() {
    return judgmentStorage;
  }

  public PreferencesStorage getPreferencesStorage() {
    return prefsStorage;
  }

  public QueryStorage getQueryStorage() {
    return queryStorage;
  }
}
