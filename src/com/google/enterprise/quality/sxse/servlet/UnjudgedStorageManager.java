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

import com.google.enterprise.quality.sxse.JudgmentDetails;
import com.google.enterprise.quality.sxse.ScoringPolicyProfile;
import com.google.enterprise.quality.sxse.SearchResult;
import com.google.enterprise.quality.sxse.servlet.JudgedQueries.ChosenQueryDetails;
import com.google.enterprise.quality.sxse.storage.JudgmentStorage;
import com.google.enterprise.quality.sxse.storage.PreferencesStorage;
import com.google.enterprise.quality.sxse.storage.QueryStorage;
import com.google.enterprise.quality.sxse.storage.StorageManager;
import com.google.enterprise.quality.sxse.storage.SxseStorageException;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * An implementation of {@link StorageManager} that encompasses a
 * {@link QueryChooser} that selects queries from all available query sets that
 * the user has not yet judged. When the user has judged all available queries,
 * the behavior of the query chooser is equivalent to that of
 * {@link RandomQueryChooser}.
 */
public class UnjudgedStorageManager implements StorageManager {
  private final StorageManager wrappedStorageManager;

  private static final class ProfilePair {
    final ScoringPolicyProfile firstProfile;
    final ScoringPolicyProfile secondProfile;
    
    ProfilePair(ScoringPolicyProfile firstProfile,
        ScoringPolicyProfile secondProfile) {
      this.firstProfile = firstProfile;
      this.secondProfile = secondProfile;
    }
  }

  private final UniquePreferencesStorage prefsStorage;
  private final UniqueQueryStorage queryStorage;
  private final UniqueJudgmentStorage judgmentStorage;

  // The maximum number of assessors for which we cache what queries are judged.
  private static final int MAX_CACHED_ASSESSORS = 20;

  /*
   * Use a lock for judgedQueriesMap and queryIndexMap, so they are safe for use
   * by multiple threads accessing the inner class instances, which are the
   * storage elements returned by this storage manager.
   */
  private final Object sharedDataLock;
  private final Map<String, JudgedQueries> judgedQueriesMap;
  private final Map<String, ChosenQueryDetails> queryIndexMap;

  private final QueryChooser queryChooser;

  public UnjudgedStorageManager(StorageManager storageManager) {
    this.wrappedStorageManager = storageManager;

    prefsStorage = new UniquePreferencesStorage(
        this.wrappedStorageManager.getPreferencesStorage());
    queryStorage = new UniqueQueryStorage(
        this.wrappedStorageManager.getQueryStorage());
    judgmentStorage = new UniqueJudgmentStorage(
        this.wrappedStorageManager.getJudgmentStorage());

    sharedDataLock = new Object();
    judgedQueriesMap = new LinkedHashMap<String, JudgedQueries>() {
      @Override
      protected boolean removeEldestEntry(
          Map.Entry<String, JudgedQueries> eldest) {
        return (size() > MAX_CACHED_ASSESSORS);
      }
    };
    queryIndexMap = new LinkedHashMap<String, ChosenQueryDetails>() {
      @Override
      protected boolean removeEldestEntry(
          Map.Entry<String, ChosenQueryDetails> eldest) {
        return (size() > MAX_CACHED_ASSESSORS);
      }
    };
    queryChooser = new UniqueQueryChooser();
  }

  public PreferencesStorage getPreferencesStorage() {
    return prefsStorage;
  }

  public QueryStorage getQueryStorage() {
    return queryStorage;
  }

  public JudgmentStorage getJudgmentStorage() {
    return judgmentStorage;
  }

  public File getRootDirectory() {
    return wrappedStorageManager.getRootDirectory();
  }

  public void tryDeleteAll() {
    wrappedStorageManager.tryDeleteAll();
  }

  /**
   * Returns the {@link QueryChooser} that selects queries the user has not yet
   * judged.
   * 
   * @return the query chooser
   */
  public QueryChooser getQueryChooser() {
    return queryChooser;
  }

  private class UniquePreferencesStorage implements PreferencesStorage {
    private final PreferencesStorage wrappedPrefsStorage;

    public UniquePreferencesStorage(PreferencesStorage prefsStorage) {
      this.wrappedPrefsStorage = prefsStorage;
    }

