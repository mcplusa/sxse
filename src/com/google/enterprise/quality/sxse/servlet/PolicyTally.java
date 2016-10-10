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

import com.google.enterprise.quality.sxse.JudgmentDetails;
import com.google.enterprise.quality.sxse.ScoringPolicyProfile;
import com.google.enterprise.quality.sxse.JudgmentDetails.Judgment;
import com.google.enterprise.quality.sxse.gxp.PolicyTallyResults;
import com.google.gxp.html.HtmlClosure;

/**
 * An analytics metric that tallies the number of judgments for two scoring
 * policies.
 */
public class PolicyTally implements AnalyticsMetric {
  private final String firstProfileName;
  private final String secondProfileName;

  private final JudgmentCounter counter;

  /**
   * Constructs a new metric that tallies the number of judgments for two
   * scoring policy profiles.
   * 
   * @param firstProfile the first profile for tallying
   * @param secondProfile the second profile for tallying
   */
  public PolicyTally(ScoringPolicyProfile firstProfile,
      ScoringPolicyProfile secondProfile) {
    firstProfileName = (firstProfile != ScoringPolicyProfile.EMPTY_PROFILE) ?
        firstProfile.getName() : "No Results";
    secondProfileName = (secondProfile != ScoringPolicyProfile.EMPTY_PROFILE) ?
        secondProfile.getName() : "No Results";

    counter = new JudgmentCounter();
    reset();
  }

  public void reset() {
    counter.reset();
  }

  public void readJudgment(String user, JudgmentDetails details,
      Judgment judgment) {
    counter.count(judgment);
  }

  public AnalyticsResult getResult() {
    return new PolicyTallyResult(firstProfileName, secondProfileName,
        counter.getFirstPolicyCount(), counter.getSecondPolicyCount(),
        counter.getEqualCount());
  }

  /**
   * The result containing the tallies of judgments.
   */
  static class PolicyTallyResult implements AnalyticsResult {
    final String firstPolicyName;
    final String secondPolicyName;

    final int firstPolicyTally;
    final int secondPolicyTally;
    final int equalTally;

    private PolicyTallyResult(String firstPolicyName, String secondPolicyName,
        int firstPolicyTally, int secondPolicyTally, int equalTally) {
      this.firstPolicyName = firstPolicyName;
      this.secondPolicyName = secondPolicyName;

      this.firstPolicyTally = firstPolicyTally;
      this.secondPolicyTally = secondPolicyTally;
      this.equalTally = equalTally;
    }

    public String getTitle() {
      return "Tallying " + firstPolicyName + " versus " + secondPolicyName;
    }

    public HtmlClosure writeResults() {
      return PolicyTallyResults.getGxpClosure(firstPolicyName,
          secondPolicyName, firstPolicyTally, secondPolicyTally, equalTally);
    }
  }
}
