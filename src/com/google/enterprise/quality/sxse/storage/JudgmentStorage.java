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

import com.google.enterprise.quality.sxse.JudgmentDetails;
import com.google.enterprise.quality.sxse.SearchResult;

import java.util.List;
import java.util.Set;

/**
 * Interface for reading and writing the judgments of users.
 */
public interface JudgmentStorage {
  /**
   * @return {@code true} if search results should be saved with each judgment
   *         made by the assessor, {@code false} otherwise
   * @throws SxseStorageException if an error occurs
   */
  public boolean isStoringResults() throws SxseStorageException;

  /**
   * Sets whether the host running SxSE will save the search results along with
   * each judgment made by the assessor.
   * 
   * @param store {@code true} if search results should be saved with each
   *        judgment made, {@code false} otherwise}
   * @throws SxseStorageException if an error occurs
   */
  public void setStoringResults(boolean store) throws SxseStorageException;

  /**
   * @return the maximum number of results returned by each policy for judgment
   *         by the assessor
   * @throws SxseStorageException if an error occurs
   */
  public int getMaxResults() throws SxseStorageException;

  /**
   * Sets the maximum number of results returned by each policy for judgment by
   * the assessor.
   * 
   * @param maxResults the maximum number of results each policy should return
   * @throws SxseStorageException if an error occurs
   */
  public void setMaxResults(int maxResults) throws SxseStorageException;

  /**
   * @return {@code true} if all results may be randomly swapped between
   *         policies A and B when displayed to the assessor, {@code false}
   *         otherwise
   * @throws SxseStorageException if an error occurs
   */
  public boolean isRandomSwapping() throws SxseStorageException;

  /**
   * Sets whether all results may be randomly swapped between policies A and B
   * when displayed to the assessor.
   * 
   * @param random {@code true} if all results may be randomly swapped between
   *        policies A and B, {@code false} otherwise
   * @throws SxseStorageException if an error occurs
   */
  public void setRandomSwapping(boolean random) throws SxseStorageException;

  /**
   * @return {@code true} if the judgment should be submitted automatically for
   *         equal results, {@code false} otherwise
   * @throws SxseStorageException if an error occurs
   */
  public boolean isSubmittingAutomatically() throws SxseStorageException;

  /**
   * Sets whether the judgment by the assessor should be submitted automatically
   * for equal results if {@link #isStoringResults()} returns {@code true}.
   * 
   * @param submitAutomatically {@code true} if the judgment should be submitted
   *        automatically for equal results, {@code false} otherwise
   * @throws SxseStorageException if an error occurs
   */
  public void setSubmittingAutomatically(boolean submitAutomatically)
      throws SxseStorageException;

  /**
   * @return the maximum amount of time, in milliseconds, to wait for results
   * @throws SxseStorageException if an error occurs
   */
  public int getResultRetrievalTimeout() throws SxseStorageException;

  /**
   * Sets the maximum amount of time, in milliseconds, to wait for results from
   * a host if {@link #isStoringResults()} returns {@code true}.
   * 
   * @param timeoutInMillis the maximum amount of time, in milliseconds, to
   *        wait for results
   * @throws SxseStorageException if an error occurs
   */
  public void setResultRetrievalTimeout(int timeoutInMillis)
      throws SxseStorageException;

  /**
   * @return an immutable list of all users
   * @throws SxseStorageException if an exception occurred while returning all
   *         users
   */
  public Set<String> getUsers() throws SxseStorageException;

  /**
   * Removes the judgments of the given users from storage.
   * 
   * @param users the users to remove
   * @return {@code true} if the user was in storage and is now removed,
   *         {@code false} if the user was not in storage
   * @throws SxseStorageException if an error occurs
   */
  public boolean removeUsers(Set<String> users) throws SxseStorageException;

  /**
   * Appends the given judgment to the list of judgments made by this user.
   * 
   * @param userName the user who made the judgment
   * @param judgment the judgment details
   * @param firstResults the first list of search results, or {@code null} if
   *        the results are unknown
   * @param secondResults the second list of search results, or {@code null} if
   *        the results are unknown
   * @return the {@link JudgmentDetails} that includes the identifier of the
   *         results if {@code firstResults} and {@code secondResults} are not
   *         null
   * @throws SxseStorageException if an error occurs
   */
  public JudgmentDetails addJudgment(String userName, JudgmentDetails judgment,
      List<SearchResult> firstResults, List<SearchResult> secondResults)
      throws SxseStorageException;

  /**
   * Returns an immutable list of all judgments made by this user.
   * 
   * @param user the user to retrieve all judgments for
   * @return a list containing all judgments made by this user
   * @throws SxseStorageException if an error occurs
   */
  public List<JudgmentDetails> getJudgments(String user)
      throws SxseStorageException;

  /**
   * Returns an immutable list of all judgments made by this user for the given
   * query.
   * 
   * @param user the user to retrieve all judgments for
   * @param query the query to retrieve all judgments for
   * @return a list containing all judgments made by this user
   * @throws SxseStorageException if an error occurs
   */
  public List<JudgmentDetails> getJudgments(String user, String query)
      throws SxseStorageException;

  /**
   * Populates the given lists with the results having the given identifier. If
   * no such results exist in storage, this method returns {@code false}.
   * 
   * @param resultsId the identifier of the search results to return
   * @param firstResults the first list of search results, which must be empty
   * @param secondResults the second list of search results, which must be empty
   * @return {@code true} if the lists are now populated with results,
   *         {@code false} otherwise
   * @throws SxseStorageException if an error occurs
   */
  public boolean getResults(String resultsId, List<SearchResult> firstResults,
      List<SearchResult> secondResults) throws SxseStorageException;

  /**
   * Returns whether storage contains results having the given identifier.
   * 
   * @param resultsId the identifier of the search results
   * @return {@code true} if search results having the given identifier are in
   *         storage, {@code false} otherwise
   * @throws SxseStorageException if an error occurs
   */
  public boolean hasResult(String resultsId) throws SxseStorageException;
}
