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

import com.google.common.base.Preconditions;
import com.google.enterprise.quality.sxse.JudgmentDetails;
import com.google.enterprise.quality.sxse.ScoringPolicyProfile;
import com.google.enterprise.quality.sxse.SearchResult;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

/**
 * A wrapper class for {@link StorageManager} instances such that the methods of
 * their storage elements are thread-safe. In order to guarantee thread-safe
 * access, it is required that all access to a backing storage element is done
 * through the created storage manager. A storage element is made thread-safe by
 * wrapping it in another object implementing the appropriate storage element
 * interface. In the wrapping implementation, each method of this interface
 * simply delegates to the wrapped object, but is also declared as
 * {@code synchronized}. Thus, for a thread to hold the lock for a storage
 * element, it can simply uses the idiom of using it in a {@code synchronized}
 * block.
 */
public final class SynchronizedStorageManager implements StorageManager {
  private final StorageManager storageManager;
  private final JudgmentStorage judgmentStorage;
  private final PreferencesStorage prefStorage;
  private final QueryStorage queryStorage;

  /**
   * Creates a thread-safe storage manager backed by the specified storage
   * manager, where all four storage elements -- {@link JudgmentStorage},
   * {@link PreferencesStorage}, and {@link QueryStorage} -- are made
   * thread-safe.
   * 
   * @param storageManager the storage manager to wrap
   */
  public SynchronizedStorageManager(StorageManager storageManager) {
    this(storageManager, true, true, true);
  }

  /**
   * Creates a thread-safe storage manager backed by the specified storage
   * manager. Only the specified storage elements are made thread-safe.
   * 
   * @param storageManager the storage manager to wrap
   * @param syncJudgment {@code true} if the judgment storage element should be
   *        made thread-safe, {@code false} otherwise
   * @param syncPrefs {@code true} if the preferences storage element should be
   *        made thread-safe, {@code false} otherwise
   * @param syncQuery {@code true} if the query storage element should be made
   *        thread-safe, {@code false} otherwise
   */
  public SynchronizedStorageManager(StorageManager storageManager,
      boolean syncJudgment, boolean syncPrefs, boolean syncQuery) {
    if (storageManager == null) {
      throw new NullPointerException("storageManager is null");
    }

    this.storageManager = storageManager;
    judgmentStorage = syncJudgment ?
        new SynchronizedJudgmentStorage(
            this.storageManager.getJudgmentStorage()) :
        this.storageManager.getJudgmentStorage();
    prefStorage = syncPrefs ?
        new SynchronizedPreferencesStorage(
            this.storageManager.getPreferencesStorage()) :
        this.storageManager.getPreferencesStorage();
    queryStorage = syncQuery ?
        new SynchronizedQueryStorage(this.storageManager.getQueryStorage()) :
        this.storageManager.getQueryStorage();
  }

  public File getRootDirectory() {
    // Don't synchronize, should be atomic and immutable.
    return storageManager.getRootDirectory();
  }

  public JudgmentStorage getJudgmentStorage() {
    return judgmentStorage;
  }

  public PreferencesStorage getPreferencesStorage() {
    return prefStorage;
  }

  public QueryStorage getQueryStorage() {
    return queryStorage;
  }

  public void tryDeleteAll() {
    /*
     * We don't need to synchronize on this, as deleting should not happening
     * while SxSE is in use, anyway. If we ever do, simply acquire locks on
     * prefStorage, userStorage, queryStorage, and resultStorage before
     * invoking.
     */
    storageManager.tryDeleteAll();
  }

