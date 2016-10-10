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
 * An {@link InputParser} for enum values.
 */
public class EnumInputParser<E extends Enum<E>> implements InputParser<E> {
  public static enum ParseError {
    NO_VALUE,
    INVALID_VALUE,
  }

  private final Class<E> enumClass;

  public EnumInputParser(Class<E> enumClass) {
    this.enumClass = enumClass;
  }

  public ParsedInput<E> parse(String paramName, Map paramMap) {
    String[] inputValues = (String[]) paramMap.get(paramName);
    return parse((inputValues != null) ? inputValues[0] : null);
  }

  public ParsedInput<E> parse(String inputValue) {
    if (inputValue == null) {
      InputErrors.Builder<ParseError> errorsBuilder =
          InputErrors.builder(ParseError.class);
      errorsBuilder.setError(ParseError.NO_VALUE, "No value provided");
      return new ParsedInput<E>("", errorsBuilder.build());
    }

    E enumValue = null;
    try {
      enumValue = Enum.valueOf(enumClass, inputValue);
    } catch (IllegalArgumentException e) {
      InputErrors.Builder<ParseError> errorsBuilder =
          InputErrors.builder(ParseError.class);
      errorsBuilder.setError(ParseError.INVALID_VALUE,
          "Invalid value: " + inputValue);
      return new ParsedInput<E>(inputValue, errorsBuilder.build());
    }

    return new ParsedInput<E>(inputValue, enumValue);
  }

  /**
   * The parsed input by an {@link EnumInputParser}.
   */
  public static class ParsedInput<E extends Enum<E>> extends AbstractParsedInput<E> {
    private final String inputValue;
    private final InputErrors<ParseError> inputErrors;

    private ParsedInput(String inputValue, InputErrors<ParseError> inputErrors) {
      super(null);
      this.inputValue = inputValue;
      this.inputErrors = inputErrors;
    }

    private ParsedInput(String inputValue, E result) {
      super(result);
      this.inputValue = inputValue;
      this.inputErrors = InputErrors.getEmpty();
    }

    /**
     * @return the field as entered
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
