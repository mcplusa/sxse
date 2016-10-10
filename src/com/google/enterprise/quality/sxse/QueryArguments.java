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
 * All arguments of a query, including the frontend, collection, and extra GET
 * arguments used.
 */
public class QueryArguments {
  public static final QueryArguments EMPTY_QUERY_ARGS = new QueryArguments() {
    @Override
    public boolean equals(Object obj) {
      return (obj == EMPTY_QUERY_ARGS);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Empty query arguments");
      return sb.toString();
    }
  };

  private final String collection;
  private final String frontend;
  private final String extraParams;

  // Only to be used by static field EMPTY_QUERY_ARGS.
  private QueryArguments() {
    collection = null;
    frontend = null;
    extraParams = null;
  }

  /**
   * Creates a new set of query arguments.
   * 
   * @param collection the collection searched in
   * @param frontend the frontend that handled the search
   * @param extraParams the extra parameters used
   */
  public QueryArguments(String collection, String frontend,
      String extraParams) {
    this.collection = Preconditions.checkNotNull(collection);
    this.frontend = Preconditions.checkNotNull(frontend);
    this.extraParams = Preconditions.checkNotNull(extraParams);
  }

  /**
   * @return the collection searched in.
   */
  public String getCollection() {
    return collection;
  }
  
  /**
   * @return the frontend that handled the search.
   */
  public String getFrontend() {
    return frontend;
  }

  /**
   * @return the extra parameters used in the search.
   */
  public String getExtraParams() {
    return extraParams;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof QueryArguments) {
      final QueryArguments qa = (QueryArguments) obj;
      return (collection.equals(qa.collection) 
          && frontend.equals(qa.frontend)
          && extraParams.equals(qa.extraParams));
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(collection, frontend, extraParams);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(1024);
    sb.append('{');
    sb.append("collection=").append(collection);
    sb.append(", frontend=").append(frontend);
    sb.append(", extra params=").append(extraParams);
    sb.append('}');
    return sb.toString();
  }
}
