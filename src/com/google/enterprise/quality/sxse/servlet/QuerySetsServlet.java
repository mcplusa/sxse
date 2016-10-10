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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.enterprise.quality.sxse.gxp.QuerySets;
import com.google.enterprise.quality.sxse.gxp.ViewQuerySet;
import com.google.enterprise.quality.sxse.input.BooleanInputParser;
import com.google.enterprise.quality.sxse.input.EnumInputParser;
import com.google.enterprise.quality.sxse.input.ErrorTransformer;
import com.google.enterprise.quality.sxse.input.InputErrors;
import com.google.enterprise.quality.sxse.input.ListStringInputParser;
import com.google.enterprise.quality.sxse.input.StringInputParser;
import com.google.enterprise.quality.sxse.servlet.QuerySetsServlet.QuerySetsFormContext.Error;
import com.google.enterprise.quality.sxse.storage.QueryStorage;
import com.google.enterprise.quality.sxse.storage.StorageManager;
import com.google.enterprise.quality.sxse.storage.SxseStorageException;
import com.google.gxp.base.GxpContext;
import com.google.gxp.com.google.common.base.Charsets;
import com.google.gxp.com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet through which the administrator can edit query sets.
 */
public final class QuerySetsServlet extends HttpServlet {
  /**
   * The {@link BannerLink} for this servlet.
   */
  public static final BannerLink BANNER_LINK = new BannerLink() {
    public String getName() {
      return "Query Sets";
    }

    public String getUrl() {
      return PATH;
    }
  };
  
  /**
   * The path for this servlet in the address.
   */
  public static final String PATH = "/querysets";

  private static final Logger LOGGER = Logger.getLogger(
      QuerySetsServlet.class.getName());

  private final Banner banner;
  private final StorageManager storageManager;

  /**
   * Creates a new {@code AdminPasswordServlet} that uses the given
   * {@link StorageManager}.
   * 
   * @param banner the banner to display across the top of the page
   * @param storageManager the storage manager
   */
  public QuerySetsServlet(Banner banner, StorageManager storageManager) {
    this.banner = banner;
    this.storageManager = storageManager;
  }

  /**
   * The action to take on a query set.
   */
  public static enum QuerySetAction {
    /**
     * Create the query set.
     */
    CREATE,
    
    /**
     * Rename the query set.
     */
    RENAME,

    /**
     * View the query set.
     */
    VIEW,

    /**
     * Delete the query set.
     */
    DELETE,

    /**
     * Update any query set preferences.
     */
    UPDATE_PREFS,
  }

  public static class CommonKeys {
    /**
     * The action to take.
     */
    public static final String SET_ACTION = "setAction";
    /**
     * The query set to take action on.
     */
    public static final String SET_NAME = "setName";
  }

  public static final class PostKeys extends CommonKeys {
    /**
     * If action is {@link QuerySetAction#CREATE}, the new set name.
     */
    public static final String NEW_SET_NAME = "newSetName";

    /**
     * If action is {@link QuerySetAction#RENAME}, the updated set name.
     */
    public static final String UPDATED_SET_NAME = "updatedSetName";

    /**
     * If action is {@link QuerySetAction#UPDATE_PREFS}, whether we prefer
     * unjudged queries.
     */
    public static final String PREFER_UNJUDGED = "preferUnjudged";

    /**
     * If action is {@link QuerySetAction#UPDATE_PREFS}, whether we show query
     * set names.
     */
    public static final String SHOW_QUERY_SETS = "showQuerySets";

    /**
     * If action is {@link QuerySetAction#UPDATE_PREFS}, the query sets to
     * choose queries from.
     */
    public static final String ACTIVE_QUERY_SETS = "querySetsToMakeActive";
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    EnumInputParser.ParsedInput<QuerySetAction> actionInput =
      new EnumInputParser<QuerySetAction>(QuerySetAction.class).parse(
          CommonKeys.SET_ACTION, req.getParameterMap());
    final QuerySetAction action = actionInput.getResult();

