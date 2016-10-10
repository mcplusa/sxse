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

import com.google.common.collect.Lists;
import com.google.enterprise.quality.sxse.storage.QueryStorage;
import com.google.enterprise.quality.sxse.storage.StorageManager;
import com.google.enterprise.quality.sxse.storage.SxseStorageException;

import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

/**
 * An implementation of {@link QueryChooser} that selects a random query from
 * all available query sets without taking into consideration which queries the
 * user has already judged.
 * 
 * An instance delegates any call to {@link #choose(StorageManager, User)} to
 * static method {@link #choose(StorageManager)}. This static method is
 * provided for convenience, allowing any other query chooser implementations to
 * resort to this simple heuristic if necessary.
 */
public class RandomQueryChooser implements QueryChooser {
  private static final Logger LOGGER = Logger.getLogger(
      RandomQueryChooser.class.getName());

  public String choose(StorageManager storageManager, User user)
      throws SxseStorageException {
    // Delegate to the static method.
    return choose(storageManager);
  }

  public String choose(StorageManager storageManager, User user,
      String querySetName) throws SxseStorageException {
    // Delegate to the static method.
    return choose(storageManager, querySetName);
  }

  /**
   * Chooses a random query from the storage manager without taking into
   * consideration whether the query has already been judged or not.
   * 
   * @param storageManager the storage manager through which queries can be
   *        retrieved
   * @return a random query
   * @throws SxseStorageException if an error occurs
   */
  public static String choose(StorageManager storageManager)
      throws SxseStorageException {
    QueryStorage queryStorage = storageManager.getQueryStorage();
    synchronized (queryStorage) {
      Set<String> setNames = queryStorage.getQuerySetNames();
      if (setNames.isEmpty()) {
        // Cannot return a random query if no query sets exist.
        return null;
      }

      Random rng = new Random();
      QuerySetChooser chooser = new QuerySetChooser();
      for (String setName : setNames) {
        if (!queryStorage.isActive(setName)) {
          // Do not choose queries from inactive query sets.
          continue;
        }

        // Add query set name to chooser, using its size as the weight.
        chooser.addQuerySet(setName, queryStorage.getQuerySetSize(setName));
      }
      if (chooser.isEmpty()) {
        // No active query sets were found.
        return null;
      }

      // Select a random query set.
      String setName = chooser.choose(rng);
      // Return a random query from the selected query set.
      List<String> queries = queryStorage.getQuerySet(setName);
      return queries.get(rng.nextInt(queries.size()));
    }
  }

  /**
   * Chooses a random query belonging to the given query set from the storage
   * manager without taking into consideration whether the query has already
   * been judged or not.
   * 
   * @param storageManager the storage manager through which queries can be
   *        retrieved
   * @param querySetName the name of the query set to choose the query from
   * @return a random query from the query set
   * @throws SxseStorageException if an error occurs
   */
  public static String choose(StorageManager storageManager,
      String querySetName) throws SxseStorageException {
    Random random = new Random();

    QueryStorage queryStorage = storageManager.getQueryStorage();
    List<String> queries = queryStorage.getQuerySet(querySetName);
    if ((queries == null) || queries.isEmpty()) {
      // Set does not exist.
      LOGGER.info("choose could not find query set " + querySetName);
      return null;
    }
    return queries.get(random.nextInt(queries.size()));
  }

  // Randomly chooses a query set that is weighted by its size.
  private static final class QuerySetChooser {
    private static final class QuerySetInfo {
      private final String name;
      private final int size;

      public QuerySetInfo(String name, int size) {
        this.name = name;
        this.size = size;
      }
    }

    private int totalSize;
    private final List<QuerySetInfo> querySetInfos;

    public QuerySetChooser() {
      totalSize = 0;
      querySetInfos = Lists.newLinkedList();
    }

    public void addQuerySet(String name, int size) {
      totalSize += size;
      querySetInfos.add(new QuerySetInfo(name, size));
    }

    public boolean isEmpty() {
      return querySetInfos.isEmpty();
    }

    public String choose(Random rng) {
      int selection = rng.nextInt(totalSize);

      int lowerBound = 0;
      ListIterator<QuerySetInfo> iter = querySetInfos.listIterator();
      while (true) {
        QuerySetInfo setInfo = iter.next();
        int upperBound = lowerBound + setInfo.size;
        if ((lowerBound <= selection) && (selection < upperBound)) {
          return setInfo.name;
        }
      }
    }
  }
}
