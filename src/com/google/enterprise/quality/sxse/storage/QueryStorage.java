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

import java.util.List;
import java.util.Set;
import java.util.SortedSet;

/**
 * Interface for reading and writing sets of queries that users judge, and the
 * preferences of the side-by-side evaluation tool related to queries.
 */
public interface QueryStorage {
  /**
   * @return {@code true} if queries that the assessor has not yet judged are
   *         selected first, {@code false} if random
   * @throws SxseStorageException if an error occurs
   */
  public boolean isPreferringUnjudged() throws SxseStorageException;

  /**
   * Sets whether queries that the assessor has not yet judged are selected
   * first.
   * 
   * @param preferUnjudged {@code true} if queries not yet judged are selected
   *        first, {@code false} if random
   * @throws SxseStorageException if an error occurs
   */
  public void setPreferringUnjudged(boolean preferUnjudged)
      throws SxseStorageException;

  /**
   * @return {@code true} if the assessor can choose what query set to select
   *         the next query from, {@code false} otherwise
   * @throws SxseStorageException if an error occurs
   */
  public boolean isShowingQuerySets() throws SxseStorageException;

  /**
   * Sets whether the assessor can choose what query set to select the next
   * query from for judgment.
   * 
   * @param showQuerySets {@code true} if the assessor can choose what query set
   *        to select the next query from, {@code false} otherwise
   * @throws SxseStorageException if an error occurs
   */
  public void setShowingQuerySets(boolean showQuerySets)
      throws SxseStorageException;

  /**
   * Creates a new query set having the given name and set of queries. If the
   * query set already exists, this method returns {@code false} and has no
   * effect.
   * 
   * @param setName the name of the query set to create
   * @param queries the set of queries
   * @return {@code true} if a new set is created successfully, {@code false} if
   *         the set already exists
   * @throws SxseStorageException if an error occurs
   */
  public boolean addQuerySet(String setName, SortedSet<String> queries)
      throws SxseStorageException;

  /**
   * Renames the given query set. For this operation to succeed,
   * {@code prevName} must be the name of an existing query set and no query set
   * titled {@code newName} must exist if different from {@code prevName}.
   * Performing this operation may be significantly more efficient than calling
   * {@code removeQuerySet} followed by {@code addQuerySet}, depending on the
   * backing implementation.
   * 
   * @param prevName the name of the query set to rename
   * @param newName the new name for the query set
   * @return {@code true} if successfully renamed, {@code false} otherwise
   * @throws SxseStorageException if an error occurs
   */
  public boolean renameQuerySet(String prevName, String newName)
      throws SxseStorageException;

  /**
   * Removes the query set having the given name.
   * 
   * @param setName the name of the query set to remove
   * @return {@code true} if the query set existed and is now removed,
   *         {@code false} if the query set did not exist
   * @throws SxseStorageException if an error occurs
   */
  public boolean removeQuerySet(String setName) throws SxseStorageException;

  /**
   * @return an immutable set of all query set names
   * @throws SxseStorageException if an error occurs
   */
  public Set<String> getQuerySetNames() throws SxseStorageException;

  /**
   * Returns an immutable list containing all the queries belonging to the
   * specified set.
   * 
   * @param setName the name of the set to retrieve queries for
   * @return a list containing all queries
   * @throws SxseStorageException if an error occurs
   */
  public List<String> getQuerySet(String setName) throws SxseStorageException;

  /**
   * Returns the number of queries belonging to the specified set. Performing
   * this operation may be significantly more efficient than calling the
   * {@code size} method on the list returned by {@link #getQuerySet(String)}.
   * 
   * @param setName the query set to return the size of
   * @return the size
   * @throws SxseStorageException if an error occurs
   */
  public int getQuerySetSize(String setName) throws SxseStorageException;

  /**
   * Returns whether queries for judgment should be chosen from the query set
   * having the given name.
   * 
   * @param setName the query set to query as active or not
   * @return {@code true} if queries can be chosen from the query set,
   *         {@code false} otherwise
   * @throws SxseStorageException if an error occurs
   */
  public boolean isActive(String setName) throws SxseStorageException;

  /**
   * Sets whether queries for judgment should be chosen from the query set
   * having the given name.
   * 
   * @param setName the query set to set as active or not
   * @param isActive {@code true} if queries can be chosen from the query set,
   *        {@code false} otherwise
   * @return {@code true} if a query set having the given name was found and set
   *         as active or inactive, {@code false} otherwise
   * @throws SxseStorageException if an error occurs
   */
  public boolean setActive(String setName, boolean isActive)
      throws SxseStorageException;
}