    // Handle action common to both GET and POST.
    handleCommonAction(req, res, req.getParameterMap(), action);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    // The MultiPartFilter of Jetty converts all values in req.getParameterMap
    // to byte arrays, but the parameters retrieved through req.getParameter
    // and req.getParameterValues remain Strings. Instead of parsing the user
    // input from req.getParameterMap and choking on the byte arrays, copy the
    // values to a new Map derived from req.getParameterValues that the input
    // can parse from.
    HashMap paramMap = new HashMap();
    for (Object key : req.getParameterMap().keySet()) {
      paramMap.put(key, req.getParameterValues((String) key));
    }

    EnumInputParser.ParsedInput<QuerySetAction> actionInput =
      new EnumInputParser<QuerySetAction>(QuerySetAction.class).parse(
          CommonKeys.SET_ACTION, paramMap);
    final QuerySetAction action = actionInput.getResult();

    try {
      if (action != null) {
        switch (action) {
        case CREATE:
          createQuerySet(req, res, paramMap);
          return;
        case RENAME:
          renameQuerySet(req, res, paramMap);
          return;
        case DELETE:
          deleteQuerySet(req, res, paramMap);
          return;
        case UPDATE_PREFS:
          updatePreferences(req, res, paramMap);
          return;
        default:
          // It's okay to pass through.
        }
      } else {
        LOGGER.warning("doPost did not find any specified action");
      }
    } catch (SxseStorageException e) {
      throw new ServletException(e);
    }

    // Handle action common to both GET and POST.
    handleCommonAction(req, res, paramMap, action);
  }

  /*
   * Handles action common to both GET and POST. Must not produce side-effects.
   */
  private void handleCommonAction(HttpServletRequest req, HttpServletResponse res,
      Map paramMap, QuerySetAction action) throws ServletException, IOException {
    try {
      if (action == QuerySetAction.VIEW) {
        viewQuerySet(req, res, paramMap);
        return;
      }

      write(req, res, new QuerySetsFormContext());
    } catch (SxseStorageException e) {
      throw new ServletException(e);
    }
  }

  public static class QuerySetsFormContext {
    public static final QuerySetsFormContext EMPTY = new QuerySetsFormContext();

    public static enum Error {
      NO_FILE_UPLOADED,
      NEW_QUERY_SET_NAME_INVALID,
      NEW_QUERY_SET_NAME_EXISTS,
      RENAMED_QUERY_SET_NAME_INVALID,
      QUERY_SET_RENAME_FAILED,
      QUERY_SET_VIEW_FAILED,
      NO_QUERY_SET_ACTIVE,
    }

    public final String setName;
    public final String newSetName;
    public final String updatedSetName;
    public final InputErrors<Error> errors;

    private QuerySetsFormContext() {
      setName = "";
      newSetName = "";
      updatedSetName = "";
      errors = InputErrors.getEmpty();
    }

    private QuerySetsFormContext(InputErrors<Error> errors) {
      this("", errors);
    }

    private QuerySetsFormContext(String newSetName, InputErrors<Error> errors) {
      Preconditions.checkNotNull(newSetName);
      Preconditions.checkNotNull(errors);
      this.setName = "";
      this.newSetName = newSetName;
      this.updatedSetName = "";
      this.errors = errors;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append('{');
      sb.append("setName=").append(setName);
      sb.append(", newSetName=").append(newSetName);
      sb.append(", updatedSetName=").append(updatedSetName);
      sb.append(", errors=").append(errors);
      sb.append('}');
      return sb.toString();
    }
  }

  private static final StringInputParser QUERY_SET_NAME_PARSER =
      StringInputParser.builder()
        .addChars('a', 'z')
        .addChars('A', 'Z')
        .addChars('0', '9')
        .addChar('_')
        .addChar('-')
        .build();
  private static final ErrorTransformer<StringInputParser.ParseError, Error> NEW_SET_NAME_TRANSLATOR =
      ErrorTransformer.builder(StringInputParser.ParseError.class, Error.class)
        .addAll(
          StringInputParser.ParseError.values(), Error.NEW_QUERY_SET_NAME_INVALID,
          "Set name invalid: ")
        .build();
  private static final ErrorTransformer<StringInputParser.ParseError, Error> UPDATED_SET_NAME_TRANSLATOR =
      ErrorTransformer.builder(StringInputParser.ParseError.class, Error.class)
        .addAll(
          StringInputParser.ParseError.values(), Error.RENAMED_QUERY_SET_NAME_INVALID,
          "Set name invalid: ")
        .build();

