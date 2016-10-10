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

import java.util.BitSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * An {@link InputParser} for string values.
 */
public class StringInputParser implements InputParser<String> {
  public static enum ParseError {
    NO_VALUE,
    INVALID_CHARACTERS,
    TOO_SHORT,
    TOO_LONG,
  }

  private static final StringInputParser ALLOW_ALL_INSTANCE =
      new StringInputParser(null, null, 1, Integer.MAX_VALUE);

  /**
   * @return a parser that accepts all strings
   */
  public static StringInputParser allowAll() {
    return ALLOW_ALL_INSTANCE;
  }

  private StringInputParser(BitSet legal, Mode mode,
      int minLegnth, int maxLength) {
    this.legal = legal;
    this.mode = mode;
    this.minLength = minLegnth;
    this.maxLength = maxLength;
  }

  private final BitSet legal;
  private final Mode mode;
  private final int minLength;
  private final int maxLength;

  public ParsedInput parse(String paramName, Map paramMap) {
    String[] inputValues = (String[]) paramMap.get(paramName);
    return parse((inputValues != null) ? inputValues[0] : null);
  }

  public ParsedInput parse(String inputValue) {
    if ((inputValue == null) || (inputValue.length() == 0)) {
      return new ParsedInput("",
          InputErrors.of(ParseError.NO_VALUE, "No value provided"));
    }
    if (inputValue.length() < minLength) {
      return new ParsedInput(inputValue, InputErrors.of(
          ParseError.TOO_SHORT, "Value shorter than " + minLength + " characters"));
    }
    if (inputValue.length() > maxLength) {
      return new ParsedInput(inputValue, InputErrors.of(
          ParseError.TOO_LONG, "Value longer than " + maxLength + " characters"));
    }

    if (legal != null) {
      BitSet illegal = new BitSet();
      char[] chars = inputValue.toCharArray();
      boolean exclude = (mode == Mode.EXCLUSIVE);
      for (int i = 0; i < chars.length; ++i) {
        if (legal.get(chars[i]) == exclude) {
          illegal.set(chars[i]);
        }
      }

      if (!illegal.isEmpty()) {
        Set<Character> set = new TreeSet<Character>();
        for (int i = illegal.nextSetBit(0); i >= 0; i = illegal.nextSetBit(i + 1)) {
          set.add(new Character((char) i));
        }
        return new ParsedInput(inputValue, InputErrors.of(
            ParseError.INVALID_CHARACTERS, "Invalid characters: " + set));
      }
    }

    return new ParsedInput(inputValue);
  }

  public static enum Mode {
    INCLUSIVE,
    EXCLUSIVE,
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder for {@link StringInputParser} instances.
   */
  public static class Builder {
    private final BitSet legal;
    private Mode mode;
    private int minLength;
    private int maxLength;

    /**
     * Creates a new builder.
     */
    public Builder() {
      this.legal = new BitSet();
      this.mode = Mode.INCLUSIVE;
      this.minLength  = 1;
      this.maxLength = Integer.MAX_VALUE;
    }

    /**
     * Adds the given character to the set of characters.
     * 
     * @param c the character to add
     * @return this builder
     */
    public Builder addChar(char c) {
      legal.set(c);
      return this;
    }

    /**
     * Removes the given character from the set of characters.
     * 
     * @param c the illegal character
     * @return this builder
     */
    public Builder removeChar(char c) {
      legal.clear(c);
      return this;
    }

    /**
     * Adds the characters in the given range, inclusive on both ends.
     * 
     * @param fromChar the first character to add
     * @param toChar the last character to add
     * @return this builder
     */
    public Builder addChars(char fromChar, char toChar) {
      legal.set(fromChar, toChar + 1);  // toIndex is exclusive
      return this;
    }

    /**
     * Removes the characters in the given range, inclusive on both ends.
     * 
     * @param fromChar the first character to add
     * @param toChar the last character to add
     * @return this builder
     */
    public Builder removeChars(char fromChar, char toChar) {
      legal.clear(fromChar, toChar + 1);    // toIndex is exclusive
      return this;
    }

    /**
     * Sets the mode of this filter. This is {@link Mode#INCLUSIVE} by default,
     * where a value is valid if all its characters are in the set of characters
     * added to this builder. If set to {@link Mode#EXCLUSIVE}, a value is valid
     * if none of its characters are in the set of characters added to this
     * builder.
     * 
     * @param mode the new mode
     * @return this builder
     */
    public Builder setMode(Mode mode) {
      this.mode = mode;
      return this;
    }

    /**
     * Sets the minimum length of the parsed string.
     * 
     * @param minLength the minimum string length allowed
     * @return this builder
     */
    public Builder setMinLength(int minLength) {
      this.minLength = minLength;
      return this;
    }

    /**
     * Sets the maximum length of the parsed string.
     * 
     * @param maxLength the maximum string length allowed
     * @return this builder
     */
    public Builder setMaxLength(int maxLength) {
      this.maxLength = maxLength;
      return this;
    }

    /**
     * Builds the {@link StringInputParser} instance.
     * 
     * @return the new parser
     */
    public StringInputParser build() {
      return new StringInputParser(
          (BitSet) legal.clone(), mode, minLength, maxLength);
    }
  }

  /**
   * The parsed input by a {@link StringInputParser}.
   */
  public static class ParsedInput extends AbstractParsedInput<String> {
    private final String inputValue;
    private final InputErrors<ParseError> inputErrors;

    private ParsedInput(String inputValue, InputErrors<ParseError> inputErrors) {
      super(null);
      this.inputValue = inputValue;
      this.inputErrors = inputErrors;
    }

    private ParsedInput(String result) {
      super(result);
      this.inputValue = result;
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
      appendToString(sb);
      sb.append(", inputValue=").append(inputValue);
      sb.append('}');
      return sb.toString();
    }
  }
}
