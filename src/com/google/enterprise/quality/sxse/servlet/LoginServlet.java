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

import com.google.enterprise.quality.sxse.SxseUtil;
import com.google.enterprise.quality.sxse.gxp.Login;
import com.google.enterprise.quality.sxse.input.BooleanInputParser;
import com.google.enterprise.quality.sxse.input.EnumInputParser;
import com.google.enterprise.quality.sxse.input.ErrorTransformer;
import com.google.enterprise.quality.sxse.input.InputErrors;
import com.google.enterprise.quality.sxse.input.StringInputParser;
import com.google.enterprise.quality.sxse.storage.PreferencesStorage;
import com.google.enterprise.quality.sxse.storage.StorageManager;
import com.google.enterprise.quality.sxse.storage.SxseStorageException;
import com.google.gxp.base.GxpContext;

import java.io.IOException;
import java.io.Writer;
import java.util.EnumSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet through which the administrator or an assessor logs into SxSE.
 */
public final class LoginServlet extends HttpServlet {
  /**
   * The path for this servlet in the address.
   */
  public static final String PATH = "/login";

  /**
   * Creates a URL for the login servlet that redirects to
   * {@link HttpServletRequest#getRequestURL()} once the user has authenticated.
   * 
   * @param request the request containing the URL to redirect to
   * @return the redirecting login page URL
   */
  public static final String makeRedirect(HttpServletRequest request) {
    return LogoutServlet.PATH + "?url="
        + SxseUtil.urlEncode(request.getRequestURL().toString());
  }

  /**
   * An enumeration over all possible input errors.
   */
  public static enum Error {
    INVALID_USERNAME,
    INVALID_PASSWORD,
  }

  /**
   * Keys allowed for POST operations.
   */
  public static final class PostKeys {
    /**
     * The type of user logging in.
     */
    public static final String USER_TYPE = "userType";

    /**
     * If an assessor, the username.
     */
    public static final String USERNAME = "username";

    /**
     * If an administrator, the password.
     */
    public static final String PASSWORD = "password";
  }
 
  private final StorageManager storageManager;

  /**
   * Creates a new {@code LoginServlet} that uses the given
   * {@link StorageManager}.
   * 
   * @param storageManager the storage manager
   */
  public LoginServlet(StorageManager storageManager) {
    this.storageManager = storageManager;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException {
    User.setUser(req, User.LOGGED_OUT_USER);

    InputErrors<Error> inputErrors = InputErrors.getEmpty();
    write(req, res, inputErrors);
  }

  private static final StringInputParser USERNAME_PARSER = StringInputParser.builder()
      .addChars('a', 'z')
      .addChars('A', 'Z')
      .addChars('0', '9')
      .addChar('_')
      .build();
  private static final ErrorTransformer<StringInputParser.ParseError, Error> USERNAME_TRANSFORMER =
      ErrorTransformer.builder(StringInputParser.ParseError.class, Error.class)
        .addAll(
          StringInputParser.ParseError.values(), Error.INVALID_USERNAME,
          "Invalid username: ")
        .build();
  private static final ErrorTransformer<StringInputParser.ParseError, Error> PASSWORD_TRANSFORMER =
      ErrorTransformer.builder(StringInputParser.ParseError.class, Error.class)
        .addAll(
          StringInputParser.ParseError.values(), Error.INVALID_PASSWORD,
          "Invalid password: ")
        .build();

  private static enum UserType {
    ADMINISTRATOR,
    ASSESSOR,
  }
  private static final EnumInputParser<UserType> USER_TYPE_PARSER =
      new EnumInputParser<UserType>(UserType.class);

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException {
    String redirectUrl = null;
    EnumInputParser.ParsedInput<UserType> userTypeInput = USER_TYPE_PARSER.parse(
        PostKeys.USER_TYPE, req.getParameterMap());

    if (userTypeInput.getResult() == UserType.ASSESSOR) {
      // Client is an assessor.
      StringInputParser.ParsedInput usernameInput = USERNAME_PARSER.parse(
          PostKeys.USERNAME, req.getParameterMap());
      if (!usernameInput.hasResult()) {
        InputErrors<Error> inputErrors = USERNAME_TRANSFORMER.transform(
            usernameInput.getErrors());
        write(req, res, inputErrors);
        return;
      }

      User.setUser(req,
          User.createAssessorClient(usernameInput.getResult()));
      res.sendRedirect((redirectUrl != null) ?
          redirectUrl : JudgmentServlet.PATH);
      return;
    }

    if (userTypeInput.getResult() == UserType.ADMINISTRATOR) {
      try {
        // Check if administrator password is correct.
        StringInputParser.ParsedInput passwordInput = StringInputParser.allowAll().parse(
            PostKeys.PASSWORD, req.getParameterMap());
        if (!passwordInput.hasResult()) {
          InputErrors<Error> inputErrors = PASSWORD_TRANSFORMER.transform(
              passwordInput.getErrors());
          write(req, res, inputErrors);
          return;
        }

        PreferencesStorage prefsStorage = storageManager.getPreferencesStorage();
        String password = passwordInput.getResult();
        if (prefsStorage.isPasswordCorrect(password)) {
          // Client is an administrator.
          res.sendRedirect((redirectUrl != null) ?
              redirectUrl : AnalyticsServlet.PATH);
          User.setUser(req, User.ADMINISTRATOR_USER);
          return;
        } else {
          InputErrors<Error> inputErrors = InputErrors.of(
              Error.INVALID_PASSWORD, "Password is incorrect");
          write(req, res, inputErrors);
          return;
        }
      } catch (SxseStorageException e) {
        throw new ServletException(e);
      }
    }
  }

  private void write(ServletRequest req, HttpServletResponse res,
      InputErrors<Error> inputErrors) throws IOException, ServletException {
    PreparePage.write(res);
    Writer writer = res.getWriter();

    try {
      PreferencesStorage prefsStorage = storageManager.getPreferencesStorage();
      Login.write(writer, new GxpContext(req.getLocale()),
          prefsStorage.getPasswordHint(), inputErrors);
    } catch (SxseStorageException e) {
      throw new ServletException(e);
    }
  }
}
