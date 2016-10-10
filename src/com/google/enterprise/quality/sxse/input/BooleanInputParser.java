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

import java.util.Map;

/**
 * An {@link InputParser} for boolean values.
 */
public class BooleanInputParser implements InputParser<Boolean> {
  public static final BooleanInputParser INSTANCE = new BooleanInputParser();

  private static final ParsedInput TRUE_INPUT = new ParsedInput(Boolean.TRUE);
  private static final ParsedInput FALSE_INPUT = new ParsedInput(Boolean.FALSE);

  private BooleanInputParser() {
  }

  public ParsedInput parse(String paramName, Map paramMap) {
    if (paramMap.containsKey(paramName)) {
      return TRUE_INPUT;
    }
    return FALSE_INPUT;
  }

  /**
   * The parsed input by a {@link BooleanInputParser}.
   */
  public static class ParsedInput extends AbstractParsedInput<Boolean> {
    private ParsedInput(Boolean result) {
      super(result);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append('{');
      super.appendToString(sb);
      sb.append('}');
      return sb.toString();
    }
  }
}
