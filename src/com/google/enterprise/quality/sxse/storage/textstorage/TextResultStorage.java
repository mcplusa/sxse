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

import com.google.common.base.Preconditions;
import com.google.enterprise.quality.sxse.SearchResult;
import com.google.enterprise.quality.sxse.hashers.Hasher;
import com.google.enterprise.quality.sxse.storage.SxseStorageConstants;
import com.google.enterprise.quality.sxse.storage.SxseStorageException;
import com.google.enterprise.quality.sxse.storage.textstorage.TextUtil.KeyValuePair;
import com.google.enterprise.quality.sxse.storage.textstorage.TextUtil.PrematureEofException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stores all search results in text format.
 */
class TextResultStorage {
  private static final Logger LOGGER = Logger.getLogger(
      TextResultStorage.class.getName());

  private final File prefsFile;
  private final File resultsFile;
  private final boolean resultsExisted;
  private final boolean prefsExisted;
  private final RandomAccessFile prefsAccess;
  private RandomAccessFile resultsAccess;

  private boolean randomSwapping;
  private boolean storingResults;
  private boolean submittingAutomatically;
  private int resultRetrievalTimeout;
  private int maxResults;

  private final Hasher resultsHasher;
  private final int resultHashSize;
  private final Map<String, HashDetails> hashOffsetMap;

  /**
   * Keys associated with values in the text file saving results.
   */
  private static final class TextResultKeys {
    static final String RESULTS_HASH = "resultsHash";
    static final String RESULTS_SIZE = "numResults";
    static final String TITLE = "title";
    static final String SNIPPET = "snippet";
    static final String URL = "url";
    static final String SIZE = "size";
    static final String CROWDED = "crowded";
  }

  /**
   * Keys associated with values in the text file saving preferences.
   */
  private static final class TextResultPrefKeys {
    static final String RANDOM_SWAPPING = "randomSwapping";
    static final String STORING_RESULTS = "storingResults";
    static final String SUBMITTING_AUTOMATICALLY = "submittingAutomatically";
    static final String RESULT_RETRIEVAL_TIMEOUT = "resultRetrievalTimeout";
    static final String MAX_RESULTS = "maxResults";
  }

  /**
   * The details of a hash, namely its reference count in {@link UserStorage},
   * and its offset in the file.
   */
  private static final class HashDetails {
    public int referenceCount;
    public long fileOffset;

    public HashDetails(int referenceCount, long fileOffset) {
      this.referenceCount = referenceCount;
      this.fileOffset = fileOffset;
    }
  }

  protected TextResultStorage(File resultDir, Hasher resultsHasher,
      HashMap<String, Integer> hashRefCounts) throws SxseStorageException {
    if (!resultDir.exists()) {
      // Make empty directory for results if it does not exist yet.
      resultDir.mkdir();
    }

    this.prefsFile = new File(resultDir, "resultPrefs");
    this.resultsFile = new File(resultDir, "results");
    prefsExisted = prefsFile.exists();
    resultsExisted = resultsFile.exists();
    try {
      prefsAccess = new RandomAccessFile(prefsFile, "rw");
      openResultsFile();
    } catch (FileNotFoundException e) {
      throw new SxseStorageException(e);
    }

    this.resultsHasher = resultsHasher;
    resultHashSize = 2 * this.resultsHasher.getHashSize();
    hashOffsetMap = new TreeMap<String, HashDetails>();

    // Initialize all members.
    resetState();
    // Read in any saved results and saved preferences.
    if (resultsExisted && (this.resultsFile.length() > 0)) {
      readAllResults(hashRefCounts);
    }
    if (prefsExisted && (this.prefsFile.length() > 0)) {
      readPreferences();
    }
  }

  private void openResultsFile() throws FileNotFoundException {
    resultsAccess = new RandomAccessFile(resultsFile, "rw");
  }

