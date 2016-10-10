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

import java.io.File;

/**
 * Interface through which all storage elements can be accessed.
 */
public interface StorageManager {
  /**
   * @return the root directory for all storage
   */
  public File getRootDirectory();

  /**
   * @return the storage for administartive preferences
   */
  public PreferencesStorage getPreferencesStorage();

  /**
   * @return the storage for judgments
   */
  public JudgmentStorage getJudgmentStorage();

  /**
   * @return the storage for queries
   */
  public QueryStorage getQueryStorage();

  /**
   * Attempts to delete all stored data by deleting all directories that were
   * created when the storage implementation was insantiated. To resist
   * accidental deletion of data, if the storage implementation did not create a
   * directory because it already existed, it is not deleted. This is typically
   * used for the purposes of unit testing, and not in production code.
   */
  public void tryDeleteAll();
}
