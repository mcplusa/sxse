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

/**
 * A pairing of a host and query arguments, typically belonging to a
 * {@link QueryFormatter} of type {@link QueryFormatter.FormatterType#GSA}.
 */
public class HostQueryArgsPair {
  public static final HostQueryArgsPair EMPTY_HOST_QUERY_ARGS =
      new HostQueryArgsPair() {
    @Override
    public boolean equals(Object obj) {
      return (obj == EMPTY_HOST_QUERY_ARGS);
    }

    @Override
    public String toString() {
      return "Empty host and query arguments";
    }
  };

  private final String host;
  private final QueryArguments queryArguments;

  // Only to be used by static field EMPTY_QUERY_ARGS.
  private HostQueryArgsPair() {
    this.host = null;
    this.queryArguments = QueryArguments.EMPTY_QUERY_ARGS;
  }

  /**
   * Creates a new pairing of a host and query arguments.
   * 
   * @param host the host
   * @param queryArguments the query arguments
   */
  public HostQueryArgsPair(String host, QueryArguments queryArguments) {
    this.host = Preconditions.checkNotNull(host);
    this.queryArguments = Preconditions.checkNotNull(queryArguments);
  }

  /**
   * @return the host.
   */
  public String getHost() {
    return host;
  }
  
  /**
   * @return the query arguments.
   */
  public QueryArguments getQueryArguments() {
    return queryArguments;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof HostQueryArgsPair) {
      HostQueryArgsPair other = (HostQueryArgsPair) obj;
      return (host.equals(other.host) &&
          queryArguments.equals(other.queryArguments));
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(host, queryArguments);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(1024);
    sb.append('{');
    sb.append("host=").append(host);
    sb.append(", queryArguments=").append(queryArguments);
    sb.append('}');
    return sb.toString();
  }
}