    public boolean addProfile(ScoringPolicyProfile spp)
        throws SxseStorageException {
      ProfilePair profilePair = makeProfilePair();
      if (wrappedPrefsStorage.addProfile(spp)) {
        compareProfiles(profilePair);
        return true;
      }
      return false;
    }

    public String getPasswordHint() throws SxseStorageException {
      return wrappedPrefsStorage.getPasswordHint();
    }

    public ScoringPolicyProfile getProfile(String name)
        throws SxseStorageException {
      return wrappedPrefsStorage.getProfile(name);
    }

    public List<ScoringPolicyProfile> getProfiles()
        throws SxseStorageException {
      return wrappedPrefsStorage.getProfiles();
    }

    public ScoringPolicyProfile getFirstProfile() throws SxseStorageException {
      return wrappedPrefsStorage.getFirstProfile();
    }

    public ScoringPolicyProfile getSecondProfile() throws SxseStorageException {
      return wrappedPrefsStorage.getSecondProfile();
    }

    public boolean isPasswordCorrect(String password)
        throws SxseStorageException {
      return wrappedPrefsStorage.isPasswordCorrect(password);
    }

    public boolean removeProfile(String name) throws SxseStorageException {
      ProfilePair profilePair = makeProfilePair();
      if (wrappedPrefsStorage.removeProfile(name)) {
        compareProfiles(profilePair);
        return true;
      }
      return false;
    }

    public boolean setFirstProfile(String name) throws SxseStorageException {
      ProfilePair profilePair = makeProfilePair();
      if (wrappedPrefsStorage.setFirstProfile(name)) {
        compareProfiles(profilePair);
        return true;
      }
      return false;
    }

    public void setNewPassword(String password) throws SxseStorageException {
      wrappedPrefsStorage.setNewPassword(password);
    }

    public void setPasswordHint(String passwordHint)
        throws SxseStorageException {
      wrappedPrefsStorage.setPasswordHint(passwordHint);
    }

    public boolean setSecondProfile(String name) throws SxseStorageException {
      ProfilePair profilePair = makeProfilePair();
      if (wrappedPrefsStorage.setSecondProfile(name)) {
        compareProfiles(profilePair);
        return true;
      }
      return false;
    }

    public void setAdministrators(Set<String> administrators)
        throws SxseStorageException {
      wrappedPrefsStorage.setAdministrators(administrators);
    }

    public Set<String> getAdministrators() throws SxseStorageException {
      return wrappedPrefsStorage.getAdministrators();
    }

    private ProfilePair makeProfilePair() throws SxseStorageException {
      return new ProfilePair(wrappedPrefsStorage.getFirstProfile(),
          wrappedPrefsStorage.getSecondProfile());
    }

    private void compareProfiles(ProfilePair currProfilePair)
        throws SxseStorageException {
      ProfilePair newProfilePair = makeProfilePair();
      if (!currProfilePair.equals(newProfilePair)) {
        synchronized (sharedDataLock) {
          // Profiles changed, so all state for judged queries is invalid.
          judgedQueriesMap.clear();
          queryIndexMap.clear();
        }
      }
    }
  }

  private class UniqueQueryStorage implements QueryStorage {
    private final QueryStorage wrappedQueryStorage;

    private UniqueQueryStorage(QueryStorage queryStorage) {
      this.wrappedQueryStorage = queryStorage;
    }

    public boolean addQuerySet(String setName, SortedSet<String> queries)
        throws SxseStorageException {
      if (!wrappedQueryStorage.addQuerySet(setName, queries)) {
        return false;
      }

      synchronized (sharedDataLock) {
        for (JudgedQueries userJudgments : judgedQueriesMap.values()) {
          userJudgments.addQuerySet(setName, queries.size());
        }
        return true;
      }
    }

    public List<String> getQuerySet(String setName)
        throws SxseStorageException {
      return wrappedQueryStorage.getQuerySet(setName);
    }

    public Set<String> getQuerySetNames() throws SxseStorageException {
      return wrappedQueryStorage.getQuerySetNames();
    }

    public int getQuerySetSize(String setName) throws SxseStorageException {
      return wrappedQueryStorage.getQuerySetSize(setName);
    }

