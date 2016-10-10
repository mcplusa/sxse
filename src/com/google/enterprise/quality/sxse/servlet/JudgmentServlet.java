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

import com.google.enterprise.quality.sxse.storage.JudgmentStorage;
import com.google.enterprise.quality.sxse.storage.StorageManager;
import com.google.enterprise.quality.sxse.storage.SxseStorageException;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet through which an assessor makes judgments on the results from
 * scoring policy profiles.
 */
public class JudgmentServlet extends HttpServlet {
  /**
   * The {@link BannerLink} for this servlet.
   */
  public static final BannerLink BANNER_LINK = new BannerLink() {
    public String getName() {
      return "Evaluation";
    }

    public String getUrl() {
      return PATH;
    }
  };

  /**
   * The path for this servlet in the address.
   */
  public static final String PATH = "/eval";

  private final JudgmentStorage judgmentStorage;

  private final JudgmentServletStrategy serverStrategy;
  private final JudgmentServletStrategy clientStrategy;

  public JudgmentServlet(Banner banner, StorageManager storageManager,
      QueryChooser queryChooser) {
    judgmentStorage = storageManager.getJudgmentStorage();

    serverStrategy = new ServerJudgmentStrategy(
        banner, storageManager, queryChooser);
    clientStrategy = new ClientJudgmentStrategy(
        banner, storageManager, queryChooser);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    getJudgmentStrategy().doGet(req, res);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    getJudgmentStrategy().doPost(req, res);
  }

  private JudgmentServletStrategy getJudgmentStrategy()
      throws ServletException {
    try {
      return judgmentStorage.isStoringResults() ?
          serverStrategy : clientStrategy;
    } catch (SxseStorageException e) {
      throw new ServletException(e);
    }
  }
}
