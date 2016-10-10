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

import com.google.enterprise.quality.sxse.gxp.Password;
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
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet through which the administrator can change the password.
 */
public class PasswordServlet extends HttpServlet {
  /**
   * The {@link BannerLink} for this servlet.
   */
  public static final BannerLink BANNER_LINK = new BannerLink() {
    public String getName() {
      return "Password";
    }

    public String getUrl() {
      return PATH;
    }
  };

  /**
   * The pathname to this servlet.
   */
  public static final String PATH = "/password";

  private static final Logger LOGGER = Logger.getLogger(
      PasswordServlet.class.getName());

  private final Banner banner;
  private final StorageManager storageManager;

  /**
   * Creates a new {@code AdminPasswordServlet} that uses the given
   * {@link StorageManager}.
   * 
   * @param banner the banner to display across the top of the page
   * @param storageManager the storage manager
   */
  public PasswordServlet(Banner banner, StorageManager storageManager) {
    this.banner = banner;
    this.storageManager = storageManager;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res)
      throws IOException {
    write(req, res, new PasswordFormContext(
        "", InputErrors.<PasswordFormContext.Error>getEmpty()));
  }

  public static final class PostKeys {
    /**
     * The new administrator password.
     */
    public static final String NEW_PASSWORD = "newPassword";

    /**
     * The new administrator password, confirmed.
     */
    public static final String NEW_PASSWORD_CONFIRM = "newPasswordConfirm";

    /**
     * A hint for the new administrator password.
     */
    public static final String PASSWORD_HINT = "passwordHint";
  }

  public static final class PasswordFormContext {
    public final String passwordHint;
    public final InputErrors<Error> errors;

    public static enum Error {
      INVALID_PASSWORD,
      PASSWORD_MISMATCH,
    }

    public PasswordFormContext(PreferencesStorage prefsStorage)
        throws SxseStorageException {
      passwordHint = prefsStorage.getPasswordHint();
      errors = InputErrors.getEmpty();
    }

    public PasswordFormContext(String passwordHint, InputErrors<Error> errors) {
      this.passwordHint = passwordHint;
      this.errors = errors;
    }
  }

  private static final ErrorTransformer<StringInputParser.ParseError, PasswordFormContext.Error> PASSWORD_TRANSFORMER =
      ErrorTransformer.builder(StringInputParser.ParseError.class, PasswordFormContext.Error.class)
        .addAll(
          StringInputParser.ParseError.values(), PasswordFormContext.Error.INVALID_PASSWORD,
          "Invalid password: ")
        .build();

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    try {
      StringInputParser.ParsedInput newPasswordInput = StringInputParser.allowAll().parse(
          PostKeys.NEW_PASSWORD, req.getParameterMap());
      StringInputParser.ParsedInput newPasswordConfirmInput = StringInputParser.allowAll().parse(
          PostKeys.NEW_PASSWORD_CONFIRM, req.getParameterMap());
      StringInputParser.ParsedInput passwordHintInput = StringInputParser.allowAll().parse(
          PostKeys.PASSWORD_HINT, req.getParameterMap());
      String passwordHint = passwordHintInput.hasResult() ?
          passwordHintInput.getResult() : "";

      if (!newPasswordInput.hasResult()) {
        InputErrors<PasswordFormContext.Error> inputErrors =
            PASSWORD_TRANSFORMER.transform(newPasswordInput.getErrors());
        write(req, res, new PasswordFormContext(passwordHint, inputErrors));
        return;
      }

      PasswordFormContext fc = null;
      String newPassword = newPasswordInput.getResult();
      String newPasswordConfirm = newPasswordConfirmInput.getResult();
      PreferencesStorage prefsStorage = storageManager.getPreferencesStorage();

      if (!newPassword.equals(newPasswordConfirm)) {
        InputErrors<PasswordFormContext.Error> inputErrors =
            InputErrors.of(PasswordFormContext.Error.PASSWORD_MISMATCH,
              "Confirmed password does not match");
        write(req, res, new PasswordFormContext(passwordHint, inputErrors));
        return;
      }

      prefsStorage.setNewPassword(newPassword);
      prefsStorage.setPasswordHint(passwordHint);
      write(req, res, new PasswordFormContext(prefsStorage));
    } catch (SxseStorageException e) {
      throw new ServletException(e);
    }
  }

  private void write(HttpServletRequest req, HttpServletResponse res,
      PasswordFormContext fc) throws IOException {
    PreparePage.write(res);
    Writer writer = res.getWriter();

    Password.write(writer, new GxpContext(req.getLocale()),
        User.getUser(req), banner, fc);
  }
}
