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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Information about the user making a servlet request.
 */
public class User {
  private static final String RESULTS_ATTRIBUTE = "sxse.results";
  private static final String USER_ATTRIBUTE = "sxse.user";

  public static SideBySideResults getSideBySideResults(HttpServletRequest req) {
    HttpSession session = req.getSession(false);
    return (session != null) ?
        (SideBySideResults) session.getAttribute(RESULTS_ATTRIBUTE) : null;
  }

  private User(UserState userState, String assessorName) {
    this.userState = userState;
    this.assessorName = assessorName;
  }

  public static User getUser(HttpServletRequest req) {
    return getUser(req, true);
  }

  public static User getUser(HttpServletRequest req, boolean createIfMissing) {
    HttpSession session = req.getSession(true);
    User user = (User) session.getAttribute(USER_ATTRIBUTE);
    if (user != null) {
      return user;
    }

    user = LOGGED_OUT_USER;
    if (createIfMissing) {
      session.setAttribute(USER_ATTRIBUTE, user);
    }
    return user;
  }

  public static void setUser(HttpServletRequest req, User user) {
    HttpSession session = req.getSession(true);
    session.setAttribute(USER_ATTRIBUTE, user);
  }

  private final UserState userState;
  private final String assessorName;

  /**
   * The state of a user.
   */
  public static enum UserState {
    /**
     * The client is logged out.
     */
    LOGGED_OUT,
    
    /**
     * The client is logged in as an administrator.
     */
    ADMINISTRATOR,
    
    /**
     * The client is logged in as an assessor.
     */
    ASSESSOR,
  }

  /**
   * @return the {@link UserState} belonging to this user
   */
  public UserState getUserState() {
    return userState;
  }

  /**
   * @return the name of this assessor, or {@code null} if {@code #getClientState()}
   *         does not return {@link UserState#ASSESSOR}
   */
  public String getAssessorName() {
    return assessorName;
  }

  public static void setSideBySideResults(HttpServletRequest req,
      SideBySideResults results) {
    HttpSession session = req.getSession(false);
    if (session != null) {
      session.setAttribute(RESULTS_ATTRIBUTE, results);
    }
  }
  
  /**
   * A {@link User} instance representing a logged out user.
   */
  public static final User LOGGED_OUT_USER =
      new User(UserState.LOGGED_OUT, null) {
    @Override
    public String toString() {
      return "{type=loggedOut}";
    }
  };

  /**
   * A {@link User} instance representing an administrator.
   */
  public static final User ADMINISTRATOR_USER =
      new User(UserState.ADMINISTRATOR, null) {
    @Override
    public String toString() {
      return "{type=administrator}";
    }
  };

  /**
   * Creates a new {@link User} instance representing an assessor.
   * 
   * @param assessorName the name of the assessor
   * @return the new user
   */
  public static User createAssessorClient(final String assessorName) {
    return new User(UserState.ASSESSOR, assessorName) {
      @Override
      public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{type=assessor,name=");
        sb.append(assessorName);
        sb.append("}");
        return sb.toString();
      }
    };
  }
}