  private void createQuerySet(HttpServletRequest req, HttpServletResponse res,
      Map paramMap) throws SxseStorageException, IOException {
    @SuppressWarnings("unchecked")
    List<File> files = (List<File>) req.getAttribute(
        "org.mortbay.servlet.MultiPartFilter.files");
    if ((files == null) || (files.size() != 1)) {
      InputErrors<QuerySetsFormContext.Error> inputErrors =
          InputErrors.of(Error.NO_FILE_UPLOADED, "No file was specified");
      write(req, res, new QuerySetsFormContext(inputErrors));
      return;
    }

    // Read all files into memory.
    List<String> queries = Files.readLines(files.get(0), Charsets.UTF_8);
    StringInputParser.ParsedInput newSetNameInput = StringInputParser.allowAll().parse(
        PostKeys.NEW_SET_NAME, paramMap);
    if (!newSetNameInput.hasResult()) {
      InputErrors<Error> inputErrors =
          NEW_SET_NAME_TRANSLATOR.transform(newSetNameInput.getErrors());
      write(req, res, new QuerySetsFormContext(inputErrors));
      return;
    }

    // Store the queries in sorted order, and remove duplicates.
    TreeSet<String> uniqueQueries = new TreeSet<String>();
    uniqueQueries.addAll(queries);
    QueryStorage queryStorage = storageManager.getQueryStorage();
    String newSetName = newSetNameInput.getResult();
    if (!queryStorage.addQuerySet(newSetName, uniqueQueries)) {
      InputErrors<Error> inputErrors = InputErrors.of(
          Error.NEW_QUERY_SET_NAME_EXISTS,
          "Set name " + newSetName + " already exists");
      write(req, res, new QuerySetsFormContext(inputErrors));
      return;
    }

    write(req, res, QuerySetsFormContext.EMPTY);
  }

  private void renameQuerySet(HttpServletRequest req, HttpServletResponse res,
      Map paramMap) throws SxseStorageException, IOException {
    StringInputParser.ParsedInput newSetNameInput = StringInputParser.allowAll().parse(
        PostKeys.UPDATED_SET_NAME, paramMap);
    if (!newSetNameInput.hasResult()) {
      InputErrors<Error> inputErrors =
        UPDATED_SET_NAME_TRANSLATOR.transform(newSetNameInput.getErrors());
      write(req, res, new QuerySetsFormContext(inputErrors));
      return;
    }

    StringInputParser.ParsedInput setNameInput = StringInputParser.allowAll().parse(
        PostKeys.SET_NAME, paramMap);
    String setName = setNameInput.getResult();
    QueryStorage queryStorage = storageManager.getQueryStorage();
    String newSetName = newSetNameInput.getResult();
    if (!queryStorage.renameQuerySet(setName, newSetName)) {
      InputErrors<Error> inputErrors = InputErrors.of(
          Error.QUERY_SET_RENAME_FAILED,
          "Could not rename " + setName + " to " + newSetName);
      write(req, res, new QuerySetsFormContext(inputErrors));
      return;
    }

    write(req, res, QuerySetsFormContext.EMPTY);
  }

  private void viewQuerySet(HttpServletRequest req, HttpServletResponse res,
      Map paramMap) throws SxseStorageException, IOException {
    StringInputParser.ParsedInput setNameInput = StringInputParser.allowAll().parse(
        PostKeys.SET_NAME, paramMap);
    String setName = setNameInput.getResult();
    QueryStorage queryStorage = storageManager.getQueryStorage();
    List<String> queries = queryStorage.getQuerySet(setName);

    if (queries == null) {
      InputErrors<Error> inputErrors = InputErrors.of(
          Error.QUERY_SET_VIEW_FAILED, "Could not get queries for " + setName);
      write(req, res, new QuerySetsFormContext(inputErrors));
      return;
    }

    // Write the queries belonging to this query set.
    writeQueries(req, res, setName, queries);
  }