  /**
   * Wrapper for a {@link PreferencesStorage} instance that provides
   * synchronization.
   */
  private static final class SynchronizedPreferencesStorage implements
      PreferencesStorage {
    private final PreferencesStorage wrappedPrefStorage;

    private SynchronizedPreferencesStorage(
        PreferencesStorage prefsStorage) {
      this.wrappedPrefStorage = Preconditions.checkNotNull(prefsStorage,
          "PreferencesStorage is null");
    }

    public synchronized boolean addProfile(ScoringPolicyProfile spp)
        throws SxseStorageException {
      return wrappedPrefStorage.addProfile(spp);
    }

    public synchronized ScoringPolicyProfile getFirstProfile()
        throws SxseStorageException {
      return wrappedPrefStorage.getFirstProfile();
    }

    public synchronized String getPasswordHint() throws SxseStorageException {
      return wrappedPrefStorage.getPasswordHint();
    }

    public synchronized ScoringPolicyProfile getProfile(String name)
        throws SxseStorageException {
      return wrappedPrefStorage.getProfile(name);
    }

    public synchronized List<ScoringPolicyProfile> getProfiles()
        throws SxseStorageException {
      return wrappedPrefStorage.getProfiles();
    }

    public synchronized ScoringPolicyProfile getSecondProfile()
        throws SxseStorageException {
      return wrappedPrefStorage.getSecondProfile();
    }

    public synchronized Set<String> getAdministrators()
        throws SxseStorageException {
      return wrappedPrefStorage.getAdministrators();
    }

    public synchronized boolean isPasswordCorrect(String password)
        throws SxseStorageException {
      return wrappedPrefStorage.isPasswordCorrect(password);
    }

    public synchronized boolean removeProfile(String name)
        throws SxseStorageException {
      return wrappedPrefStorage.removeProfile(name);
    }

    public synchronized boolean setFirstProfile(String name)
        throws SxseStorageException {
      return wrappedPrefStorage.setFirstProfile(name);
    }

    public synchronized void setNewPassword(String password)
        throws SxseStorageException {
      wrappedPrefStorage.setNewPassword(password);
    }

    public synchronized void setPasswordHint(String passwordHint)
        throws SxseStorageException {
      wrappedPrefStorage.setPasswordHint(passwordHint);
    }

    public synchronized boolean setSecondProfile(String name)
        throws SxseStorageException {
      return wrappedPrefStorage.setSecondProfile(name);
    }

    public synchronized void setAdministrators(Set<String> administators)
        throws SxseStorageException {
      wrappedPrefStorage.setAdministrators(administators);
    }
  }

  /**
   * Wrapper for a {@link JudgmentStorage} instance that provides synchronization.
   */
  private static final class SynchronizedJudgmentStorage
      implements JudgmentStorage {
    private final JudgmentStorage wrappedJudgmentStorage;

    private SynchronizedJudgmentStorage(JudgmentStorage judgmentStorage) {
      this.wrappedJudgmentStorage = Preconditions.checkNotNull(judgmentStorage,
          "JudgmentStorage is null");
    }

    public synchronized JudgmentDetails addJudgment(String userName,
        JudgmentDetails judgment, List<SearchResult> firstResults,
        List<SearchResult> secondResults) throws SxseStorageException {
      return wrappedJudgmentStorage.addJudgment(userName, judgment,
          firstResults, secondResults);
    }

    public synchronized List<JudgmentDetails> getJudgments(String user)
        throws SxseStorageException {
      return wrappedJudgmentStorage.getJudgments(user);
    }

    public synchronized List<JudgmentDetails> getJudgments(String user,
        String query) throws SxseStorageException {
      return wrappedJudgmentStorage.getJudgments(user, query);
    }

    public synchronized int getMaxResults() throws SxseStorageException {
      return wrappedJudgmentStorage.getMaxResults();
    }

    public synchronized int getResultRetrievalTimeout()
        throws SxseStorageException {
      return wrappedJudgmentStorage.getResultRetrievalTimeout();
    }

    public synchronized boolean getResults(String resultsId,
        List<SearchResult> firstResults, List<SearchResult> secondResults)
        throws SxseStorageException {
      return wrappedJudgmentStorage.getResults(resultsId, firstResults,
          secondResults);
    }

    public synchronized Set<String> getUsers() throws SxseStorageException {
      return wrappedJudgmentStorage.getUsers();
    }

    public synchronized boolean hasResult(String resultsId)
        throws SxseStorageException {
      return wrappedJudgmentStorage.hasResult(resultsId);
    }

    public synchronized boolean isRandomSwapping()
        throws SxseStorageException {
      return wrappedJudgmentStorage.isRandomSwapping();
    }

    public synchronized boolean isStoringResults()
        throws SxseStorageException {
      return wrappedJudgmentStorage.isStoringResults();
    }

    public synchronized boolean isSubmittingAutomatically()
        throws SxseStorageException {
      return wrappedJudgmentStorage.isSubmittingAutomatically();
    }

    public synchronized boolean removeUsers(Set<String> users)
        throws SxseStorageException {
      return wrappedJudgmentStorage.removeUsers(users);
    }

    public synchronized void setMaxResults(int maxResults)
        throws SxseStorageException {
      wrappedJudgmentStorage.setMaxResults(maxResults);
    }

    public synchronized void setRandomSwapping(boolean random)
        throws SxseStorageException {
      wrappedJudgmentStorage.setRandomSwapping(random);
    }

    public synchronized void setResultRetrievalTimeout(int timeout)
        throws SxseStorageException {
      wrappedJudgmentStorage.setResultRetrievalTimeout(timeout);
    }

    public synchronized void setStoringResults(boolean store)
        throws SxseStorageException {
      wrappedJudgmentStorage.setStoringResults(store);
    }

    public synchronized void setSubmittingAutomatically(
        boolean submitAutomatically) throws SxseStorageException {
      wrappedJudgmentStorage.setSubmittingAutomatically(submitAutomatically);
    }
  }

