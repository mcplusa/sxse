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

import com.google.enterprise.quality.sxse.gxp.AccessDenied;
import com.google.enterprise.quality.sxse.servlet.User.UserState;
import com.google.gxp.base.GxpContext;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A {@link Filter} that assures only a user having the proper privileges can
 * see a page.
 */
abstract class UserTypeFilter implements Filter {
  public void init(FilterConfig filterConfig) {
  }

  public void destroy() {
  }

  public final void doFilter(ServletRequest req, ServletResponse res,
      FilterChain filterChain) throws IOException, ServletException {
    HttpServletRequest httpReq = (HttpServletRequest) req;
    User user = User.getUser(httpReq);
    if (hasPermission(user)) {
      // User is of the correct type, proceed.
      filterChain.doFilter(req, res);
    } else {
      writeNoPermission(httpReq, (HttpServletResponse) res);
    }
  }

  /**
   * Returns whether the user has permission to view the page protected by this
   * filter.
   * 
   * @param user the user
   * @return {@code true} if the user has permission, {@code false} otherwise
   */
  protected abstract boolean hasPermission(User user);

  private void writeNoPermission(
      HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException {
    PreparePage.write(res);
    Writer writer = res.getWriter();

    AccessDenied.write(writer, new GxpContext(req.getLocale()),
        LoginServlet.makeRedirect(req));
  }
}
