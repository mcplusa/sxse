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

import com.google.common.collect.ImmutableList;
import com.google.enterprise.quality.sxse.gxp.AdminBanner;
import com.google.enterprise.quality.sxse.gxp.AssessorBanner;
import com.google.gxp.html.HtmlClosure;

import java.util.List;

/**
 * The banner to be displayed at the top of a page.
 */
public abstract class Banner {
  /**
   * Returns a {@link HtmlClosure} instance that writes the banner.
   * 
   * @param user the user
   * @param selectedLink the selected banner link
   */
  public abstract HtmlClosure write(User user, BannerLink selectedLink);

  // Private constructor, only use static variables.
  private Banner() {
  }

  // All banner links to display to an assessor.
  private static final List<BannerLink> ASSESSOR_LINKS = ImmutableList.of(
      JudgmentServlet.BANNER_LINK,
      HistoryServlet.BANNER_LINK
  );

  // All banner links to display to an administrator.
  private static final List<BannerLink> ADMIN_LINKS = ImmutableList.of(
      AnalyticsServlet.BANNER_LINK,
      PasswordServlet.BANNER_LINK,
      PolicyProfilesServlet.BANNER_LINK,
      QuerySetsServlet.BANNER_LINK,
      UsersServlet.BANNER_LINK
  );

  /**
   * The banner for administrators authenticated using a cookie.
   */
  public static final Banner ADMIN_BANNER = new Banner() {
    public HtmlClosure write(User user, BannerLink selectedLink) {
      return AdminBanner.getGxpClosure(
          user, ADMIN_LINKS, selectedLink);
    }
  };

  /**
   * The banner for assessors authenticated using a cookie. 
   */
  public static final Banner ASSESSOR_BANNER = new Banner() {
    public HtmlClosure write(User user, BannerLink selectedLink) {
      return AssessorBanner.getGxpClosure(
          user, ASSESSOR_LINKS, selectedLink);
    }
  };
}
