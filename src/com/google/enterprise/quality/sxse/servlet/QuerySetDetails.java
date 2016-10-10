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
 * The details of a query set.
 */
public class QuerySetDetails {
  private final String querySetName;
  private final boolean isActive;

  /**
   * Creates the details of a query set.
   * 
   * @param querySetName the name of the query set
   * @param isActive {@code true} if the query set is active, {@code false}
   *        otherwise
   */
  public QuerySetDetails(String querySetName, boolean isActive) {
    this.querySetName = querySetName;
    this.isActive = isActive;
  }

  /**
   * @return the query set name
   */
  public String getQuerySetName() {
    return querySetName;
  }

  /**
   * @return {@code true} if the query set is active, {@code false} otherwise
   */
  public boolean isActive() {
    return isActive;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('{');
    sb.append("querySetname=").append(querySetName);
    sb.append(", isActive=").append(isActive);
    sb.append('}');
    return sb.toString();
  }
}
