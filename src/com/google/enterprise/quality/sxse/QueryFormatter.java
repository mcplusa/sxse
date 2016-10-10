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

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Formats queries to some index of search results.
 */
public abstract class QueryFormatter {
  /**
   * Options used with a {@link QueryFormatter} to complete a query.
   */
  public static final class QueryOptions {
    private final String query;
    private final int numResults;

    /**
     * Creates new search options.
     * 
     * @param query the query of the search
     * @param numResults the number of results to return
     */
    public QueryOptions(String query, int numResults) {
      Preconditions.checkNotNull(numResults > 0);

      this.query = Preconditions.checkNotNull(query);
      this.numResults = numResults;
    }

    /**
     * @return the query
     */
    public String getQuery() {
      return query;
    }

    /**
     * @return the number of results to display
     */
    public int getNumResults() {
      return numResults;
    }
  }

  /**
   * The type of query formatter.
   */
  public static enum FormatterType {
    /**
     * The query formatter used by {@link ScoringPolicyProfile#EMPTY_PROFILE}.
     */
    EMPTY,

    /**
     * The type of query formatter returned by
     * {@link QueryFormatter#createUrlPrefixFormatter(String)}.
     */
    URL_PREFIX,

    /**
     * Type type of query formatter returned by
     * {@link QueryFormatter#createGsaFormatter(HostQueryArgsPair)}.
     */
    GSA,
  }

  /**
   * The empty query formatter, only to be used with
   * {@link ScoringPolicyProfile#EMPTY_PROFILE}.
   */
  public static final QueryFormatter EMPTY_FORMATTER = new QueryFormatter() {
    public FormatterType getFormatterType() {
      return FormatterType.EMPTY;
    }

    public String getUrlPrefix() {
      return null;
    }

    public HostQueryArgsPair getHostQueryArgsPair() {
      return null;
    }

    public URI createQueryUri(QueryOptions queryOptions) {
      return null;
    }

    public List<SearchResult> getSearchResults(QueryOptions queryOptions) {
      return null;
    }

    // Use the default identity equals and hashCode methods.

    @Override
    public String toString() {
      return "Empty formatter";
    }
  };

  /**
   * @return the type of formatter
   */
  public abstract FormatterType getFormatterType();

  /**
   * @return the URL prefix if {@link #getFormatterType()} returns
   *         {@link FormatterType#URL_PREFIX}, {@code null} otherwise
   */
  public abstract String getUrlPrefix();

  /**
   * @return the host and query arguments if {@link #getFormatterType()} returns
   *         {@link FormatterType#GSA}, {@code null} otherwise
   */
  public abstract HostQueryArgsPair getHostQueryArgsPair();

  /**
   * Creates a URI for the given query so that its results can be displayed in
   * an iframe.
   * 
   * @param queryOptions the options to complete the search request 
   * @return the URI containing the query
   */
  public abstract URI createQueryUri(QueryOptions queryOptions);

  /**
   * Returns the search results for the given query so that they can be saved
   * and then displayed to the user, or {@code null} if this operation is not
   * supported.
   * 
   * @param queryOptions the options to complete the search request 
   * @return the list of search results, or {@code null} if this operation is
   *         not supported
   */
  public abstract List<SearchResult> getSearchResults(
      QueryOptions queryOptions);

  /**
   * Creates a query formatter that generates query URLs from a URL prefix.
   * 
   * @param urlPrefix the prefix of the URL
   * @return the URL prefix formatter
   */
  public static QueryFormatter createUrlPrefixFormatter(String urlPrefix) {
    return new UrlPrefixFormatter(Preconditions.checkNotNull(urlPrefix));
  }

  /**
   * Creates a query formatter that generates query URLs from a GSA
   * description.
   * 
   * @param hostQueryArgsPair the host and query arguments of the GSA
   * @return the GSA formatter
   */
  public static QueryFormatter createGsaFormatter(
      HostQueryArgsPair hostQueryArgsPair) {
    return new GsaFormatter(Preconditions.checkNotNull(hostQueryArgsPair));
  }

  private QueryFormatter() {
    // Only allow private inner classes to subclass.
  }

  // The type of QueryFormatter returned by createUrlPrefixFormatter.
  private static final class UrlPrefixFormatter extends QueryFormatter {
    private final String urlPrefix;

