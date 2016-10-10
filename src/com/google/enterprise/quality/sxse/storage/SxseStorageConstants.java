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

/**
 * Constant values in SxSE.
 */
public class SxseStorageConstants {
  // Do not allow instantiation of utility class.
  private SxseStorageConstants() {
  }

  /**
   * Default values for {@link JudgmentStorage}. Only primitive values are
   * listed here; for any collection that can be returned, we assume that by
   * default the empty collection is returned; for any other data type, we
   * assume that by default {@code null} or the default instance is returned if
   * there is one.
   */
  public static final class JudgmentStorageDefaults {
    private JudgmentStorageDefaults() {
      // Only static, constant values, so prohibit instantiation.
    }

    /**
     * Default value returned by {@link JudgmentStorage#isStoringResults()}.
     */
    public static final boolean IS_STORING_RESULTS = true;

    /**
     * Default value returned by {@link JudgmentStorage#getMaxResults()}.
     */
    public static final int MAX_RESULTS = 10;

    /**
     * Default value returned by {@link JudgmentStorage#isRandomSwapping()}.
     */
    public static final boolean IS_RANDOM_SWAPPING = false;

    /**
     * Default value returned by
     * {@link JudgmentStorage#isSubmittingAutomatically()}.
     */
    public static final boolean IS_SUBMITTING_AUTOMATICALLY = false;

    /**
     * Default value returned by
     * {@link JudgmentStorage#getResultRetrievalTimeout()}.
     */
    public static final int RESULT_RETRIEVAL_TIMEOUT = 5000;
  }

  /**
   * Default values for {@link PreferencesStorage}. Only primitive values are
   * listed here; for any collection that can be returned, we assume that by
   * default the empty collection is returned; for any other data type, we
   * assume that by default {@code null} or the default instance is returned if
   * there is one.
   */
  public static final class PreferencesStorageDefaults {
    private PreferencesStorageDefaults() {
      // Only static, constant values, so prohibit instantiation.
    }

    /**
     * Default password accepted by
     * {@link PreferencesStorage#isPasswordCorrect(String)}.
     */
    public static final String PASSWORD = "test";

    /**
     * Default value returned by {@link PreferencesStorage#getPasswordHint()}.
     */
    public static final String PASSWORD_HINT = "";
  }

  /**
   * Default values for {@link QueryStorage}. Only primitive values are listed
   * here; for any collection that can be returned, we assume that by default
   * the empty collection is returned; for any other data type, we assume that
   * by default {@code null} or the default instance is returned if there is
   * one.
   */
  public static final class QueryStorageDefaults {
    private QueryStorageDefaults() {
      // Only static, constant values, so prohibit instantiation.
    }

    /**
     * Default value returned by {@link QueryStorage#isPreferringUnjudged()}.
     */
    public static final boolean IS_PREFERRING_UNJUDGED = false;

    /**
     * Default value returned by {@link QueryStorage#isShowingQuerySets()}.
     */
    public static final boolean IS_SHOWING_QUERY_SETS = false;
  }
}
