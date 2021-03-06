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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet logs the user out from SxSE, on either a GET or a POST.
 */
public final class LogoutServlet extends HttpServlet {
  /**
   * The {@link BannerLink} for this servlet.
   */
  public static final BannerLink BANNER_LINK = new BannerLink() {
    public String getName() {
      return "Log Out";
    }

    public String getUrl() {
      return PATH;
    }
  };

  /**
   * The path for this servlet in the address.
   */
  public static final String PATH = "/logout";

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res)
      throws IOException {
    logout(req, res);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res)
      throws IOException {
    logout(req, res);
  }

  private void logout(HttpServletRequest req, HttpServletResponse res)
      throws IOException {
    // The login servlet will log out the user.
    res.sendRedirect(LoginServlet.PATH);
  }
}
