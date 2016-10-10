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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSortedSet;
import com.google.enterprise.quality.sxse.JudgmentDetails;
import com.google.enterprise.quality.sxse.QueryFormatter;
import com.google.enterprise.quality.sxse.JudgmentDetails.Judgment;
import com.google.enterprise.quality.sxse.storage.SxseStorageException;
import com.google.enterprise.quality.sxse.storage.textstorage.TextUtil.PrematureEofException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the {@code UserStorage} interface where all data is
 * saved in text format.
 */
class TextUserStorage {
  private static final Logger LOGGER = Logger.getLogger(
      TextUserStorage.class.getName());

  private final boolean existed;
  private final File usersSubdir;

  private final Set<String> userNames;
  private final int resultHashSize;

  /**
   * Keys associated with values in the saved text file.
   */
  private static final class TextUserKeys {
    static final String QUERY = "query";
    static final String JUDGMENT = "judgment";
    static final String TIMESTAMP = "time";
    static final String RESULTS_HASH = "resultsHash";
  }

  public TextUserStorage(File userSubdir, int resultHashSize,
      Map<String, Integer> hashRefCounts) throws SxseStorageException {
    this.usersSubdir = userSubdir;
    existed = usersSubdir.exists();
    if (!existed) {
      // Make empty directory for users if it does not exist yet.
      usersSubdir.mkdir();
    }

    // Create unmodifiable list of users backed by the real list.
    userNames = new TreeSet<String>();
    this.resultHashSize = 2 * resultHashSize;
    buildUsers(hashRefCounts);
  }

  private void buildUsers(Map<String, Integer> hashRefCounts)
      throws SxseStorageException {
    // Build the list of users.
    File[] userFiles = this.usersSubdir.listFiles(
        TextUtil.getFilesOnlyFilter());
    for (File userFile : userFiles) {
      String userName = userFile.getName();
      userNames.add(userName);
      buildHashRefCounts(userName, hashRefCounts);
    }
    LOGGER.info(userFiles.length + " existing users found");
  }

