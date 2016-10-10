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

import org.joda.time.DateTime;
import org.joda.time.IllegalFieldValueException;

import java.util.Map;

/**
 * An {@link InputParser} for {@link DateTime} values.
 */
public class DateTimeInputParser implements InputParser<DateTime> {
  public static enum ParseError {
    NO_VALUE,
    INVALID_DATE,
  }

  public static DateTimeInputParser allowAll() {
    return new DateTimeInputParser();
  }

  private static final class KeyNames {
    public static final String YEAR = "year";
    public static final String MONTH = "month";
    public static final String DAY = "day";
    public static final String HOUR = "hour";
    public static final String MINUTE = "minute";
    public static final String SECOND = "second";
  }
  
  private static String makeKey(String root, String keyName) {
    return root + "." + keyName;
  }

  public ParsedInput parse(String paramName, Map paramMap) {
    InputErrors.Builder<ParseError> errorsBuilder =
        InputErrors.builder(ParseError.class);

    String yearValue = ((String[]) paramMap.get(makeKey(paramName, KeyNames.YEAR)))[0];
    String monthValue = ((String[]) paramMap.get(makeKey(paramName, KeyNames.MONTH)))[0];
    String dayValue = ((String[]) paramMap.get(makeKey(paramName, KeyNames.DAY)))[0];
    String hourValue = ((String[]) paramMap.get(makeKey(paramName, KeyNames.HOUR)))[0];
    String minuteValue = ((String[]) paramMap.get(makeKey(paramName, KeyNames.MINUTE)))[0];

    IntegerInputParser.ParsedInput yearInput = IntegerInputParser.allowAll().parse(yearValue);
    if (!yearInput.hasResult()) {
      errorsBuilder.setError(ParseError.NO_VALUE, "Year not provided");
    }
    IntegerInputParser.ParsedInput monthInput = IntegerInputParser.allowAll().parse(monthValue);
    if (!monthInput.hasResult()) {
      errorsBuilder.setError(ParseError.NO_VALUE, "Month not provided");
    }
    IntegerInputParser.ParsedInput dayInput = IntegerInputParser.allowAll().parse(dayValue);
    if (!dayInput.hasResult()) {
      errorsBuilder.setError(ParseError.NO_VALUE, "Day not provided");
    }
    IntegerInputParser.ParsedInput hourInput = IntegerInputParser.allowAll().parse(hourValue);
    if (!hourInput.hasResult()) {
      errorsBuilder.setError(ParseError.NO_VALUE, "Hour not provided");
    }
    IntegerInputParser.ParsedInput minuteInput = IntegerInputParser.allowAll().parse(minuteValue);
    if (!minuteInput.hasResult()) {
      errorsBuilder.setError(ParseError.NO_VALUE, "Minute not provided");
    }
    if (errorsBuilder.setAny()) {
      return new ParsedInput(
          yearValue, monthValue, dayValue, hourValue, minuteValue,
          errorsBuilder.build());
    }

    try {
      DateTime result = new DateTime(
          yearInput.getResult(), monthInput.getResult(), dayInput.getResult(),
          hourInput.getResult(), minuteInput.getResult(), 0, 0);
      return new ParsedInput(yearValue, monthValue, dayValue, hourValue, minuteValue,
          result);
    } catch (IllegalFieldValueException e) {
      errorsBuilder.setError(ParseError.INVALID_DATE, "Not a valid date");
      return new ParsedInput(yearValue, monthValue, dayValue, hourValue, minuteValue,
          errorsBuilder.build());
    }
  }

  /**
   * The parsed input by a {@link DateTimeInputParser}.
   */
  public static class ParsedInput extends AbstractParsedInput<DateTime> {
    private final String year;
    private final String month;
    private final String day;
    private final String hour;
    private final String minute;

    private final InputErrors<ParseError> inputErrors;

    private ParsedInput(
        String year, String month, String day, String hour, String minute,
        InputErrors<ParseError> inputErrors) {
      super(null);

      this.year = year;
      this.month = month;
      this.day = day;
      this.hour = hour;
      this.minute = minute;

      this.inputErrors = inputErrors;
    }

    private ParsedInput(
        String year, String month, String day, String hour, String minute,
        DateTime result) {
      super(result);

      this.year = year;
      this.month = month;
      this.day = day;
      this.hour = hour;
      this.minute = minute;

      this.inputErrors = InputErrors.getEmpty();
    }

    /**
     * @return the year field as entered
     */
    public String getYear() {
      return year;
    }

    /**
     * @return the month field as entered
     */
    public String getMonth() {
      return month;
    }

    /**
     * @return the day field as entered
     */
    public String getDay() {
      return day;
    }

    /**
     * @return the hour field as entered
     */
    public String getHour() {
      return hour;
    }

    /**
     * @return the minute field as entered
     */
    public String getMinute() {
      return minute;
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
      sb.append(", year=").append(year);
      sb.append(", month=").append(month);
      sb.append(", day=").append(day);
      sb.append(", hour=").append(hour);
      sb.append(", minute=").append(minute);
      sb.append(", inputErrors=").append(inputErrors);
      sb.append('}');
      return sb.toString();
    }
  }
}
