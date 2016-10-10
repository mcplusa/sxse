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

import com.google.common.collect.Lists;
import com.google.enterprise.quality.sxse.JudgmentDetails;
import com.google.enterprise.quality.sxse.ScoringPolicyProfile;
import com.google.enterprise.quality.sxse.JudgmentDetails.Judgment;
import com.google.enterprise.quality.sxse.gxp.PolicyProbabilityResults;
import com.google.gxp.html.HtmlClosure;

import java.util.List;

/**
 * An analytics metric that calculates the probability of each policy being
 * better, with confidence limits.
 *
 * The computation is based on a "coin toss" model, in which we designate one
 * outcome as head and one as tails. The probability that head is preferred is
 * then p = H/H+T where H and T are the counts of heads and tails. The
 * confidence interval is p-E to p+E where E = Z/(2 * sqrt(H+T)), and Z is the
 * Z-value of a normal distribution for the desired level of confidence, for
 * example Z=1.96 for 95% confidence.
 *
 * We have two ways of calculating H and T to allow for equal
 * judgments. Either we test definite judgments for policy A against other
 * outcomes, in which case H = count for A and T = count for B + count for
 * equal. Or, we test policy A against policy B, dividing the equal counts
 * between then, giving H = count for A + .5 count for equal, and similarly
 * for T. A flag to the constructor sets which mode we want.
 *
 * The underlying theory says that this is only reliable when we get above a
 * certain amount of data (below this, a more exact test based on the
 * binomial distribution would be needed). The test is that each probability
 * is greater than or equal to 5/(H+T).
 */
public class PolicyProbability implements AnalyticsMetric {
  private final String firstProfileName;
  private final String secondProfileName;

  private final JudgmentCounter counter;
  private final boolean distributeEqualCounts;
  private final ConfidenceLevel confidenceLevels[];

  /** The confidence levels we can compute.
   */
  public static enum ConfidenceLevel {
    /** 50% confidence */
    CONFIDENCE_50 (0.50, 0.6745),

    /** 68% confidence (one standard deviation) */
    CONFIDENCE_68 (0.68, 1.0),

    /** 90% confidence */
    CONFIDENCE_90 (0.90, 1.6449),

    /** 95% confidence */
    CONFIDENCE_95 (0.95, 1.9599),

    /** 99% confidence */
    CONFIDENCE_99 (0.99, 2.5759);

    private final double confidence;
    private final double z;

    /**
     * Construct a confidence level, setting the actual Z-value.
     * @param confidence the confidence level.
     * @param z the Z-value of a standard normal distribution.
     */
    ConfidenceLevel(double confidence, double z) {
      this.confidence = confidence;
      this.z = z;
    }

    /**
     * @return the confidence level (between 0 and 1).
     */
    public double getConfidence() {
      return confidence;
    }

    /**
     * @return the z value associated with this confidence level.
     */
    public double getZ() {
      return z;
    }
  }

  /**
   * Constructs a new metric that tallies the number of judgments for two
   * scoring policy profiles, and the computes probabilities that each policy
   * is better, with confidence limits.
   *
   * @param firstProfile the first profile
   * @param secondProfile the second profile
   * @param distributeEqualCounts if true, equal judgments are distributed
   * equally between the two policies; if false, we test A vs non-A.
   * @param confidenceLevels array of the confidence levels we will compute
   * results for.
   */
  public PolicyProbability(
      ScoringPolicyProfile firstProfile,
      ScoringPolicyProfile secondProfile,
      boolean distributeEqualCounts,
      ConfidenceLevel... confidenceLevels) {
    firstProfileName = (firstProfile != ScoringPolicyProfile.EMPTY_PROFILE) ?
        firstProfile.getName() : "No Results";
    secondProfileName = (secondProfile != ScoringPolicyProfile.EMPTY_PROFILE) ?
        secondProfile.getName() : "No Results";

    counter = new JudgmentCounter();
    this.distributeEqualCounts = distributeEqualCounts;
    this.confidenceLevels = confidenceLevels.clone();

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
    double firstPolicyProbability;
    double secondPolicyProbability;
    PolicyProbabilityResult result;

    int firstPolicyCount = counter.getFirstPolicyCount();
    int secondPolicyCount = counter.getSecondPolicyCount();
    int equalCount = counter.getEqualCount();
    int total = firstPolicyCount + secondPolicyCount + equalCount;
    if (total > 0) {
      double totalCount = total;

      if (distributeEqualCounts) {
        firstPolicyProbability
            = (firstPolicyCount + equalCount / 2.0) / totalCount;
        secondPolicyProbability
            = (secondPolicyCount + equalCount / 2.0) / totalCount;
      } else {
        firstPolicyProbability = firstPolicyCount / totalCount;
        secondPolicyProbability = secondPolicyCount / totalCount;
      }

      double limit = 5 / totalCount;
      boolean reliable = (firstPolicyProbability >= limit)
          && (secondPolicyProbability >= limit);

      result = new PolicyProbabilityResult(
          firstProfileName, secondProfileName,
          firstPolicyProbability, secondPolicyProbability, reliable,
          confidenceLevels.length);

      double rootCount = 2 * Math.sqrt(totalCount);
      for (ConfidenceLevel level : confidenceLevels) {
        double errorBound = level.getZ() / rootCount;
        result.addConfidenceRange(level.getConfidence(), errorBound);
      }
    } else {
      result = new PolicyProbabilityResult(
          firstProfileName, secondProfileName, -1, -1, false, 0);
    }

    return result;
  }

  public static final class RangeConfidencePair {
    private final double confidence;
    private final double range;
    
    private RangeConfidencePair(double confidence, double range) {
      this.confidence = confidence;
      this.range = range;
    }
    
    public double getConfidence() {
      return confidence;
    }
    
    public double getRange() {
      return range;
    }
  }

  /**
   * The result containing the probabilities and confidence ranges
   */
  static final class PolicyProbabilityResult implements AnalyticsResult {
    final String firstPolicyName;
    final String secondPolicyName;

    // Use -1 for probabilities when we didn't get any counts at all
    final double firstPolicyProbability;
    final double secondPolicyProbability;

    final boolean reliable;

    // Confidence limits containing the confidence level (e.g. 0.90) and the
    // range on the probabilities. The range is the same for both the first
    // and second policies.
    List<RangeConfidencePair> ranges;

    private PolicyProbabilityResult(
        String firstPolicyName, String secondPolicyName,
        double firstPolicyProbability, double secondPolicyProbability,
        boolean reliable, int levels) {
      this.firstPolicyName = firstPolicyName;
      this.secondPolicyName = secondPolicyName;

      this.firstPolicyProbability = firstPolicyProbability;
      this.secondPolicyProbability = secondPolicyProbability;

      this.reliable = reliable;

      this.ranges = Lists.newArrayListWithExpectedSize(levels);
    }

    public String getTitle() {
      return "Comparing probabilities of " + firstPolicyName
          + " and " + secondPolicyName;
    }

    private void addConfidenceRange(double confidence, double range) {
      ranges.add(new RangeConfidencePair(confidence, range));
    }

    public HtmlClosure writeResults() {
      return PolicyProbabilityResults.getGxpClosure(
          firstPolicyName, secondPolicyName,
          firstPolicyProbability, secondPolicyProbability,
          reliable, ranges);
    }
  }
}
