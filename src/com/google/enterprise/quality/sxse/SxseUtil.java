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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Utility methods for SxSE.
 */
public class SxseUtil {
  private static final String URL_ENCODING = "UTF-8";
  
  public static String urlEncode(String str) {
    try {
      return URLEncoder.encode(str, URL_ENCODING);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("UTF-8 must be supported", e);
    }
  }
  
  public static String urlDecode(String str) {
    try {
      return URLDecoder.decode(str, URL_ENCODING);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("UTF-8 must be supported", e);
    }
  }

  private SxseUtil() {
    // Do not allow instantiation.
  }
}
