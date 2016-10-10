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

package com.google.enterprise.quality.sxse;

import com.google.enterprise.quality.sxse.servlet.AdministratorUserTypeFilter;
import com.google.enterprise.quality.sxse.servlet.AnalyticsServlet;
import com.google.enterprise.quality.sxse.servlet.AssessorUserTypeFilter;
import com.google.enterprise.quality.sxse.servlet.Banner;
import com.google.enterprise.quality.sxse.servlet.HistoryServlet;
import com.google.enterprise.quality.sxse.servlet.JudgmentServlet;
import com.google.enterprise.quality.sxse.servlet.LoginServlet;
import com.google.enterprise.quality.sxse.servlet.LogoutServlet;
import com.google.enterprise.quality.sxse.servlet.PasswordServlet;
import com.google.enterprise.quality.sxse.servlet.PolicyProfilesServlet;
import com.google.enterprise.quality.sxse.servlet.PreferencesQueryChooser;
import com.google.enterprise.quality.sxse.servlet.QueryChooser;
import com.google.enterprise.quality.sxse.servlet.QuerySetsServlet;
import com.google.enterprise.quality.sxse.servlet.UnjudgedStorageManager;
import com.google.enterprise.quality.sxse.servlet.UsersServlet;
import com.google.enterprise.quality.sxse.storage.CachingStorageManager;
import com.google.enterprise.quality.sxse.storage.StorageManager;
import com.google.enterprise.quality.sxse.storage.SxseStorageException;
import com.google.enterprise.quality.sxse.storage.SynchronizedStorageManager;
import com.google.enterprise.quality.sxse.storage.textstorage.TextStorage;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.servlet.MultiPartFilter;

import java.io.File;

import javax.servlet.Filter;

/**
 * Driver program for SxSE.
 */
public class Sxse {
  public static void main(String[] args) throws Exception {
    int port = 8000;
    String storageDir = null;

    for (String arg : args) {
      String[] tokens = arg.split("=", 2);
      System.out.println("key = " + tokens[0]);
      if (tokens[0].equals("--port")) {
        port = Integer.valueOf(tokens[1]).intValue();
      } else if (tokens[0].equals("--storage")) {
        storageDir = tokens[1];
      }
    }

    if (storageDir == null) {
      printUsage();
      return;
    }

    Sxse sxse = new Sxse(port);
    sxse.setup(new File(storageDir));
    sxse.start();
  }

  private static void printUsage() {
    System.err.println("Command line arguments:\n"
        + "--port: port number to listen for HTTP requests on\n"
        + "--storage_dir: directory where data should be saved\n");
  }

  private final Server server;
  private final ContextHandlerCollection contexts;

  private Sxse(int port) {
    server = new Server(port);
    contexts = new ContextHandlerCollection();
    server.setHandler(contexts);
  }

  private void setup(File storageDir) throws SxseStorageException {
    // Create the storage manager that prefers unjudged queries.
    UnjudgedStorageManager unjudgedStorageManager =
        new UnjudgedStorageManager(
          new CachingStorageManager(
            new TextStorage(storageDir)));
    // Apply a synchronized wrapper around this storage manager.
    StorageManager storageManager = new SynchronizedStorageManager(
        unjudgedStorageManager);
    QueryChooser queryChooser = new PreferencesQueryChooser(
        storageManager.getQueryStorage(),
        unjudgedStorageManager.getQueryChooser());

    Context rootContext = new Context(contexts, "/", Context.SESSIONS);
    addServlet(rootContext, LoginServlet.PATH,
        new ServletHolder(new LoginServlet(storageManager)));
    addServlet(rootContext, LogoutServlet.PATH,
        new ServletHolder(new LogoutServlet()));

    // Configure the assessor servlets.
    AssessorUserTypeFilter assessorUserTypeFilter =
        new AssessorUserTypeFilter();
    addServlet(rootContext, HistoryServlet.PATH,
        new ServletHolder(new HistoryServlet(Banner.ASSESSOR_BANNER,
          storageManager)), assessorUserTypeFilter);
    addServlet(rootContext, JudgmentServlet.PATH,
        new ServletHolder(new JudgmentServlet(Banner.ASSESSOR_BANNER,
          storageManager, queryChooser)), assessorUserTypeFilter);

    // Configure the administrator servlets.
    AdministratorUserTypeFilter adminUserTypeFilter =
        new AdministratorUserTypeFilter();
    addServlet(rootContext, AnalyticsServlet.PATH,
        new ServletHolder(new AnalyticsServlet(Banner.ADMIN_BANNER,
          storageManager)), adminUserTypeFilter);
    addServlet(rootContext, PasswordServlet.PATH,
        new ServletHolder(new PasswordServlet(Banner.ADMIN_BANNER,
          storageManager)), adminUserTypeFilter);
    addServlet(rootContext, PolicyProfilesServlet.PATH,
        new ServletHolder(new PolicyProfilesServlet(Banner.ADMIN_BANNER,
          storageManager)), adminUserTypeFilter);
    addServlet(rootContext, UsersServlet.PATH,
        new ServletHolder(new UsersServlet(Banner.ADMIN_BANNER,
          storageManager)), adminUserTypeFilter);
    
    // Configure the query set servlet to accept file uploads.
    rootContext.addServlet(new ServletHolder(
        new QuerySetsServlet(Banner.ADMIN_BANNER, storageManager)),
        QuerySetsServlet.PATH);
    FilterHolder multiPartFilter = new FilterHolder(new MultiPartFilter());
    multiPartFilter.setInitParameter("deleteFiles", "true");
    rootContext.addFilter(multiPartFilter, QuerySetsServlet.PATH,
        Handler.DEFAULT);
    rootContext.addFilter(new FilterHolder(adminUserTypeFilter),
        QuerySetsServlet.PATH, Handler.DEFAULT);

    // Configure static file handling.
    Context staticContext = new Context(contexts, "/static", Context.SESSIONS);
    ServletHolder sh = new ServletHolder(DefaultServlet.class);
    sh.setInitParameter("dirAllowed", String.valueOf(true));
    // TODO: this should come from the JAR file
    sh.setInitParameter("resourceBase", "./static/");
    staticContext.addServlet(sh, "/*");
  }

  private void addServlet(Context rootContext, String path,
      ServletHolder servletHolder, Filter... filters) {
    rootContext.addServlet(servletHolder, path);
    for (Filter filter : filters) {
      FilterHolder filterHolder = new FilterHolder(filter);
      rootContext.addFilter(filterHolder, path, Handler.DEFAULT);
    }
  }

  private void start() throws Exception {
    server.start();
  }
}
