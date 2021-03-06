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
 * Bundle containing the required fields to display to the results of an
 * analytics computation.
 */
public interface AnalyticsResult {
  /**
   * @return the title of the analytics computation
   */
  public String getTitle();

  /**
   * @return the {@link HtmlClosure} to write the computation results
   */
  public HtmlClosure writeResults();
}