  /**
   * Wrapper for a {@link QueryStorage} instance that provides synchronization.
   */
  private static final class SynchronizedQueryStorage implements QueryStorage {
    private final QueryStorage wrappedQueryStorage;

    private SynchronizedQueryStorage(QueryStorage queryStorage) {
      this.wrappedQueryStorage = Preconditions.checkNotNull(queryStorage,
          "QueryStorage is null");
    }

    public synchronized boolean addQuerySet(
        String setName, SortedSet<String> queries) throws SxseStorageException {
      return wrappedQueryStorage.addQuerySet(setName, queries);
    }

    public synchronized boolean renameQuerySet(
        String prevName, String newName) throws SxseStorageException {
      return wrappedQueryStorage.renameQuerySet(prevName, newName);
    }

    public synchronized List<String> getQuerySet(String setName)
        throws SxseStorageException {
      return wrappedQueryStorage.getQuerySet(setName);
    }

    public synchronized Set<String> getQuerySetNames()
        throws SxseStorageException {
      return wrappedQueryStorage.getQuerySetNames();
    }

    public synchronized boolean removeQuerySet(String setName)
        throws SxseStorageException {
      return wrappedQueryStorage.removeQuerySet(setName);
    }

    public synchronized int getQuerySetSize(String setName)
        throws SxseStorageException {
      return wrappedQueryStorage.getQuerySetSize(setName);
    }

    public synchronized boolean isActive(String setName)
        throws SxseStorageException {
      return wrappedQueryStorage.isActive(setName);
    }

    public synchronized boolean setActive(String setName, boolean isActive)
        throws SxseStorageException {
      return wrappedQueryStorage.setActive(setName, isActive);
    }

    public synchronized boolean isPreferringUnjudged()
        throws SxseStorageException {
      return wrappedQueryStorage.isPreferringUnjudged();
    }

    public synchronized void setPreferringUnjudged(boolean preferUnjudged)
        throws SxseStorageException {
      wrappedQueryStorage.setPreferringUnjudged(preferUnjudged);
    }

    public synchronized boolean isShowingQuerySets()
        throws SxseStorageException {
      return wrappedQueryStorage.isShowingQuerySets();
    }

    public synchronized void setShowingQuerySets(boolean showQuerySets)
        throws SxseStorageException {
      wrappedQueryStorage.setShowingQuerySets(showQuerySets);
    }
  }
}
