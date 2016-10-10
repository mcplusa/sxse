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

import com.google.common.base.Preconditions;
import com.google.enterprise.quality.sxse.QueryFormatter;
import com.google.enterprise.quality.sxse.ScoringPolicyProfile;
import com.google.enterprise.quality.sxse.SearchResult;

import java.util.List;
import java.util.UUID;

/**
 * Side-by-side results under review by an assessor.
 */
final class SideBySideResults {
  private final UUID uuid;

  private final String query;
  private final QueryFormatter firstQueryFormatter;
  private final QueryFormatter secondQueryFormatter;
  private final List<SearchResult> firstResults;
  private final List<SearchResult> secondResults;

  /**
   * Creates new side-by-side results.
   * 
   * @param query the query issued
   * @param firstProfile the scoring policy profile that generated the first
   *                     list of results
   * @param secondProfile the scoring policy profile that generated the second
   *                      list of results
   * @param firstResults the first list of results, or {@code null} if they
   *                     could not be retrieved from the server, or are
   *                     displayed in iframes
   * @param secondResults the second list of results, or {@code null} if they
   *                      could not be retrieved from the server, or are
   *                      displayed in iframes
   */
  public SideBySideResults(String query,
      ScoringPolicyProfile firstProfile, ScoringPolicyProfile secondProfile,
      List<SearchResult> firstResults, List<SearchResult> secondResults) {
    uuid = UUID.randomUUID();
    
    this.query = Preconditions.checkNotNull(query);
    Preconditions.checkNotNull(firstProfile);
    Preconditions.checkNotNull(secondProfile);
    this.firstQueryFormatter = firstProfile.getQueryFormatter();
    this.secondQueryFormatter = secondProfile.getQueryFormatter();

    this.firstResults = firstResults;
    this.secondResults = secondResults;
  }

  /**
   * @return the unique identifier for these results, decied upon construction
   */
  public UUID getUuid() {
    return uuid;
  }

  /**
   * @return the query issued
   */
  public String getQuery() {
    return query;
  }

  /**
   * @return the query formatter of the scoring policy profile that generated
   *         the first list of results
   */
  public QueryFormatter getFirstQueryFormatter() {
    return firstQueryFormatter;
  }

  /**
   * @return the query formatter of the scoring policy profile that generated
   *         the second list of results
   */
  public QueryFormatter getSecondQueryFormatter() {
    return secondQueryFormatter;
  }

  /**
   * @return the first list of results, or possibly {@code null}
   */
  public List<SearchResult> getFirstResults() {
    return firstResults;
  }

  /**
   * @return the second list of results, or possibly {@code null}
   */
  public List<SearchResult> getSecondResults() {
    return secondResults;
  }
}
