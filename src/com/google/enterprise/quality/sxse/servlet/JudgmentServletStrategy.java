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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * An interface for strategies that determine how a {@link JudgmentServlet}
 * should display results for judgment.
 */
interface JudgmentServletStrategy {
  /**
   * Invoked by a {@link JudgmentServlet} instance to handle a GET request.
   * 
   * @param req an {@link HttpServletRequest} object that contains the request
   * @param res an {@link HttpServletResponse} object that contains the response
   * @throws ServletException if the GET request could not be handled
   * @throws IOException if an input or output error is detected
   */
  public void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException;

  /**
   * Invoked by a {@link JudgmentServlet} instance to handle a POST request.
   * 
   * @param req an {@link HttpServletRequest} object that contains the request
   * @param res an {@link HttpServletResponse} object that contains the response
   * @throws ServletException if the POST request could not be handled
   * @throws IOException if an input or output error is detected
   */
  public void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException;
}
