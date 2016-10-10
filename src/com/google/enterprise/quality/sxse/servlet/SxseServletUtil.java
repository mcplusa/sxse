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

package com.google.enterprise.quality.sxse.servlet;

import java.text.DateFormat;
import java.util.Locale;
import java.util.Random;

/**
 * Utility methods and fields for SxSE servlets.
 */
class SxseServletUtil {
  /**
   * A shared random number generator, which is thread-safe.
   */
  public static final Random RNG = new Random();

  /**
   * Returns a {@link DateFormat} instance that displays the date for the given
   * locale. 
   * 
   * @param locale the locale of the date
   * @return a formatter for the date
   */
  public static DateFormat getDateFormat(Locale locale) {
    return DateFormat.getDateInstance(DateFormat.LONG, locale);
  }

  /**
   * Returns a {@link DateFormat} instance that displays the date and time for
   * the given locale.
   * 
   * @param locale the locale of the date and time
   * @return a formatter for the date and time
   */
  public static DateFormat getDateTimeFormat(Locale locale) {
    return DateFormat.getDateTimeInstance(
        DateFormat.MEDIUM, DateFormat.LONG, locale);
  }

  // Do not allow instantiation.
  private SxseServletUtil() {
  }
}