  private void deleteQuerySet(HttpServletRequest req, HttpServletResponse res,
      Map paramMap) throws SxseStorageException, IOException {
    StringInputParser.ParsedInput setNameInput = StringInputParser.allowAll().parse(
        PostKeys.SET_NAME, paramMap);
    String setName = setNameInput.getResult();
    QueryStorage queryStorage = storageManager.getQueryStorage();
    // If this fails, then it's already been deleted.
    queryStorage.removeQuerySet(setName);

    write(req, res, QuerySetsFormContext.EMPTY);
  }

  private void updatePreferences(
      HttpServletRequest req, HttpServletResponse res, Map paramMap)
      throws SxseStorageException, IOException {
    ListStringInputParser.ParsedInput querySetsInput = ListStringInputParser.INSTANCE.parse(
        PostKeys.ACTIVE_QUERY_SETS, paramMap);

    // Update which query sets are active, and which are not.
    Set<String> activeSets = new HashSet<String>(querySetsInput.getResult());
    QueryStorage queryStorage = storageManager.getQueryStorage();
    Set<String> inactiveSets = new TreeSet<String>(
        queryStorage.getQuerySetNames());
    inactiveSets.removeAll(activeSets);

    BooleanInputParser.ParsedInput preferUnjudgedInput = BooleanInputParser.INSTANCE.parse(
        PostKeys.PREFER_UNJUDGED, paramMap);
    queryStorage.setPreferringUnjudged(preferUnjudgedInput.getResult());
    BooleanInputParser.ParsedInput showQuerySetsInput = BooleanInputParser.INSTANCE.parse(
        PostKeys.SHOW_QUERY_SETS, paramMap);
    queryStorage.setShowingQuerySets(showQuerySetsInput.getResult());

    QuerySetsFormContext fc = null;
    if (activeSets.isEmpty() && !inactiveSets.isEmpty()) {
      InputErrors<Error> inputErrors = InputErrors.of(
          Error.NO_QUERY_SET_ACTIVE,
          "At least one query set must be active");
      write(req, res, new QuerySetsFormContext(inputErrors));
      return;
    }

    for (String activeSetName : activeSets) {
      queryStorage.setActive(activeSetName, true);
    }
    for (String inactiveSetName : inactiveSets) {
      queryStorage.setActive(inactiveSetName, false);
    }
    write(req, res, QuerySetsFormContext.EMPTY);
  }

  private void write(HttpServletRequest req, HttpServletResponse res,
      QuerySetsFormContext fc) throws SxseStorageException, IOException {
    QueryStorage queryStorage = storageManager.getQueryStorage();
    // Create the details of each query set.
    Set<String> querySetNames = queryStorage.getQuerySetNames();
    List<QuerySetDetails> querySetDetails =
        new ArrayList<QuerySetDetails>(querySetNames.size());
    for (String querySetName : querySetNames) {
      querySetDetails.add(new QuerySetDetails(
          querySetName, queryStorage.isActive(querySetName)));
    }

    PreparePage.write(res);
    Writer writer = res.getWriter();

    QuerySets.write(writer, new GxpContext(req.getLocale()),
        User.getUser(req), banner, querySetDetails,
        queryStorage.isPreferringUnjudged(), queryStorage.isShowingQuerySets(),
        fc);
  }

  /*
   * Write the queries belonging to a query set.
   */
  private void writeQueries(HttpServletRequest req, HttpServletResponse res,
      String querySetName, List<String> queries) throws IOException {
    PreparePage.write(res);
    Writer writer = res.getWriter();

    ViewQuerySet.write(writer, new GxpContext(req.getLocale()),
        User.getUser(req), banner, querySetName, queries);
  }
}
