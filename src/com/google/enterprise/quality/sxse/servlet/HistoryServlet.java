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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.enterprise.quality.sxse.gxp.History;
import com.google.enterprise.quality.sxse.storage.JudgmentStorage;
import com.google.enterprise.quality.sxse.storage.PreferencesStorage;
import com.google.enterprise.quality.sxse.storage.StorageManager;
import com.google.enterprise.quality.sxse.storage.SxseStorageException;
import com.google.enterprise.quality.sxse.JudgmentDetails;
import com.google.enterprise.quality.sxse.ScoringPolicyProfile;
import com.google.gxp.base.GxpContext;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet through which the user can retrieve his or her history of rated
 * queries.
 */
public final class HistoryServlet extends HttpServlet {
  /**
   * The {@link BannerLink} for this servlet.
   */
  public static final BannerLink BANNER_LINK = new BannerLink() {
    public String getName() {
      return "History";
    }

    public String getUrl() {
      return PATH;
    }
  };

  /**
   * The path for this servlet in the address.
   */
  public static final String PATH = "/history";

  private final Banner banner;
  private final StorageManager storageManager;

  /**
   * Creates a new servlet through which the user can retrieve his history.
   * 
   * @param banner the banner to display across the top of the page
   * @param storageManager the storage from which this servlet can retrieve user
   *        histories
   */
  public HistoryServlet(Banner banner, StorageManager storageManager) {
    this.banner = banner;
    this.storageManager = storageManager;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    Set<String> uniqueQueries = new TreeSet<String>(
        String.CASE_INSENSITIVE_ORDER);

    try {
      String userName = User.getUser(req).getAssessorName();
      JudgmentStorage judgmentStorage = storageManager.getJudgmentStorage();
      List<JudgmentDetails> allJudgments =
          judgmentStorage.getJudgments(userName);
      if (allJudgments == null) {
        // Account for case if user has not yet been created.
        allJudgments = Collections.emptyList();
      }

      PreferencesStorage prefStorage =
        storageManager.getPreferencesStorage();
      // Get all judgments by this user against the current policy profiles.
      ScoringPolicyProfile firstProfile = prefStorage.getFirstProfile();
      ScoringPolicyProfile secondProfile = prefStorage.getSecondProfile();
      Iterable<JudgmentDetails> details = Iterables.filter(allJudgments,
          new ScoringPolicyFilter(firstProfile, secondProfile));
      // Get all queries from the judgments.
      Iterable<String> queries = Iterables.transform(
          details, new Function<JudgmentDetails, String>() {
        public String apply(JudgmentDetails from) {
          return from.getQuery();
        }
      });
      // Remove query duplication.
      Iterables.addAll(uniqueQueries, queries);
    } catch (SxseStorageException e) {
      throw new ServletException(e);
    }
    write(req, res, uniqueQueries);
  }

  /*
   * Display all the user's judgments.
   */
  private void write(HttpServletRequest req, HttpServletResponse res,
      Set<String> queries) throws IOException {
    PreparePage.write(res);
    Writer writer = res.getWriter();

    History.write(writer, new GxpContext(req.getLocale()),
        User.getUser(req), banner, queries);
  }
}
