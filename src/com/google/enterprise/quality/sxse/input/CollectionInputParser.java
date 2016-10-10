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
 * A {@link InputParser} for GSA collection names.
 */
public class CollectionInputParser {
  public static enum ParseError {
    NO_VALUE,
    INVALID_CHARACTERS,
    TOO_LONG,
    INVALID_NAME,
  }

  private static final StringInputParser helperParser = StringInputParser.builder()
      .removeChar('/')
      .removeChar(',')
      .removeChar('@')
      .setMaxLength(128)
      .setMode(StringInputParser.Mode.EXCLUSIVE)
      .build();

  private static final String INVALID_MESSAGE = "Invalid collection: ";
  private static final ErrorTransformer<StringInputParser.ParseError, ParseError> translator =
      ErrorTransformer.builder(StringInputParser.ParseError.class, ParseError.class)
        .add(StringInputParser.ParseError.NO_VALUE, ParseError.NO_VALUE,
          INVALID_MESSAGE)
        .add(StringInputParser.ParseError.INVALID_CHARACTERS, ParseError.INVALID_CHARACTERS,
          INVALID_MESSAGE)
        .add(StringInputParser.ParseError.TOO_LONG, ParseError.TOO_LONG,
          INVALID_MESSAGE)
        .build();

  public static final CollectionInputParser INSTANCE = new CollectionInputParser();

  private CollectionInputParser() {
  }

  public ParsedInput parse(String paramName, Map paramMap) {
    String[] inputValues = (String[]) paramMap.get(paramName);
    return parse((inputValues != null) ? inputValues[0] : null);
  }

  public ParsedInput parse(String inputValue) {
    StringInputParser.ParsedInput helperInput = helperParser.parse(inputValue);
    if (!helperInput.hasResult()) {
      return new ParsedInput(inputValue, translator.transform(helperInput.getErrors()));
    }

    if (inputValue.equals(".") || inputValue.equals("..")) {
      return new ParsedInput(inputValue, InputErrors.of(
          ParseError.INVALID_NAME, "Value is not allowed"));
    }
    return new ParsedInput(inputValue, inputValue);
  }

  /**
   * The parsed input by a {@link CollectionInputParser}.
   */
  public static class ParsedInput extends AbstractParsedInput<String> {
    private final String inputValue;
    private final InputErrors<ParseError> inputErrors;

    private ParsedInput(String inputValue, InputErrors<ParseError> inputErrors) {
      super(null);
      this.inputValue = inputValue;
      this.inputErrors = inputErrors;
    }

    private ParsedInput(String inputValue, String result) {
      super(result);
      this.inputValue = inputValue;
      this.inputErrors = InputErrors.getEmpty();
    }

    /**
     * @return the field entered
     */
    public String getInputValue() {
      return inputValue;
    }

    /**
     * @return the input errors
     */
    public InputErrors<ParseError> getErrors() {
      return inputErrors;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append('{');
      super.appendToString(sb);
      sb.append(", inputValue=").append(inputValue);
      sb.append(", inputErrors=").append(inputErrors);
      sb.append('}');
      return sb.toString();
    }
  }
}
