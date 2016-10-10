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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An {@link InputParser} for lists of string values.
 */
public class ListStringInputParser implements InputParser<List<String>> {
  public static final ListStringInputParser INSTANCE = new ListStringInputParser();

  private ListStringInputParser() {
  }

  public ParsedInput parse(String paramName, Map paramMap) {
    String[] values = (String[]) paramMap.get(paramName);
    if ((values == null) || (values.length == 0)) {
      return new ParsedInput(Collections.<String>emptyList());
    }
    return new ParsedInput(Arrays.asList(values));
  }

  /**
   * The parsed input by a {@link ListStringInputParser}.
   */
  public static class ParsedInput extends AbstractParsedInput<List<String>> {
    private ParsedInput(List<String> result) {
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
