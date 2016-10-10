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

import com.google.enterprise.quality.sxse.storage.QueryStorage;
import com.google.enterprise.quality.sxse.storage.StorageManager;
import com.google.enterprise.quality.sxse.storage.SxseStorageException;

public final class PreferencesQueryChooser implements QueryChooser {
  final QueryStorage queryStorage;
  final QueryChooser randomQueryChooser;
  final QueryChooser unjudgedQueryChooser;

  public PreferencesQueryChooser(QueryStorage queryStorage,
      QueryChooser unjudgedQueryChooser) {
    this.queryStorage = queryStorage;
    this.randomQueryChooser = new RandomQueryChooser();
    this.unjudgedQueryChooser = unjudgedQueryChooser;
  }

  public String choose(StorageManager storageManager, User user)
      throws SxseStorageException {
    return getChooser().choose(storageManager, user);
  }

  public String choose(StorageManager storageManager, User user,
      String querySetName) throws SxseStorageException {
    return getChooser().choose(storageManager, user, querySetName);
  }

  private QueryChooser getChooser() throws SxseStorageException {
    return queryStorage.isPreferringUnjudged() ?
        unjudgedQueryChooser : randomQueryChooser;
  }
}
