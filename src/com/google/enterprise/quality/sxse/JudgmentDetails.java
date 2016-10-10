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

package com.google.enterprise.quality.sxse;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.enterprise.quality.sxse.storage.JudgmentStorage;

/**
 * The details of a judgment issued by a user.
 */
public class JudgmentDetails {
  /**
   * The judgment issued by the user, declaring the results from one scoring
   * policy better than the other, or each having equal quality.
   */
  public static enum Judgment {
    /**
     * The results from the first scoring policy are better.
     */
    FIRST_BETTER,

    /**
     * The results from both scoring policies are of equal quality.
     */
    EQUAL,

    /**
     * The results of the second scoring policy are better.
     */
    SECOND_BETTER
  }

  private final String query;
  private final Judgment judgment;
  private final long timestamp;
  private final QueryFormatter firstQueryFormatter;
  private final QueryFormatter secondQueryFormatter;
  private final String resultsId;

  /**
   * Creates new details for the judgment issued by the user.
   * 
   * @param query the query issued in the comparison
   * @param judgment the judgment given in the comparison
   * @param timestamp the time at which the judgment was issued
   * @param firstQueryFormatter the query formatter of the first results
   * @param secondQueryFormatter the query formatter of the second results
   * @param resultsId the unique identifier of the results in
   *        {@link JudgmentStorage}, or {@code null} if no results exist
   */
  public JudgmentDetails(String query, Judgment judgment, long timestamp,
      QueryFormatter firstQueryFormatter, QueryFormatter secondQueryFormatter,
      String resultsId) {
    this.query = Preconditions.checkNotNull(query, "query required");
    this.judgment = Preconditions.checkNotNull(judgment, "judgment required");
    this.timestamp = Preconditions.checkNotNull(timestamp, "timestamp required");
    this.firstQueryFormatter = Preconditions.checkNotNull(firstQueryFormatter,
        "firstQueryFormatter required");
    this.secondQueryFormatter = Preconditions.checkNotNull(secondQueryFormatter,
        "secondQueryFormatter required");
    this.resultsId = resultsId;
  }

  /**
   * @return the query issued in the comparison.
   */
  public final String getQuery() {
    return query;
  }

  /**
   * @return the judgment given in the comparison.
   */
  public final Judgment getJudgment() {
    return judgment;
  }

  /**
   * @return the time at which the judgment was issued.
   */
  public final long getTimestamp() {
    return timestamp;
  }

  /**
   * @return the query formatter used to generate the first results.
   */
  public final QueryFormatter getFirstQueryFormatter() {
    return firstQueryFormatter;
  }

  /**
   * @return the query formatter used to generate the second results.
   */
  public final QueryFormatter getSecondQueryFormatter() {
    return secondQueryFormatter;
  }

  /**
   * @return the identifier of the 
   */
  public final String getResultsId() {
    return resultsId;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (obj instanceof JudgmentDetails) {
      final JudgmentDetails jd = (JudgmentDetails) obj;
      return (query.equals(jd.query) && judgment.equals(jd.judgment)
          && (timestamp == jd.timestamp)
          && firstQueryFormatter.equals(jd.firstQueryFormatter)
          && secondQueryFormatter.equals(jd.secondQueryFormatter)
          && Objects.equal(resultsId, jd.resultsId));
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(query, judgment, timestamp, 
        firstQueryFormatter, secondQueryFormatter, resultsId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(1024);
    sb.append('{');
    sb.append("query=").append(query).append('\n');
    sb.append(", judgment=").append(judgment).append('\n');
    sb.append(", timestamp=").append(timestamp).append('\n');
    sb.append(", firstQueryFormatter=").append(firstQueryFormatter);
    sb.append(", secondQueryFormatter=").append(secondQueryFormatter);
    sb.append(", resultsId=").append(resultsId);
    sb.append('}');
    return sb.toString();
  }
}
