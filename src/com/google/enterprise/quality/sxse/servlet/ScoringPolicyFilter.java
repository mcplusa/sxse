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

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.enterprise.quality.sxse.JudgmentDetails;
import com.google.enterprise.quality.sxse.QueryFormatter;
import com.google.enterprise.quality.sxse.ScoringPolicyProfile;

/**
 * A {@link Predicate} that returns whether a {@link JudgmentDetails} contains
 * the host and query arguments of the given {@link ScoringPolicyProfile}
 * instances. The ordering of the profiles does not matter. We use this to
 * generate the history for a assessor that only contains queries run against
 * the current profiles.
 */
class ScoringPolicyFilter implements Predicate<JudgmentDetails> {
  private final QueryFormatter firstQueryFormatter;
  private final QueryFormatter secondQueryFormatter;

  /**
   * Creates a new predicate for matching against the given scoring policy
   * profiles.
   * 
   * @param firstProfile the profile containing the first host and query
   *        arguments to match against
   * @param secondProfile the profile containing the second host and query
   *        arguments to match against
   */
  public ScoringPolicyFilter(ScoringPolicyProfile firstProfile,
      ScoringPolicyProfile secondProfile) {
    // If either profile is EMPTY_PROFILE, will assign null here.
    firstQueryFormatter = firstProfile.getQueryFormatter();
    secondQueryFormatter = secondProfile.getQueryFormatter();
  }

  /**
   * @return the first query formatter to match against
   */
  public QueryFormatter getFirstQueryFormatter() {
    return firstQueryFormatter;
  }

  /**
   * @return the second query formatter to match against
   */
  public QueryFormatter getSecondQueryFormatter() {
    return secondQueryFormatter;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('{');
    sb.append("firstQueryFormatter=").append(firstQueryFormatter);
    sb.append(", secondQueryFormatter=").append(secondQueryFormatter);
    sb.append('}');
    return sb.toString();
  }

  public boolean apply(JudgmentDetails details) {
    QueryFormatter firstDetailsQueryFormatter = details.getFirstQueryFormatter();
    QueryFormatter secondDetailsQueryFormatter = details.getSecondQueryFormatter();
    return
        (Objects.equal(firstQueryFormatter, firstDetailsQueryFormatter) &&
          Objects.equal(secondQueryFormatter, secondDetailsQueryFormatter)) ||
        (Objects.equal(secondQueryFormatter, firstDetailsQueryFormatter) &&
          Objects.equal(firstQueryFormatter, secondDetailsQueryFormatter));
  }
}
