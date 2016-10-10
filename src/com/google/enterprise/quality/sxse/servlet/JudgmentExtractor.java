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

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.enterprise.quality.sxse.JudgmentDetails;
import com.google.enterprise.quality.sxse.QueryFormatter;
import com.google.enterprise.quality.sxse.JudgmentDetails.Judgment;

/**
 * A {@link Function} that returns the judgment from a {@link JudgmentDetails}
 * that is accepted by a corresponding {@link ScoringPolicyFilter}. The function
 * rewrites the judgment if it finds that the ordering of the host and query
 * arguments is reversed. If the host or query arguments are not equal, meaning
 * the judgment is not accepted by the filter, the
 * {@link #apply(JudgmentDetails)} method throws an
 * {@link IllegalArgumentException}.
 */
class JudgmentExtractor implements Function<JudgmentDetails, Judgment> {
  private final ScoringPolicyFilter scoringPolicyFilter;

  /**
   * Creates a new function for extracting judgments accepted by the given
   * {@link ScoringPolicyFilter}.
   * 
   * @param scoringPolicyFilter the filter for judgments
   */
  public JudgmentExtractor(ScoringPolicyFilter scoringPolicyFilter) {
    this.scoringPolicyFilter = scoringPolicyFilter;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(1024);
    sb.append('{');
    sb.append("scoringPolicyFilter=").append(scoringPolicyFilter);
    sb.append('}');
    return sb.toString();
  }

  public Judgment apply(JudgmentDetails details) {
    QueryFormatter firstQueryFormatter = details.getFirstQueryFormatter();
    QueryFormatter secondQueryFormatter = details.getSecondQueryFormatter();
    if (Objects.equal(scoringPolicyFilter.getFirstQueryFormatter(),
          firstQueryFormatter) &&
        Objects.equal(scoringPolicyFilter.getSecondQueryFormatter(),
          secondQueryFormatter)) {
      return details.getJudgment();
    }
    if (Objects.equal(scoringPolicyFilter.getFirstQueryFormatter(),
          secondQueryFormatter) &&
        Objects.equal(scoringPolicyFilter.getSecondQueryFormatter(),
          firstQueryFormatter)) {
      // Profiles not in same order, so rewrite judgment.
      return AbstractJudgmentStrategy.swapJudgment(details.getJudgment());
    }

    throw new IllegalArgumentException(
        "JudgmentDetails instance not accepted by filter: "
          + scoringPolicyFilter);
  }
}