    public boolean removeQuerySet(String setName) throws SxseStorageException {
      if (!wrappedQueryStorage.removeQuerySet(setName)) {
        return false;
      }

      synchronized (sharedDataLock) {
        Iterator<Map.Entry<String, JudgedQueries>> i =
            judgedQueriesMap.entrySet().iterator();
        while (i.hasNext()) {
          Map.Entry<String, JudgedQueries> entry = i.next();
          JudgedQueries userJudgments = entry.getValue();
          userJudgments.removeQuerySet(setName);

          if (userJudgments.isEmpty()) {
            i.remove();
          }
        }
        return true;
      }
    }

    public boolean renameQuerySet(String prevName, String newName)
        throws SxseStorageException {
      if (!wrappedQueryStorage.renameQuerySet(prevName, newName)) {
        return false;
      }

      synchronized (sharedDataLock) {
        for (JudgedQueries userJudgments : judgedQueriesMap.values()) {
          userJudgments.renameQuerySet(prevName, newName);
        }
        return true;
      }
    }

    public boolean isActive(String setName) throws SxseStorageException {
      return wrappedQueryStorage.isActive(setName);
    }

    public boolean setActive(String setName, boolean isActive)
        throws SxseStorageException {
      return wrappedQueryStorage.setActive(setName, isActive);
    }

    public boolean isPreferringUnjudged() throws SxseStorageException {
      return wrappedQueryStorage.isPreferringUnjudged();
    }

    public void setPreferringUnjudged(boolean preferUnjudged)
        throws SxseStorageException {
      wrappedQueryStorage.setPreferringUnjudged(preferUnjudged);

      boolean preferJudged = !preferUnjudged;
      if (preferJudged) {
        synchronized (sharedDataLock) {
          // Clear all data, admin turned off preferring unjudged queries.
          judgedQueriesMap.clear();
          queryIndexMap.clear();
        }
      }
    }

    public boolean isShowingQuerySets() throws SxseStorageException {
      return wrappedQueryStorage.isShowingQuerySets();
    }

    public void setShowingQuerySets(boolean showQuerySets)
        throws SxseStorageException {
      wrappedQueryStorage.setShowingQuerySets(showQuerySets);
    }
  }

  private class UniqueJudgmentStorage implements JudgmentStorage {
    private final JudgmentStorage wrappedJudgmentStorage;

    private UniqueJudgmentStorage(JudgmentStorage judgmentStorage) {
      this.wrappedJudgmentStorage = judgmentStorage;
    }

    public JudgmentDetails addJudgment(String userName,
        JudgmentDetails judgment, List<SearchResult> firstResults,
        List<SearchResult> secondResults) throws SxseStorageException {
      JudgmentDetails updatedDetails = wrappedJudgmentStorage.addJudgment(
          userName, judgment, firstResults, secondResults);

      synchronized (sharedDataLock) {
        String judgedQuery = judgment.getQuery();
        JudgedQueries userJudgments = judgedQueriesMap.get(userName);
        if (userJudgments == null) {
          // Will populate judgments when user selects a query.
          return updatedDetails;
        }

        // Set query as judged.
        ChosenQueryDetails queryDetails = queryIndexMap.remove(userName);
        if ((queryDetails != null) && judgedQuery.equals(queryDetails.query)) {
          userJudgments.nowJudged(queryDetails);
        } else {
          userJudgments.nowJudged(judgedQuery);
        }
      }
      return updatedDetails;
    }

    public List<JudgmentDetails> getJudgments(String user)
        throws SxseStorageException {
      return wrappedJudgmentStorage.getJudgments(user);
    }

    public List<JudgmentDetails> getJudgments(String user, String query)
        throws SxseStorageException {
      return wrappedJudgmentStorage.getJudgments(user, query);
    }

    public int getMaxResults() throws SxseStorageException {
      return wrappedJudgmentStorage.getMaxResults();
    }

    public int getResultRetrievalTimeout() throws SxseStorageException {
      return wrappedJudgmentStorage.getResultRetrievalTimeout();
    }

    public boolean getResults(String resultsId,
        List<SearchResult> firstResults, List<SearchResult> secondResults)
        throws SxseStorageException {
      return wrappedJudgmentStorage.getResults(
          resultsId, firstResults, secondResults);
    }

    public Set<String> getUsers() throws SxseStorageException {
      return wrappedJudgmentStorage.getUsers();
    }

