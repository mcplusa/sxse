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
import com.google.enterprise.quality.sxse.hashers.Hasher;

import java.net.URI;

/**
 * A single search result.
 */
public final class SearchResult {
  private final URI url;
  private final String title;
  private final String snippet;
  private final String size;
  private final boolean crowded;

  /**
   * Creates a single search result.
   * 
   * @param url the URL of the result
   * @param title the title of the result
   * @param snippet the snippet of the result
   * @param size the size of the result
   * @param crowded {@code true} if the result is crowded, or indented
   *        when displayed; {@code false} otherwise
   */
  public SearchResult(URI url, String title, String snippet, String size,
      boolean crowded) {
    if (url == null) {
      throw new NullPointerException("uri is null");
    } else if (title == null) {
      throw new NullPointerException("title is null");
    } else if (snippet == null) {
      throw new NullPointerException("snippet is null");
    } else if (size == null) {
      throw new NullPointerException("size is null");
    }

    this.url = url;
    this.title = title;
    this.snippet = snippet;
    this.size = size;
    this.crowded = crowded;
  }

  /**
   * @return the URL
   */
  public URI getUrl() {
    return url;
  }

  /**
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * @return the snippet
   */
  public String getSnippet() {
    return snippet;
  }

  /**
   * @return the size
   */
  public String getSize() {
    return size;
  }

  /**
   * @return whether the result is crowded
   */
  public boolean isCrowded() {
    return crowded;
  }

  /**
   * Updates the contents of the hash with the fingerprint of this search
   * result, derived from the URL, title, snippet, size, and whether it's
   * crowded.
   * 
   * @param hasher the {@link Hasher} to update
   */
  public void updateHasher(Hasher hasher) {
    hasher.update(url.toString().getBytes());
    hasher.update(title.getBytes());
    hasher.update(snippet.getBytes());
    hasher.update(size.getBytes());
    hasher.update(String.valueOf(isCrowded()).getBytes());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (obj instanceof SearchResult) {
      final SearchResult sr = (SearchResult) obj;
      return (url.equals(sr.url) && title.equals(sr.title)
          && snippet.equals(sr.snippet) && size.equals(sr.size)
          && (crowded == sr.crowded));
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(url, title, snippet, size, crowded);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(1024);
    sb.append('{');
    sb.append("url=").append(url);
    sb.append(", title=").append(title);
    sb.append(", snippet=").append(snippet);
    sb.append(", size=").append(size);
    sb.append(", crowded=").append(crowded);
    sb.append('}');
    return sb.toString();
  }
}
