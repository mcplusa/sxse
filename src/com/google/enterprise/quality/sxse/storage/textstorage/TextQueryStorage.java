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

import com.google.common.base.Joiner;
import com.google.enterprise.quality.sxse.storage.QueryStorage;
import com.google.enterprise.quality.sxse.storage.SxseStorageConstants;
import com.google.enterprise.quality.sxse.storage.SxseStorageException;
import com.google.enterprise.quality.sxse.storage.textstorage.TextUtil.KeyValuePair;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * Implementation of the {@link QueryStorage} interface where all data is
 * saved in text format.
 */
class TextQueryStorage implements QueryStorage {
  private static final Logger LOGGER = Logger.getLogger(
      TextQueryStorage.class.getName());

  private static final String QUERY_SET_FILE_PREFIX = "qs_";
  private static final FileFilter QUERY_SET_FILE_FILTER = new FileFilter() {
    public boolean accept(File pathname) {
      return (TextUtil.getFilesOnlyFilter().accept(pathname) &&
          pathname.getName().startsWith(QUERY_SET_FILE_PREFIX));
    }
  };

  private final File querySubdir;
  private final boolean subdirExisted;
  private final RandomAccessFile prefsAccess;

  private final Set<String> activeSetNames;
  private boolean preferringUnjudged;
  private boolean showingQuerySets;

  private final Map<String, Integer> setNamesToSizeMap;

  /**
   * Keys associated with values in the text file saving preferences.
   */
  private static final class TextQueryPrefKeys {
    static final String ACTIVE_SETS = "activeSets";
    static final String PREFERRING_UNJUDGED = "preferringUnjudged";
    static final String SHOWING_QUERY_SETS = "showingQuerySets";
  }

  protected TextQueryStorage(File querySubdir) throws SxseStorageException {
    this.querySubdir = querySubdir;
    subdirExisted = querySubdir.exists();
    if (!subdirExisted) {
      // Make empty directory for queries if it does not exist yet.
      this.querySubdir.mkdir();
    }

    File prefsFile = new File(querySubdir, "queryPrefs");
    try {
      prefsAccess = new RandomAccessFile(prefsFile, "rw");
    } catch (FileNotFoundException e) {
      LOGGER.severe("Caught IOException when accessing results file");
      throw new SxseStorageException(e);
    }

    // Create unmodifiable list of set names backed by the real list.
    activeSetNames = new TreeSet<String>();
    setNamesToSizeMap = new TreeMap<String, Integer>();
    // Build the list of query sets.
    File[] querySetFiles = this.querySubdir.listFiles(QUERY_SET_FILE_FILTER);
    for (File f : querySetFiles) {
      // Do not read number of queries in file until needed.
      setNamesToSizeMap.put(makeQuerySetName(f), null);
    }
    LOGGER.info(querySetFiles.length + " existing query sets found");

    // Initialize all members.
    resetState();
    // Read in the saved preferences.
    if (subdirExisted && prefsFile.length() > 0) {
      LOGGER.info("Existing preferences file found");
      readPreferences();
    }
  }

  /*
   * Resets the in-memory state of the preferences. Do this to safely recover
   * from an IOException when writing to disk.
   */
  private void resetState() {
    activeSetNames.clear();
    preferringUnjudged = SxseStorageConstants.QueryStorageDefaults.IS_PREFERRING_UNJUDGED;
    showingQuerySets = SxseStorageConstants.QueryStorageDefaults.IS_SHOWING_QUERY_SETS;
  }

  public boolean addQuerySet(String setName, SortedSet<String> queries)
      throws SxseStorageException {
    // Do not succeed if query set already exists.
    if (setNamesToSizeMap.containsKey(setName)) {
      return false;
    }

    // Try writing to disk first.
    File addedSet = new File(querySubdir, makeFilename(setName));
    try {
      BufferedWriter out = new BufferedWriter(new FileWriter(addedSet));
      for (String rawQuery : queries) {
        // Remove leading and trailing whitespace from query.
        String query = rawQuery.trim();
        if (!query.equals("")) {
          // Do not write the empty string.
          out.write(query + "\n");
        }
      }
      out.close();
    } catch (IOException e) {
      // If could not write in entirety, delete the file.
      LOGGER.severe("addQuerySet caught IOException, no set created");
      addedSet.delete();
      throw new SxseStorageException(e);
    }

    // Query set written successfully, now update in-memory data.
    setNamesToSizeMap.put(setName, queries.size());
    addActiveSet(setName);
    return true;
  }

  public boolean renameQuerySet(String prevName, String newName)
      throws SxseStorageException {
    // If not renaming, succeed immediately.
    if (prevName.equals(newName)) {
      return true;
    }
    // Check that both set names are valid.
    if (!setNamesToSizeMap.containsKey(prevName) ||
        setNamesToSizeMap.containsKey(newName)) {
      return false;
    }

    File renamedSet = new File(querySubdir, makeFilename(prevName));
    if (renamedSet.renameTo(new File(querySubdir, makeFilename(newName)))) {
      // Renamed on disk, now update in-memory data.
      Integer setSize = setNamesToSizeMap.remove(prevName);
      setNamesToSizeMap.put(newName, setSize);
      if (activeSetNames.remove(prevName)) {
        addActiveSet(newName);
      }
      return true;
    } else {
      LOGGER.severe("renameQuerySet could not rename file on disk");
      throw new SxseStorageException("renameQuerySet could not rename file");
    }
  }

