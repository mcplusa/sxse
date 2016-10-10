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

package com.google.enterprise.quality.sxse.input;

/**
 * A parsed input, consisting of either the parsed value or the errors found
 * while attempting to parse.
 */
public abstract class AbstractParsedInput<T> {
  private T result;

  protected AbstractParsedInput(T result) {
    this.result = result;
  }

  /**
   * @return {@code true} if the result was parsed successfully, {@code false}
   *         otherwise
   */
  public boolean hasResult() {
    return (result != null);
  }

  /**
   * @return the result, or {@code null} if it could not be parsed
   */
  public T getResult() {
    return result;
  }

  protected void appendToString(StringBuilder sb) {
    sb.append("result=").append(result);
  }
}
