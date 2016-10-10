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
 * A {@link InputParser} for floating point numbers.
 */
public class FloatInputParser implements InputParser<Float> {
  public static enum ParseError {
    NO_VALUE,
    INVALID_NUMBER,
    TOO_SMALL,
    TOO_LARGE,
  }

  private static final FloatInputParser ALLOW_ALL_INSTANCE =
      new FloatInputParser(Float.MIN_VALUE, Float.MAX_VALUE);

  /**
   * @return a parser that accepts all floating point values
   */
  public static FloatInputParser allowAll() {
    return ALLOW_ALL_INSTANCE;
  }

  private final float minValue;
  private final float maxValue;

  /**
   * Creates a new parser that accepts values in the given range, inclusive at
   * both ends.
   * 
   * @param minValue the minimum value to accept
   * @param maxValue the maximum value to accept
   */
  public FloatInputParser(float minValue, float maxValue) {
    this.minValue = minValue;
    this.maxValue = maxValue;
  }

  public ParsedInput parse(String paramName, Map paramMap) {
    String[] inputValues = (String[]) paramMap.get(paramName);
    return parse((inputValues != null) ? inputValues[0] : null);
  }

  public ParsedInput parse(String inputValue) {
    if (inputValue == null) {
      return new ParsedInput(inputValue,
          InputErrors.of(ParseError.NO_VALUE, "No value provided"));
    }

    Float result = null;
    try {
      result = new Float(inputValue);
    } catch (NumberFormatException e) {
      return new ParsedInput(inputValue, InputErrors.of(ParseError.INVALID_NUMBER,
          "Value is not a floating point number"));
    }

    if (result < minValue) {
      return new ParsedInput(inputValue, InputErrors.of(ParseError.TOO_SMALL,
          "Value is smaller than minimum " + minValue));
    }
    if (result > maxValue) {
      return new ParsedInput(inputValue, InputErrors.of(ParseError.TOO_LARGE,
          "Value is larger than maximum" + minValue));
    }
    return new ParsedInput(inputValue, result);
  }

  /**
   * The parsed input by a {@link FloatInputParser}.
   */
  public static class ParsedInput extends AbstractParsedInput<Float> {
    private final String inputValue;
    private final InputErrors<ParseError> inputErrors;

    private ParsedInput(String inputValue, InputErrors<ParseError> inputErrors) {
      super(null);
      this.inputValue = inputValue;
      this.inputErrors = inputErrors;
    }

    private ParsedInput(String inputValue, Float result) {
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
