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

import com.google.gxp.html.HtmlClosure;

/**
 * Bundle containing the required fields to display to the user results
 * side-by-side for judgment.
 */
public interface JudgmentServletDetails {
  /**
   * @return the {@link HtmlClosure} to write the header of the results
   */
  public HtmlClosure writeHeader();

  /**
   * @return the {@link HtmlClosure} to write the footer of the results
   */
  public HtmlClosure writeFooter();
  
  /**
   * @return the {@link HtmlClosure} to write the results side-by-side
   */
  public HtmlClosure writeResults();

  /**
   * @return whether the user should be allowed to judge the results
   */
  public boolean allowJudgment();
}