    private UrlPrefixFormatter(String urlPrefix) {
      this.urlPrefix = urlPrefix;
    }

    public String getUrlPrefix() {
      return urlPrefix;
    }

    public HostQueryArgsPair getHostQueryArgsPair() {
      return null;
    }

    public FormatterType getFormatterType() {
      return FormatterType.URL_PREFIX;
    }

    public URI createQueryUri(QueryOptions queryOptions) {
      StringBuilder sb = new StringBuilder();
      sb.append(urlPrefix);
      sb.append(SxseUtil.urlEncode(queryOptions.getQuery()));
      try {
        return new URI(sb.toString());
      } catch (URISyntaxException e) {
        // This should never happen.
        throw new RuntimeException(e);
      }
    }

    public List<SearchResult> getSearchResults(QueryOptions queryOptions) {
      // Do not support getting the list of search results directly.
      return null;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      } else if (obj instanceof UrlPrefixFormatter) {
        UrlPrefixFormatter formatter = (UrlPrefixFormatter) obj;
        return (urlPrefix.equals(formatter.urlPrefix));
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(urlPrefix);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder(1024);
      sb.append('{');
      super.appendToStringBuilder(sb);
      sb.append(", urlPrefix=").append(urlPrefix);
      return sb.toString();
    }
  }

  // The type of QueryFormatter returned by createGsaFormatter.
  private static final class GsaFormatter extends QueryFormatter {
    /**
     * The name of the default frontend, used if none is specified.
     */
    static final String ENCODED_DEFAULT_FRONTEND =
        SxseUtil.urlEncode("default_frontend");
    /**
     * The name of the default collection, used if none is specified.
     */
    static final String ENCODED_DEFAULT_COLLECTION =
        SxseUtil.urlEncode("default_collection");

    // XML element and attribute names traversed in the DOM hierarchy.
    private static final String RESULTS = "RES";
    private static final String RESULT = "R";
    private static final String HAS = "HAS";
    private static final String CACHE = "C";
    // XML element and attribute names for values to extract.
    private static final String TITLE = "T";
    private static final String URL = "U";
    private static final String SNIPPET = "S";
    private static final String SIZE = "SZ";
    private static final String INDENT = "L";

    private final HostQueryArgsPair hostQueryArgsPair;

    private GsaFormatter(HostQueryArgsPair hostQueryArgsPair) {
      this.hostQueryArgsPair = hostQueryArgsPair;
    }

    public String getUrlPrefix() {
      return null;
    }

    public HostQueryArgsPair getHostQueryArgsPair() {
      return hostQueryArgsPair;
    }

    public FormatterType getFormatterType() {
      return FormatterType.GSA;
    }

    public URI createQueryUri(QueryOptions queryOptions) {
      try {
        return new URI(createUri(queryOptions, false));
      } catch (URISyntaxException e) {
        throw new RuntimeException("should never happen");
      }
    }

    private String createUri(QueryOptions queryOptions, boolean useXml) {
      StringBuilder sb = new StringBuilder();
      String host = hostQueryArgsPair.getHost();
      if (!host.startsWith("http://")) {
        sb.append("http://");
      }
      sb.append(host).append("/search");
      sb.append("?q=").append(SxseUtil.urlEncode(queryOptions.getQuery()));

      // Append frontend and collection.
      QueryArguments queryArgs = hostQueryArgsPair.getQueryArguments();
      String frontend = queryArgs.getFrontend();
      String collection = queryArgs.getCollection();
      sb.append("&client=").append((frontend.length() > 0) ?
          SxseUtil.urlEncode(frontend) : ENCODED_DEFAULT_FRONTEND);
      sb.append("&site=").append((collection.length() > 0) ?
          SxseUtil.urlEncode(collection) : ENCODED_DEFAULT_COLLECTION);
      // Specify the maximum number of results to return.
      sb.append("&num=").append(queryOptions.getNumResults());
      // Specify XML results if necessary.
      sb.append("&output=xml_no_dtd");
      if (!useXml) {
        sb.append("&proxystylesheet=default_frontend");
      }
      // Add extra GET parameters, only adding '&' if needed.
      String extraParams = queryArgs.getExtraParams();
      if (extraParams.length() > 0) {
        if (extraParams.charAt(0) != '&') {
          sb.append('&');
        }
        sb.append(extraParams);
      }

      return sb.toString();
    }

