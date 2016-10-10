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

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletResponse;

/**
 * Prepares the web page for writing using GXP:
 * 
 * <ul>
 * <li>Sets the character set to UTF-8 so that browsers shouldn't guess.</li>
 * <li>If specified, sets the page to non-caching by default.</li>
 * </ul>
 */
final class PreparePage {
  // Do not allow instantiation.
  private PreparePage() {
  }
  
  /**
   * Writes the header of an HTML page. This is a utility version for the most
   * common case. It sets caching to false.
   * 
   * @param res the servlet response to use.
   * @throws IOException on an error writing to the servlet response.
   */
  public static void write(HttpServletResponse res) throws IOException {
    write(res, false);
  }

  /**
   * Writes the header of an HTML page.
   * 
   * @param res servlet response to use
   * @param allowCaching {@code true} if caching is allowed,
   *        {@code false} to disable caching by setting the appropriate
   *        header on {@code res}
   * @throws IOException if an error occurs while writing the servlet response
   */
  public static void write(HttpServletResponse res, boolean allowCaching)
      throws IOException {
    res.setContentType("text/html; charset=UTF-8");

    if (!allowCaching) {
      res.setHeader("Cache-control", "no-cache");
    }

    Writer writer = res.getWriter();
    writer.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" "
        + "\"http://www.w3.org/TR/html4/strict.dtd\">\n");
  }
}
