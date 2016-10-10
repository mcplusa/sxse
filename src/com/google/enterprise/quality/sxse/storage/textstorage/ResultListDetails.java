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

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.enterprise.quality.sxse.QueryFormatter;
import com.google.enterprise.quality.sxse.ScoringPolicyProfile;

/**
 * Details of a list of search results, including the query formatter of a
 * scoring policy profile, and the hash of the results if SxSE has access to
 * them.
 */
public class ResultListDetails {
  private final QueryFormatter queryFormatter;
  private final String resultHash;

  /**
   * Creates new details for a list of search results.
   * 
   * @param spp the scoring policy profile containing the query formatter to use
   * @param resultHash the hash of the results returned by the host, or
   *        {@code null} if SxSE does not have access to the results
   */
  public ResultListDetails(ScoringPolicyProfile spp, String resultHash) {
    this(spp.getQueryFormatter(), resultHash);
  }

  /**
   * Creates new details for a list of search results.
   * 
   * @param queryFormatter the queryFormatter used when the query was issued
   * @param resultHash the hash of the results returned by the host, or
   *        {@code null} if SxSE does not have access to the results
   */
  public ResultListDetails(QueryFormatter queryFormatter,
      String resultHash) {
    this.queryFormatter = Preconditions.checkNotNull(queryFormatter);
    this.resultHash = resultHash;
  }

  /**
   * @return the query formatter
   */
  public final QueryFormatter getQueryFormatter() {
    return queryFormatter;
  }

  /**
   * @return the hash of the results returned by the host, or {@code null} if
   *         SxSE does not have access to the results
   */
  public final String getResultHash() {
    return resultHash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (obj instanceof ResultListDetails) {
      final ResultListDetails rld = (ResultListDetails) obj;
      return (queryFormatter.equals(rld.queryFormatter) &&
          Objects.equal(resultHash, rld.resultHash));
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(queryFormatter, resultHash);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(1024);
    sb.append('{');
    sb.append("queryFormatter=").append(queryFormatter);
    sb.append(", resultHash=").append(resultHash);
    sb.append('}');
    return sb.toString();
  }
}
