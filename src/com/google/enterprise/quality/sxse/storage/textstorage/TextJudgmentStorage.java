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

package com.google.enterprise.quality.sxse.storage.textstorage;

import com.google.common.collect.Maps;
import com.google.enterprise.quality.sxse.JudgmentDetails;
import com.google.enterprise.quality.sxse.SearchResult;
import com.google.enterprise.quality.sxse.hashers.Hasher;
import com.google.enterprise.quality.sxse.storage.JudgmentStorage;
import com.google.enterprise.quality.sxse.storage.SxseStorageException;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the {@link JudgmentStorage} interface where all data is
 * saved in text format.
 */
public class TextJudgmentStorage implements JudgmentStorage {
  private final TextResultStorage resultStorage;
  private final TextUserStorage userStorage;

  public TextJudgmentStorage(File resultsDir, Hasher resultsHasher,
      File usersDir) throws SxseStorageException {
    HashMap<String, Integer> hashRefCounts = Maps.newHashMap();
    userStorage = new TextUserStorage(
        usersDir, resultsHasher.getHashSize(), hashRefCounts);
    resultStorage = new TextResultStorage(
        resultsDir, resultsHasher, hashRefCounts);
  }

  public boolean isStoringResults() throws SxseStorageException {
    return resultStorage.isStoringResults();
  }

  public void setStoringResults(boolean store) throws SxseStorageException {
    resultStorage.setStoringResults(store);
  }

  public int getMaxResults() throws SxseStorageException {
    return resultStorage.getMaxResults();
  }

  public void setMaxResults(int maxResults) throws SxseStorageException {
    resultStorage.setMaxResults(maxResults);
  }

  public boolean isRandomSwapping() throws SxseStorageException {
    return resultStorage.isRandomSwapping();
  }

  public void setRandomSwapping(boolean random) throws SxseStorageException {
    resultStorage.setRandomSwapping(random);
  }

  public boolean isSubmittingAutomatically() throws SxseStorageException {
    return resultStorage.isSubmittingAutomatically();
  }

  public void setSubmittingAutomatically(boolean submitAutomatically)
      throws SxseStorageException {
    resultStorage.setSubmittingAutomatically(submitAutomatically);
  }

  public int getResultRetrievalTimeout() throws SxseStorageException {
    return resultStorage.getResultRetrievalTimeout();
  }

  public void setResultRetrievalTimeout(int timeout)
      throws SxseStorageException {
    resultStorage.setResultRetrievalTimeout(timeout);
  }

  public boolean hasResult(String resultsId) throws SxseStorageException {
    return resultStorage.hasResult(resultsId);
  }

  public boolean getResults(String resultsId, List<SearchResult> firstResults,
      List<SearchResult> secondResults) throws SxseStorageException {
    return resultStorage.getResults(resultsId, firstResults, secondResults);
  }

  public JudgmentDetails addJudgment(String userName, JudgmentDetails judgment,
      List<SearchResult> firstResults, List<SearchResult> secondResults)
      throws SxseStorageException {
    String resultsId = resultStorage.addResults(firstResults, secondResults);
    return userStorage.addJudgment(userName, judgment, resultsId);
  }

  public List<JudgmentDetails> getJudgments(String user)
      throws SxseStorageException {
    return userStorage.getJudgments(user);
  }

  public List<JudgmentDetails> getJudgments(String user, String query)
      throws SxseStorageException {
    return userStorage.getJudgments(user, query);
  }

  public Set<String> getUsers() throws SxseStorageException {
    return userStorage.getUsers();
  }

  public boolean removeUsers(Set<String> users) throws SxseStorageException {
    Map<String, Integer> removedHashRefCounts = Maps.newHashMap();
    boolean removedAny = userStorage.removeUsers(users, removedHashRefCounts);
    resultStorage.updateHashRefCounts(removedHashRefCounts);
    return removedAny;
  }

  void tryDelete() {
    resultStorage.tryDelete();
    userStorage.tryDelete();
  }
}
