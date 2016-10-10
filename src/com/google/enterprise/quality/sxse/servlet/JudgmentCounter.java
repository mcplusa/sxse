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

import com.google.enterprise.quality.sxse.JudgmentDetails.Judgment;

/**
 * A class that, for a collection of {@link Judgment} instances, keeps a count
 * for each possible value.
 */
class JudgmentCounter {
  private int firstPolicyCount;
  private int secondPolicyCount;
  private int equalCount;

  /**
   * Constructs a new counter with all counts initialized to 0.
   */
  public JudgmentCounter() {
    reset();
  }

  /**
   * Resets the counts for all judgment values.
   */
  public void reset() {
    firstPolicyCount = 0;
    secondPolicyCount = 0;
    equalCount = 0;
  }

  /**
   * Increments the count corresponding to the value of the given judgment.
   * 
   * @param judgment the judgment to count
   */
  public void count(Judgment judgment) {
    switch (judgment) {
    case EQUAL:
      ++equalCount;
      break;
    case FIRST_BETTER:
      ++firstPolicyCount;
      break;
    case SECOND_BETTER:
      ++secondPolicyCount;
      break;
    }
  }

  /**
   * @return the count for {@link Judgment#FIRST_BETTER}
   */
  public int getFirstPolicyCount() {
    return firstPolicyCount;
  }

  /**
   * @return the count for {@link Judgment#SECOND_BETTER}
   */
  public int getSecondPolicyCount() {
    return secondPolicyCount;
  }

  /**
   * @return the count for {@link Judgment#EQUAL}
   */
  public int getEqualCount() {
    return equalCount;
  }
}