  private void resetState() {
    randomSwapping = SxseStorageConstants.JudgmentStorageDefaults.IS_RANDOM_SWAPPING;
    storingResults = SxseStorageConstants.JudgmentStorageDefaults.IS_STORING_RESULTS;
    submittingAutomatically =
        SxseStorageConstants.JudgmentStorageDefaults.IS_SUBMITTING_AUTOMATICALLY;
    resultRetrievalTimeout =
        SxseStorageConstants.JudgmentStorageDefaults.RESULT_RETRIEVAL_TIMEOUT;
    maxResults = SxseStorageConstants.JudgmentStorageDefaults.MAX_RESULTS;
  }

  /**
   * Adds the side-by-side results to storage, and returns their identifier.
   *
   * @param firstResults the first list of results to add to storage
   * @param secondResults the second list of results to add to storage
   * @return the identifier for the side-by-side results
   * @throws SxseStorageException if an error occurs
   */
  public String addResults(List<SearchResult> firstResults,
      List<SearchResult> secondResults) throws SxseStorageException {
    if ((firstResults == null) || (secondResults == null)) {
      return null;
    }

    String firstHashString = addResult(firstResults);
    String secondHashString = addResult(secondResults);
    return firstHashString + secondHashString;
  }

  private String addResult(List<SearchResult> results)
      throws SxseStorageException{
    String resultsHash = hashSearchResults(results);
    HashDetails hashDetails = hashOffsetMap.get(resultsHash);
    if (hashDetails != null) {
      // Results already exist on disk, so simply increment reference count.
      ++hashDetails.referenceCount;
      return resultsHash;
    }

    try {
      // Seek to end of file, then write result.
      final long fileEnd = resultsAccess.length();
      resultsAccess.seek(fileEnd);
      writeResult(resultsAccess, resultsHash, results);

      // Results written, now update mapping from hash to offset in file.
      hashDetails = new HashDetails(1, fileEnd);
      hashOffsetMap.put(resultsHash, hashDetails);
      return resultsHash;
    } catch (IOException e) {
      LOGGER.severe("addResult caught IOException, partial results written");
      throw new SxseStorageException(e);
    }
  }

  private static void writeResult(RandomAccessFile raf,
      String resultsHashString, List<SearchResult> results) throws IOException {
    StringBuilder sb = new StringBuilder();
    TextUtil.writeValue(TextResultKeys.RESULTS_HASH, resultsHashString, sb);
    TextUtil.writeValue(TextResultKeys.RESULTS_SIZE, results.size(), sb);

    // Write hash of all results, number of results.
    raf.writeBytes(sb.toString());

    // For each result, write title, snippet, url, size, and whether crowded.
    for (SearchResult result : results) {
      StringBuilder resultSb = new StringBuilder();

      TextUtil.writeValue(TextResultKeys.TITLE,
          result.getTitle(), resultSb);
      TextUtil.writeValue(TextResultKeys.SNIPPET,
          result.getSnippet(), resultSb);
      TextUtil.writeValue(TextResultKeys.URL,
          result.getUrl(), resultSb);
      TextUtil.writeValue(TextResultKeys.SIZE,
          result.getSize(), resultSb);
      TextUtil.writeValue(TextResultKeys.CROWDED,
          result.isCrowded(), resultSb);

      raf.writeBytes(resultSb.toString());
    }
  }

  public boolean hasResult(String resultsId) {
    Preconditions.checkArgument(
        resultsId.length() == (2 * resultHashSize), "identifier is wrong size");

    String firstHash = resultsId.substring(0, resultHashSize);
    String secondHash = resultsId.substring(resultHashSize, 2 * resultHashSize);
    return (hashOffsetMap.containsKey(firstHash) &&
        hashOffsetMap.containsKey(secondHash));
  }

