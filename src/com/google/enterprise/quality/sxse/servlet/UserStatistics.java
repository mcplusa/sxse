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

/**
 * A class that contains basic statistics about a user.
 */
class UserStatistics {
  private int numJudgments;
  private int numQueries;
  private long minTimestamp;
  private long maxTimestamp;

  public UserStatistics() {
    numJudgments = 0;
    minTimestamp = Long.MAX_VALUE;
    maxTimestamp = Long.MIN_VALUE;
  }

  /*
   * Updates the statistics given the timestamp of a new judgment.
   */
  public void update(long timestamp) {
    ++numJudgments;
    if (timestamp < minTimestamp) {
      minTimestamp = timestamp;
    }
    if (timestamp > maxTimestamp) {
      maxTimestamp = timestamp;
    }
  }

  /*
   * Sets the number of queries judged
   */
  public void setNumQueries(int numQueries) {
    this.numQueries = numQueries;
  }

  /**
   * @return the number of queries judged
   */
  public int getNumQueries() {
    return numQueries;
  }

  /**
   * @return the number of judgments issued
   */
  public int getNumJudgments() {
    return numJudgments;
  }

  /**
   * @return the timestamp of the first judgment issued by the user
   */
  public long getMinTimestamp() {
    return (numJudgments == 0) ? -1 : minTimestamp;
  }

  /**
   * @return the timestamp of the last judgment issued by the user
   */
  public long getMaxTimestamp() {
    return (numJudgments == 0) ? -1 : maxTimestamp;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('{');
    sb.append("numJudgments=").append(numJudgments);
    sb.append(", numQueries=").append(numQueries);
    sb.append(", minTimestamp=").append(minTimestamp);
    sb.append(", maxTimestamp=").append(maxTimestamp);
    sb.append('}');
    return sb.toString();
  }
}