  public int getQuerySetSize(String setName) throws SxseStorageException {
    if (!setNamesToSizeMap.containsKey(setName)) {
      return -1;
    }

    Integer setSize = setNamesToSizeMap.get(setName);
    if (setSize == null) {
      List<String> querySet = getQuerySet(setName);
      setSize = querySet.size();
      setNamesToSizeMap.put(setName, setSize);
    }
    return setSize;
  }

  public List<String> getQuerySet(String setName) throws SxseStorageException {
    // Do not succeed if query set does not exist.
    if (!setNamesToSizeMap.containsKey(setName)) {
      return null;
    }

    List<String> queries = new ArrayList<String>();
    File retrievedSet = new File(querySubdir, makeFilename(setName));
    try {
      BufferedReader in = new BufferedReader(new FileReader(retrievedSet));
      while (true) {
        String query = in.readLine();
        if (query == null) {
          break;
        }
        queries.add(query);
      }
      in.close();
    } catch (IOException e) {
      LOGGER.severe("getQuerySet caught IOException, no set read");
      throw new SxseStorageException(e);
    }
    // Update size in map in case it is still null.
    setNamesToSizeMap.put(setName, queries.size());
    return Collections.unmodifiableList(queries);
  }

  public Set<String> getQuerySetNames() {
    return Collections.unmodifiableSet(
        new TreeSet<String>(setNamesToSizeMap.keySet()));
  }

  public boolean removeQuerySet(String setName) throws SxseStorageException {
    File removedSet = new File(querySubdir, makeFilename(setName));
    // Do not succeed if query set does not exist.
    if (setNamesToSizeMap.containsKey(setName) && removedSet.delete()) {
      setNamesToSizeMap.remove(setName);
      removeActiveSet(setName);
      return true;
    }
    return false;
  }

  public boolean isActive(String setName) {
    return activeSetNames.contains(setName);
  }

  public boolean setActive(String setName, boolean isActive)
      throws SxseStorageException {
    if (!setNamesToSizeMap.containsKey(setName)) {
      return false;
    }

    if (isActive) {
      addActiveSet(setName);
    } else {
      removeActiveSet(setName);
    }
    return true;
  }

  public boolean isPreferringUnjudged() {
    return preferringUnjudged;
  }

  public void setPreferringUnjudged(boolean preferUnjudged)
      throws SxseStorageException {
    preferringUnjudged = preferUnjudged;

    writePreferences();
  }

  public boolean isShowingQuerySets() {
    return showingQuerySets;
  }

  public void setShowingQuerySets(boolean showQuerySets)
      throws SxseStorageException {
    showingQuerySets = showQuerySets;

    writePreferences();
  }

  /*
   * Converts the name of a query set to its filename on disk.
   */
  private String makeFilename(String querySetName) {
    return QUERY_SET_FILE_PREFIX + querySetName;
  }

  /*
   * Converts the file of a query set to the query set name.
   */
  private String makeQuerySetName(File f) {
    return f.getName().substring(QUERY_SET_FILE_PREFIX.length());
  }

  private void addActiveSet(String setName) throws SxseStorageException {
    if (activeSetNames.add(setName)) {
      // If query set was not active already, write new preferences.
      writePreferences();
    }
  }

  private void removeActiveSet(String setName) throws SxseStorageException {
    if (activeSetNames.remove(setName)) {
      // If query set was not inactive already, write new preferences.
      writePreferences();
    }
  }

  private void readPreferences() throws SxseStorageException {
    // Reset the in-memory state of the preferences.
    activeSetNames.clear();

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
      if (kvp.key.equals(TextQueryPrefKeys.ACTIVE_SETS)) {
        StringTokenizer tokenizer = new StringTokenizer(kvp.value, ",");
        while (tokenizer.hasMoreTokens()) {
          activeSetNames.add(tokenizer.nextToken());
        }
      } else if (kvp.key.equals(TextQueryPrefKeys.PREFERRING_UNJUDGED)) {
        preferringUnjudged = Boolean.valueOf(kvp.value);
      } else if (kvp.key.equals(TextQueryPrefKeys.SHOWING_QUERY_SETS)) {
        showingQuerySets = Boolean.valueOf(kvp.value);
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

      // Append what query sets are active.
      TextUtil.writeValue(TextQueryPrefKeys.ACTIVE_SETS,
          Joiner.on(",").join(activeSetNames), sb);
      // Append whether unjudged queries are preferred for judgment.
      TextUtil.writeValue(TextQueryPrefKeys.PREFERRING_UNJUDGED,
          String.valueOf(preferringUnjudged), sb);
      // Append whether query sets are shown at judgment.
      TextUtil.writeValue(TextQueryPrefKeys.SHOWING_QUERY_SETS,
          String.valueOf(showingQuerySets), sb);

      // Flush buffer to file.
      prefsAccess.writeBytes(sb.toString());
    } catch (IOException e) {
      throw new SxseStorageException(e);
    }
  }

  protected void tryDelete() {
    // Only delete the directory if we created it.
    if (!subdirExisted) {
      querySubdir.delete();
    }
  }
}