  public boolean getResults(String resultsId,
      List<SearchResult> firstResults, List<SearchResult> secondResults)
      throws SxseStorageException {
    Preconditions.checkArgument(
        resultsId.length() == (2 * resultHashSize), "identifier is wrong size");

    String firstHash = resultsId.substring(0, resultHashSize);
    String secondHash = resultsId.substring(resultHashSize, 2 * resultHashSize);

    if (!hashOffsetMap.containsKey(firstHash) ||
        !hashOffsetMap.containsKey(secondHash)) {
      return false;
    }
    getResult(firstHash, firstResults);
    getResult(secondHash, secondResults);
    return true;
  }

  private void getResult(String resultHash, List<SearchResult> results)
      throws SxseStorageException {
    try {
      // Seek to results in file.
      long offset = hashOffsetMap.get(resultHash).fileOffset;
      resultsAccess.seek(offset);

      // Ensure that offset is not EOF.
      String resultsHashString = TextUtil.readValue(
          TextResultKeys.RESULTS_HASH, resultsAccess);
      String numResultsString = TextUtil.readValue(
          TextResultKeys.RESULTS_SIZE, resultsAccess);
      if ((resultsHashString == null) || (numResultsString == null)) {
        LOGGER.severe("getResult read EOF prematurely");
        throw new PrematureEofException("getResult");
      }

      int numResults = Integer.valueOf(numResultsString);
      readResultsList(results, numResults);
    } catch (IOException e) {
      LOGGER.severe("getResult caught IOException, no results read");
      throw new SxseStorageException(e);
    }
  }

  private String hashSearchResults(List<SearchResult> resultList) {
    for (Iterator<SearchResult> i = resultList.iterator(); i.hasNext(); ) {
      SearchResult nextResult = i.next();
      nextResult.updateHasher(resultsHasher);
    }
    byte[] resultsHash = resultsHasher.finish();
    return TextUtil.bytesToHexString(resultsHash);
  }

  public boolean isRandomSwapping() {
    return randomSwapping;
  }

  public void setRandomSwapping(boolean random) throws SxseStorageException {
    randomSwapping = random;
    writePreferences();
  }

  public boolean isStoringResults() {
    return storingResults;
  }

  public void setStoringResults(boolean store) throws SxseStorageException {
    storingResults = store;
    writePreferences();
  }

  public boolean isSubmittingAutomatically() {
    return submittingAutomatically;
  }

  public void setSubmittingAutomatically(boolean submitAutomatically)
      throws SxseStorageException {
    submittingAutomatically = submitAutomatically;
    writePreferences();
  }

  public int getResultRetrievalTimeout() throws SxseStorageException {
    return resultRetrievalTimeout;
  }

  public void setResultRetrievalTimeout(int timeout)
      throws SxseStorageException {
    Preconditions.checkArgument(timeout > 0, "timeout value must be positive");

    this.resultRetrievalTimeout = timeout;
    writePreferences();
  }

  public int getMaxResults() {
    return maxResults;
  }

  public void setMaxResults(int maxResults) throws SxseStorageException {
    Preconditions.checkArgument(maxResults > 0,
        "maxResults value must be positive");

    this.maxResults = maxResults;
    writePreferences();
  }

  public void updateHashRefCounts(Map<String, Integer> removedHashRefCounts)
      throws SxseStorageException {
    int numOrphaned = 0;
    for (Map.Entry<String, Integer> removedHashRefCount :
        removedHashRefCounts.entrySet()) {
      HashDetails hashDetails = hashOffsetMap.get(removedHashRefCount.getKey());
      if (hashDetails == null) {
        LOGGER.severe("ResultStorage missing hash from UserStorage, "
            + "possible data corruption");
        continue;
      }

      hashDetails.referenceCount -= removedHashRefCount.getValue();
      if (hashDetails.referenceCount <= 0) {
        ++numOrphaned;
      }
    }

    discardOrphanedHashes(numOrphaned);
  }

