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

import com.google.enterprise.quality.sxse.hashers.Hasher;
import com.google.enterprise.quality.sxse.storage.JudgmentStorage;
import com.google.enterprise.quality.sxse.storage.PreferencesStorage;
import com.google.enterprise.quality.sxse.storage.QueryStorage;
import com.google.enterprise.quality.sxse.storage.StorageManager;
import com.google.enterprise.quality.sxse.storage.SxseStorageException;

import java.io.File;

/**
 * Implementation of the {@link StorageManager} interface where all data is
 * saved in text format.
 */
public final class TextStorage implements StorageManager {
  private static final String PREFERENCES_FILE = "prefs";
  private static final String QUERIES_SUBDIR = "queries";
  private static final String USERS_SUBDIR = "users";

  private final File rootDirectory;
  private final boolean existed;
  private final TextPreferencesStorage prefsStorage;
  private final TextQueryStorage queriesStorage;
  private final TextJudgmentStorage judgmentStorage;

  /**
   * Creates a new storage element that will save all data in the given
   * directory in text format. If the directory does not exist, it is created.
   * 
   * @param rootDirectory the directory to save all data in
   * @throws SxseStorageException if the storage element could not be
   *         initialized
   */
  public TextStorage(File rootDirectory) throws SxseStorageException {
    this(rootDirectory,
        TextUtil.getResultsHasherFactory().getHasher(),
        TextUtil.getPasswordHasherFactory().getHasher());
  }

  /**
   * Creates a new storage element that will save all data in the given
   * directory in text format. If the directory does not exist, it is created.
   * 
   * @param rootDirectory the directory to save all data in
   * @param resultsHasher the hasher to run over search results
   * @param passwordHasher the hashser to run over passwords
   * @throws SxseStorageException if the storage element could not be
   *         initialized
   */
  public TextStorage(File rootDirectory,
      Hasher resultsHasher, Hasher passwordHasher)
      throws SxseStorageException {
    // create root directory
    this.rootDirectory = rootDirectory;
    existed = rootDirectory.exists();
    rootDirectory.mkdirs();

    // create individidual storage elements
    prefsStorage = new TextPreferencesStorage(
        new File(rootDirectory, PREFERENCES_FILE), passwordHasher);
    queriesStorage = new TextQueryStorage(
        new File(rootDirectory, QUERIES_SUBDIR));
    judgmentStorage = new TextJudgmentStorage(rootDirectory, resultsHasher,
        new File(rootDirectory, USERS_SUBDIR));
  }

  public File getRootDirectory() {
    return rootDirectory;
  }

  public PreferencesStorage getPreferencesStorage() {
    return prefsStorage;
  }

  public QueryStorage getQueryStorage() {
    return queriesStorage;
  }

  public JudgmentStorage getJudgmentStorage() {
    return judgmentStorage;
  }

  public void tryDeleteAll() {
    // Try to delete root directory contents and subdirectories first.
    prefsStorage.tryDelete();
    queriesStorage.tryDelete();
    judgmentStorage.tryDelete();

    if (!existed) {
      // Try to delete root dierctory.
      rootDirectory.delete();
    }
  }
}