    public List<SearchResult> getSearchResults(QueryOptions queryOptions) {
      // Constructed string should force returning XML results.
      String getString = createUri(queryOptions, true);
      URI getUrl = null;
      try {
        getUrl = new URI(getString);
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }

      // First, retrieve XML results from the given URL.
      String xmlResults = getXmlResults(getUrl);
      if (xmlResults == null) {
        return null;
      }

      // Convert XML results from string form to a native Document object.
      StringReader sr = new StringReader(xmlResults);
      Document doc = null;
      try {
        doc = new SAXBuilder(false).build(sr);
      } catch (IOException e) {
        throw new RuntimeException(e);
      } catch (JDOMException e) {
        throw new RuntimeException(e);
      }

      // Create a list of SearchResult objects from the Document object.
      return buildResultList(doc);
    }

    private static String getXmlResults(URI url) {
      try {
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(url.toURL().openStream()));
        StringWriter writer = new StringWriter();
        
        CharBuffer buffer = CharBuffer.allocate(16 * 1024);
        while (true) {
          int bytesRead = reader.read(buffer);
          if (bytesRead == -1) {
            break;
          }
          buffer.flip();
          writer.append(buffer, 0, bytesRead);
        }

        return writer.toString();
      } catch (IOException e) {
        // Could not retrieve all results, so will return null.
        throw new RuntimeException(e);
      }
    }

    /*
     * Convert all result elements in the given document into a list of
     * <code>SearchResult</code> instances.
     */
    public static List<SearchResult> buildResultList(Document doc) {
      Element rootElement = doc.getRootElement().getChild(RESULTS);
      if (rootElement == null) {
        // If no RES tag, no results.
        return Collections.emptyList();
      }

      List<?> resElements = rootElement.getChildren(RESULT);
      if (resElements.isEmpty()) {
        // No results.
        return Collections.emptyList();
      }

      // Build results elements.
      List<SearchResult> results =
          new ArrayList<SearchResult>(resElements.size());
      for (Iterator<?> i = resElements.iterator(); i.hasNext(); ) {
        SearchResult nextResult = buildResult((Element) i.next());
        if (nextResult != null) {
          results.add(nextResult);
        }
      }
      return results;
    }

    /*
     * Convert a single result element into a <code>SearchResult</code>
     * instance.
     */
    private static SearchResult buildResult(Element resElement) {
      // Get title.
      String title = null;
      Element titleChild = resElement.getChild(TITLE);
      if (titleChild != null) {
        title = titleChild.getValue();
      }

      // Get snippet.
      String snippet = null;
      Element snippetChild = resElement.getChild(SNIPPET);
      if (snippetChild != null) {
        snippet = snippetChild.getValue();
      } else {
        snippet = "";
      }

      // Get URL.
      String urlString = resElement.getChild(URL).getValue();
      URI url = null;
      try {
        url = new URI(urlString);
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
      if (title == null) {
        title = urlString;
      }

      // Get size.
      String size = null;
      Element hasChild = resElement.getChild(HAS);
      if (hasChild != null) {
        Element cacheChild = hasChild.getChild(CACHE);
        if (cacheChild != null) {
          size = cacheChild.getAttributeValue(SIZE);
        }
      }
      if (size == null) {
        size = "Size unknown";
      }

      // Get whether crowded or not.
      boolean crowded = false;
      Attribute indentAttr = resElement.getAttribute(INDENT);
      if (indentAttr != null) {
        String indentValue = indentAttr.getValue();
        crowded = (indentValue != null) && (Integer.valueOf(indentValue) > 1);
      }

      return new SearchResult(url, title, snippet, size, crowded);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      } else if (obj instanceof GsaFormatter) {
        GsaFormatter formatter = (GsaFormatter) obj;
        return hostQueryArgsPair.equals(formatter.hostQueryArgsPair);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(hostQueryArgsPair);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder(1024);
      sb.append('{');
      super.appendToStringBuilder(sb);
      sb.append(", hostQueryArgsPair=").append(hostQueryArgsPair.toString());
      return sb.toString();
    }
  }

  protected void appendToStringBuilder(StringBuilder sb) {
    sb.append("formatType=").append(getFormatterType());
  }
}