  private void discardOrphanedHashes(int numOrphaned)
      throws SxseStorageException {
    if (numOrphaned == hashOffsetMap.size()) {
      // We're discarding all hashes, so clear the map and delete the file.
      discardAllHashes();
      return;
    }

    File newResultsFile = new File(resultsFile.getParent(), "resultsNew");
    RandomAccessFile newResultsAccess = null;
    try {
      newResultsAccess = new RandomAccessFile(newResultsFile, "rw");
    } catch (FileNotFoundException e) {
      throw new SxseStorageException(e);
    }

    try {
      Iterator<Map.Entry<String, HashDetails>> offsetIterator =
          hashOffsetMap.entrySet().iterator();
      List<SearchResult> results = new LinkedList<SearchResult>();

      while (offsetIterator.hasNext()) {
        Map.Entry<String, HashDetails> entry = offsetIterator.next();
        String resultHash = entry.getKey();
        HashDetails details = entry.getValue();

        if (details.referenceCount > 0) {
          // Copy the results to the new file.
          final long newEntryPos = newResultsAccess.length();
          getResult(resultHash, results);
          writeResult(newResultsAccess, resultHash, results);
          results.clear();
          // Update its file offset mapping.
          details.fileOffset = newEntryPos;
        } else {
          // Results not copied to new file, so remove its offset mapping.
          offsetIterator.remove();
        }
      }

      // Replace old results file with new one.
      resultsAccess.close();
      newResultsAccess.close();
      resultsFile.delete();
      newResultsFile.renameTo(resultsFile);
      openResultsFile();
    } catch (IOException e) {
      throw new SxseStorageException(e);
    }
  }

  private void discardAllHashes() throws SxseStorageException {
    try {
      resultsAccess.close();
      resultsFile.delete();
      openResultsFile();

      hashOffsetMap.clear();
    } catch (IOException e) {
      throw new SxseStorageException(e);
    }
  }

  private void readResultsList(List<SearchResult> results, int numResults)
      throws SxseStorageException {
    // Read title, snippet, url, size, and whether crowded for each result.
    for (int i = 0; i < numResults; ++i) {
      String title = TextUtil.readValue(TextResultKeys.TITLE, resultsAccess);
      String snippet = TextUtil.readValue(
          TextResultKeys.SNIPPET, resultsAccess);
      String urlString = TextUtil.readValue(TextResultKeys.URL, resultsAccess);
      String size = TextUtil.readValue(TextResultKeys.SIZE, resultsAccess);
      String isCrowdedString = TextUtil.readValue(
          TextResultKeys.CROWDED, resultsAccess);

      // Do not construct new result if have reached end of file.
      if ((title == null) || (snippet == null) || (urlString == null)
          || (size == null)) {
        throw new PrematureEofException("readResultsList");
      }
      URI url = null;
      try {
        url = new URI(urlString);
      } catch (URISyntaxException e) {
        LOGGER.log(Level.WARNING, "getResult read unexpected URL syntax, "
            + "skipping result", e);
        continue;
      }
      boolean isCrowded = Boolean.valueOf(isCrowdedString);

      // Append read result to list.
      SearchResult result = new SearchResult(url, title, snippet, size,
          isCrowded);
      results.add(result);
    }
  }

