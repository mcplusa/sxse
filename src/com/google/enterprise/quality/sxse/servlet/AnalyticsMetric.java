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
import com.google.enterprise.quality.sxse.JudgmentDetails.Judgment;

/**
 * A interface for any object that computes a metric for analysis of user
 * judgments.
 */
interface AnalyticsMetric {
  /**
   * Resets the internal state of this metric, so that it is equal to when it
   * was first created.
   */
  public void reset();

  /**
   * Notifies this object of the given judgment that passed all filtering
   * criteria established by the administrator. This object can then factor the
   * judgment into the metrics computation as necessary. The {@link Judgment}
   * parameter may be reversed from than that returned by
   * {@link JudgmentDetails#getJudgment()} if the host and query arguments in
   * the details are reversed from those passed into the calling filter, and
   * should be used as the true value of the judgment.
   * 
   * @param user the user that made the judgment
   * @param details the details of the judgment
   * @param judgment the potentially reversed judgment
   */
  public void readJudgment(String user, JudgmentDetails details,
      Judgment judgment);

  /**
   * @return the result of the analytics computation
   */
  public AnalyticsResult getResult();
}