    public boolean hasResult(String resultsId) throws SxseStorageException {
      return wrappedJudgmentStorage.hasResult(resultsId);
    }

    public boolean isRandomSwapping() throws SxseStorageException {
      return wrappedJudgmentStorage.isRandomSwapping();
    }

    public boolean isStoringResults() throws SxseStorageException {
      return wrappedJudgmentStorage.isStoringResults();
    }

    public boolean isSubmittingAutomatically() throws SxseStorageException {
      return wrappedJudgmentStorage.isSubmittingAutomatically();
    }

    public boolean removeUsers(Set<String> users) throws SxseStorageException {
      if (!wrappedJudgmentStorage.removeUsers(users)) {
        return false;
      }

      synchronized (sharedDataLock) {
        for (String user : users) {
          judgedQueriesMap.remove(user);
          queryIndexMap.remove(user);
        }
      }
      return true;
    }

    public void setMaxResults(int maxResults) throws SxseStorageException {
      wrappedJudgmentStorage.setMaxResults(maxResults);
    }

    public void setRandomSwapping(boolean random) throws SxseStorageException {
      wrappedJudgmentStorage.setRandomSwapping(random);
    }

    public void setResultRetrievalTimeout(int timeout)
        throws SxseStorageException {
      wrappedJudgmentStorage.setResultRetrievalTimeout(timeout);
    }

    public void setStoringResults(boolean store) throws SxseStorageException {
      wrappedJudgmentStorage.setStoringResults(store);
    }

    public void setSubmittingAutomatically(boolean submitAutomatically)
        throws SxseStorageException {
      wrappedJudgmentStorage.setSubmittingAutomatically(submitAutomatically);
    }
  }
  
  /*
   * Implementation of QueryChooser returned to clients. This implementation
   * must not acquire the lock sharedDataLock, and then call a method any
   * method on storageManager. This can result in deadlock if the
   * storageManager wraps this UnjudgedStorageManager instance and is of type
   * SynchronizedStorageManager.
   */
  private final class UniqueQueryChooser implements QueryChooser {
    public String choose(StorageManager storageManager, User user)
        throws SxseStorageException {
      final String userName = user.getAssessorName();
      JudgmentStorage judgmentStorage = storageManager.getJudgmentStorage();
      if (!judgmentStorage.getUsers().contains(userName)) {
        // User has not yet made any judgments, so don't try to retrieve them.
        return RandomQueryChooser.choose(storageManager);
      }

      JudgedQueries userJudgments =
          getUserJudgments(storageManager, userName);
      ChosenQueryDetails chosenQuery =
          userJudgments.chooseQuery(storageManager);
      updateSharedData(userName, userJudgments, chosenQuery);
      return chosenQuery.query;
    }

    public String choose(StorageManager storageManager, User user,
        String querySetName) throws SxseStorageException {
      final String userName = user.getAssessorName();
      JudgmentStorage judgmentStorage = storageManager.getJudgmentStorage();
      if (!judgmentStorage.getUsers().contains(userName)) {
        // User has not yet made any judgments, so don't try to retrieve them.
        return RandomQueryChooser.choose(storageManager, querySetName);
      }

      JudgedQueries userJudgments =
        getUserJudgments(storageManager, userName);
      ChosenQueryDetails chosenQuery =
          userJudgments.chooseQuery(storageManager, querySetName);
      updateSharedData(userName, userJudgments, chosenQuery);
      return chosenQuery.query;
    }

    private void updateSharedData(String userName, JudgedQueries userJudgments,
        ChosenQueryDetails chosenQuery) {
      // Try to update both maps in a single transaction.
      synchronized (sharedDataLock) {
        // Save judged queries for next call.
        judgedQueriesMap.put(userName, userJudgments);
        if (chosenQuery.detailsKnown()) {
          // Cache details of query for when its judgment is issued.
          queryIndexMap.put(userName, chosenQuery);
        }
      }
    }

    private JudgedQueries getUserJudgments(
        StorageManager storageManager, String userName)
        throws SxseStorageException {
      JudgedQueries userJudgments = null;
      synchronized (sharedDataLock) {
        judgedQueriesMap.get(userName);
      }

      if (userJudgments == null) {
        // Judged queries for user are not in memory, populate now.
        userJudgments = new JudgedQueries(userName, storageManager);
      }
      return userJudgments;
    }
  }
}