  private void readAllResults(HashMap<String, Integer> hashRefCounts)
      throws SxseStorageException {
    long resultsOffset = 0L;
    List<SearchResult> results = new LinkedList<SearchResult>();
    int numOrphaned = 0;

    while (true) {
      String nextLine;
      try {
        nextLine = resultsAccess.readLine();
      } catch (IOException e) {
        throw new SxseStorageException(e);
      }
      if (nextLine == null) {
        break;
      }

      String resultsHash =
          TextUtil.readValue(TextResultKeys.RESULTS_HASH, nextLine);
      String numResultsString =
          TextUtil.readValue(TextResultKeys.RESULTS_SIZE, resultsAccess);
      if ((resultsHash == null) || (numResultsString == null)) {
        // Have reached EOF, so no more results.
        break;
      }

      // Read in results, but discard.
      int numResults = Integer.valueOf(numResultsString);
      readResultsList(results, numResults);
      results.clear();
      // Assign a reference count of 0 if missing in UserStorage.
      Integer referenceCount = hashRefCounts.get(resultsHash);
      if (referenceCount == null) {
        referenceCount = 0;
        ++numOrphaned;
      }
      // Add mapping from hash to offset and reference count in file.
      hashOffsetMap.put(resultsHash,
          new HashDetails(referenceCount, resultsOffset));
      try {
        // Update offset for next results.
        resultsOffset = resultsAccess.getFilePointer();
      } catch (IOException e) {
        throw new SxseStorageException(e);
      }
    }

    discardOrphanedHashes(numOrphaned);
  }

  private void readPreferences() throws SxseStorageException {
    try {
      // Move to beginning of file.
      prefsAccess.seek(0L);
    } catch (IOException e) {
      throw new SxseStorageException(e);
    }

    while (true) {
      String line;
      try {
        line = prefsAccess.readLine();
      } catch (IOException e) {
        throw new SxseStorageException(e);
      }
      if (line == null) {
        // Reached end of file
        break;
      }

      KeyValuePair kvp = TextUtil.makeKeyValuePair(line);
      if (kvp.key.equals(TextResultPrefKeys.RANDOM_SWAPPING)) {
        randomSwapping = Boolean.valueOf(kvp.value);
      } else if (kvp.key.equals(TextResultPrefKeys.STORING_RESULTS)) {
        storingResults = Boolean.valueOf(kvp.value);
      } else if (kvp.key.equals(TextResultPrefKeys.SUBMITTING_AUTOMATICALLY)) {
        submittingAutomatically = Boolean.valueOf(kvp.value);
      } else if (kvp.key.equals(TextResultPrefKeys.RESULT_RETRIEVAL_TIMEOUT)) {
        resultRetrievalTimeout = Integer.valueOf(kvp.value);
      } else if (kvp.key.equals(TextResultPrefKeys.MAX_RESULTS)) {
        maxResults = Integer.valueOf(kvp.value);
      }
    }
  }

  private void writePreferences() throws SxseStorageException {
    try {
      // Clear contents of file, flush this change.
      prefsAccess.getChannel().truncate(0L);
      prefsAccess.getChannel().force(false);
      // Must seek or new preferences prefaced with null bytes.
      prefsAccess.seek(0L);

      StringBuilder sb = new StringBuilder();

      // Append whether results may be randomly swapped.
      TextUtil.writeValue(TextResultPrefKeys.RANDOM_SWAPPING,
          String.valueOf(randomSwapping), sb);
      // Append whether search results are stored with the judgment.
      TextUtil.writeValue(TextResultPrefKeys.STORING_RESULTS,
          String.valueOf(storingResults), sb);
      // Append whether results are written automatically for equal results.
      TextUtil.writeValue(TextResultPrefKeys.SUBMITTING_AUTOMATICALLY,
          String.valueOf(submittingAutomatically), sb);
      TextUtil.writeValue(TextResultPrefKeys.RESULT_RETRIEVAL_TIMEOUT,
          String.valueOf(resultRetrievalTimeout), sb);
      // Append the maximum number of results each policy should return.
      TextUtil.writeValue(TextResultPrefKeys.MAX_RESULTS,
          String.valueOf(maxResults), sb);

      // Flush buffer to file.
      prefsAccess.writeBytes(sb.toString());
    } catch (IOException e) {
      throw new SxseStorageException(e);
    }
  }

  protected void tryDelete() {
    // Only delete the files we created.
    if (!resultsExisted) {
      resultsFile.delete();
    }
    if (!prefsExisted) {
      prefsFile.delete();
    }
  }
}