  private void buildHashRefCounts(String userName,
      Map<String, Integer> hashRefCounts) throws SxseStorageException {
    try {
      BufferedReader userReader = createReaderForUser(userName);
      while (true) {
        JudgmentDetails judgment = readNextJudgment(userReader);
        if (judgment == null) {
          break;
        }

        String resultsId = judgment.getResultsId();
        if (resultsId != null) {
          String firstHash = resultsId.substring(0, resultHashSize);
          increaseRefCount(firstHash, hashRefCounts);
          String secondHash = resultsId.substring(resultHashSize,
              2 * resultHashSize);
          increaseRefCount(secondHash, hashRefCounts);
        }
      }
      userReader.close();
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE,
          "Could not read data for user " + userName, e);
    }
  }

  private void increaseRefCount(String hash, Map<String, Integer> hashRefCounts) {
    Integer refCount = hashRefCounts.get(hash);
    if (refCount == null) {
      hashRefCounts.put(hash, 1);
    } else {
      hashRefCounts.put(hash, refCount + 1);
    }
  }

  public JudgmentDetails addJudgment(String userName, JudgmentDetails judgment,
      String resultsId) throws SxseStorageException {
    if (!userNames.contains(userName)) {
      addMissingUser(userName);
    }

    // Append to list of judgments on disk.
    File addedSet = new File(usersSubdir, userName);
    try {
      BufferedWriter out = new BufferedWriter(new FileWriter(addedSet, true));
      TextUtil.writeValue(TextUserKeys.QUERY,
          judgment.getQuery(), out);
      TextUtil.writeValue(TextUserKeys.JUDGMENT,
          judgment.getJudgment(), out);
      TextUtil.writeValue(TextUserKeys.TIMESTAMP,
          judgment.getTimestamp(), out);

      String firstHash = null;
      String secondHash = null;
      if (resultsId != null) {
        firstHash = resultsId.substring(0, resultHashSize);
        secondHash = resultsId.substring(resultHashSize, 2 * resultHashSize);
      }
      writeResultList(out, new ResultListDetails(
          judgment.getFirstQueryFormatter(), firstHash));
      writeResultList(out, new ResultListDetails(
          judgment.getSecondQueryFormatter(), secondHash));
      out.close();
    } catch (IOException e) {
      LOGGER.severe("addJudgment caught IOException, partial judgment written");
      throw new SxseStorageException(e);
    }

    return new JudgmentDetails(
        judgment.getQuery(), judgment.getJudgment(), judgment.getTimestamp(),
        judgment.getFirstQueryFormatter(), judgment.getSecondQueryFormatter(),
        resultsId);
  }

  private void addMissingUser(String userName) throws SxseStorageException {
    File addedUser = new File(usersSubdir, userName);
    try {
      addedUser.createNewFile();
      userNames.add(userName);
    } catch (IOException e) {
      LOGGER.severe("addQuerySet caught IOException, no user created");
      throw new SxseStorageException(e);
    }
  }

  public List<JudgmentDetails> getJudgments(String userName)
      throws SxseStorageException {
    Predicate<JudgmentDetails> alwaysTrue = Predicates.alwaysTrue();
    return getJudgments(userName, alwaysTrue);
  }

  public List<JudgmentDetails> getJudgments(String userName,
      final String query) throws SxseStorageException {
    Predicate<JudgmentDetails> queryPredicate =
        new Predicate<JudgmentDetails>() {
      public boolean apply(JudgmentDetails judgment) {
        // Only accumulate judgments for the given query.
        return query.equals(judgment.getQuery());
      }
    };
    return getJudgments(userName, queryPredicate);
  }

  private List<JudgmentDetails> getJudgments(String userName,
      Predicate<JudgmentDetails> keepPredicate) throws SxseStorageException {
    if (!userNames.contains(userName)) {
      return Collections.emptyList();
    }

    BufferedReader userReader = createReaderForUser(userName);
    List<JudgmentDetails> judgments = new ArrayList<JudgmentDetails>();
    try {
      while (true) {
        JudgmentDetails judgment = readNextJudgment(userReader);
        if (judgment == null) {
          // Reached end of file.
          break;
        } else if (!keepPredicate.apply(judgment)) {
          // Judgment does not meet criteria of the returned list.
          continue;
        }
        // Append judgment to list of judgments by user.
        judgments.add(judgment);
      }
      userReader.close();
    } catch (IOException e) {
      LOGGER.severe("getJudgments caught IOException, no results read");
      throw new SxseStorageException(e);
    }

    return Collections.unmodifiableList(judgments);
  }
  
  private BufferedReader createReaderForUser(String userName)
      throws SxseStorageException {
    File userFile = new File(usersSubdir, userName);
    try {
      return new BufferedReader(new FileReader(userFile));
    } catch (FileNotFoundException e) {
      // Judgments for user are not on disk, attempt to sync in-memory list.
      LOGGER.log(Level.WARNING, "judgments for user not found", e);
      userNames.remove(userName);
      throw new SxseStorageException(e);
    }
  }

  private JudgmentDetails readNextJudgment(BufferedReader in)
      throws IOException, SxseStorageException {
    String nextLine = in.readLine();
    if (nextLine == null) {
      // Reached end of file
      return null;
    }

    // Read query, judgment, and timestamp.
    String query = TextUtil.readValue(TextUserKeys.QUERY, nextLine);
    String judgmentString = TextUtil.readValue(
        TextUserKeys.JUDGMENT, in);
    String timestampString = TextUtil.readValue(
        TextUserKeys.TIMESTAMP, in);
    if (query == null) {
      // Okay if null, reached end of file.
      return null;
    } else if ((judgmentString == null) || (timestampString == null)) {
      throw new PrematureEofException("getJudgments");
    }

    // Read results from both scoring policies.
    ResultListDetails firstResultList = readResultList(in);
    ResultListDetails secondResultList = readResultList(in);

    // Convert judgment and timestamp from strings.
    Judgment judgment = Judgment.valueOf(judgmentString);
    if (judgment == null) {
      throw new SxseStorageException(
          "readNextJudgment read invalid judgment value");
    }
    long timestamp = Long.valueOf(timestampString);
    if (timestamp < 0) {
      throw new SxseStorageException(
          "readNextJudgment read invalid timestamp value");
    }

    String resultsId = null;
    if ((firstResultList.getResultHash().length() > 0) &&
        (secondResultList.getResultHash().length() > 0)) {
      
      resultsId =
          firstResultList.getResultHash() + secondResultList.getResultHash();
    }
    // Append judgment to list of judgments by user.
    return new JudgmentDetails(query, judgment, timestamp,
        firstResultList.getQueryFormatter(),
        secondResultList.getQueryFormatter(), resultsId);
  }

  public boolean removeUsers(Set<String> deletedUserNames,
      Map<String, Integer> removedHashRefCounts) throws SxseStorageException {
    boolean removedAny = false;
    for (String userName : deletedUserNames) {
      if (!userNames.contains(userName)) {
        continue;
      }

      buildHashRefCounts(userName, removedHashRefCounts);
      File userFile = new File(usersSubdir, userName);
      userFile.delete();
      userNames.remove(userName);

      removedAny = true;
    }
    return removedAny;
  }

  public Set<String> getUsers() {
    return ImmutableSortedSet.copyOf(userNames);
  }

  private void writeResultList(BufferedWriter out, ResultListDetails results)
      throws SxseStorageException {
    QueryFormatter queryFormatter = results.getQueryFormatter();
    TextUtil.writeQueryFormatter(queryFormatter, out);

    // Write hash of search results if present.
    String resultHash = results.getResultHash();
    if (resultHash != null) {
      TextUtil.writeValue(TextUserKeys.RESULTS_HASH, resultHash, out);
    } else {
      TextUtil.writeEmptyValue(TextUserKeys.RESULTS_HASH, out);
    }
  }

  private ResultListDetails readResultList(BufferedReader in)
      throws SxseStorageException {
    QueryFormatter queryFormatter = TextUtil.readQueryFormatter(in);
    String resultHash = TextUtil.readValue(
        TextUserKeys.RESULTS_HASH, in);
    return new ResultListDetails(queryFormatter, resultHash);
  }

  protected void tryDelete() {
    // Only delete the directory if we created it.
    if (!existed) {
      usersSubdir.delete();
    }
  }
}
