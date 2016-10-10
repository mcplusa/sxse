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

import com.google.enterprise.quality.sxse.storage.StorageManager;
import com.google.enterprise.quality.sxse.storage.SxseStorageException;

/**
 * Interface for selecting a query for an assessor to judge.
 */
public interface QueryChooser {
  /**
   * Chooses the next query for {@code user} to judge.
   * 
   * @param storageManager the storage manager through which queries can be
   *        retrieved
   * @param user the user to judge the next query
   * @return the next query to judge
   * @throws SxseStorageException if an error occurs
   */
  public String choose(StorageManager storageManager, User user)
      throws SxseStorageException;
  
  /**
   * Chooses the next query for {@code user} to judge from the query set having
   * the given name.
   * 
   * @param storageManager the storage manager through which queries can be
   *        retrieved
   * @param user the user to judge the next query
   * @param querySet the name of the query set to choose the query from
   * @return the next query to judge from the query set
   * @throws SxseStorageException if an error occurs
   */
  public String choose(StorageManager storageManager, User user,
      String querySet) throws SxseStorageException;
}
