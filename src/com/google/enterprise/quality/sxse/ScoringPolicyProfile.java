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

import com.google.common.base.Preconditions;
import com.google.enterprise.quality.sxse.QueryFormatter.FormatterType;

/**
 * The profile of a scoring policy to run queries against.
 */
public class ScoringPolicyProfile {
  /**
   * The empty scoring policy profile that has no name, host, or query
   * arguments. Using this profile will show no results in evaluation.
   */
  public static final ScoringPolicyProfile EMPTY_PROFILE =
      new ScoringPolicyProfile() {
    @Override
    public String toString() {
      return "Blank profile";
    }
  };

  private final String name;
  private final QueryFormatter queryFormatter;

  private ScoringPolicyProfile() {
    name = null;
    queryFormatter = QueryFormatter.EMPTY_FORMATTER;
  }

  /**
   * Creates a new scoring policy profile.
   * 
   * @param name the profile name
   * @param queryFormatter the query formatter
   */
  public ScoringPolicyProfile(String name, QueryFormatter queryFormatter) {
    Preconditions.checkArgument(
        queryFormatter.getFormatterType() != FormatterType.EMPTY);
    this.name = Preconditions.checkNotNull(name);
    this.queryFormatter = queryFormatter;
  }

  /**
   * @return the name of the profile
   */
  public String getName() {
    return name;
  }

  /**
   * @return the query formatter
   */
  public QueryFormatter getQueryFormatter() {
    return queryFormatter;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('{');
    sb.append("name=").append(name);
    sb.append(", queryFormatter=").append(queryFormatter);
    sb.append('}');
    return sb.toString();
  }
}
