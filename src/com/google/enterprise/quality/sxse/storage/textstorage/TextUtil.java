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

package com.google.enterprise.quality.sxse.storage.textstorage;

import com.google.enterprise.quality.sxse.HostQueryArgsPair;
import com.google.enterprise.quality.sxse.QueryArguments;
import com.google.enterprise.quality.sxse.QueryFormatter;
import com.google.enterprise.quality.sxse.QueryFormatter.FormatterType;
import com.google.enterprise.quality.sxse.hashers.HasherFactory;
import com.google.enterprise.quality.sxse.hashers.HasherFactorySha1;
import com.google.enterprise.quality.sxse.storage.SxseStorageException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Static utility methods for the text implementation of storage.
 */
class TextUtil {
  // Only static utility methods, do not allow instantiation.
  private TextUtil() {
  }

  private static final FileFilter filesOnlyFilter = new FileFilter() {
    public boolean accept(File pathname) {
      return pathname.isFile();
    }
  };

  /**
   * @return a {@link FileFilter} implementation that returns only files.
   */
  public static FileFilter getFilesOnlyFilter() {
    return filesOnlyFilter;
  }

  /**
   * @return a {@link HasherFactory} implementation suitable for hashing search
   *         results
   */
  public static HasherFactory getResultsHasherFactory() {
    // Use 160-bit SHA-1 hash for search results.
    return HasherFactorySha1.INSTANCE;
  }

  /**
   * @return a {@link HasherFactory} implementation suitable for hashing
   *         passwords
   */
  public static HasherFactory getPasswordHasherFactory() {
    // Use 160-bit SHA-1 hash for passwords.
    return HasherFactorySha1.INSTANCE;
  }

  private static final char[] HEX_CHARS = {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
      'a', 'b', 'c', 'd', 'e', 'f'
  };

  /**
   * Converts the given byte array to a hexadecimal string that can be converted
   * back by {@link #hexStringToBytes(String)}.
   * 
   * @param array the array of bytes to convert
   * @return the hexadecimal string
   */
  public static String bytesToHexString(byte[] array) {
    StringBuilder sb = new StringBuilder(array.length * 2);
    for (int i = 0; i < array.length; ++i) {
      sb.append(HEX_CHARS[(array[i] >>> 4) & 0xF]);
      sb.append(HEX_CHARS[array[i] & 0xF]);
    }
    return sb.toString();
  }

  /**
   * Converts the given hexadecimal string to a byte array that can be converted
   * back by {@link #bytesToHexString(byte[])}.
   * 
   * @param str the hexadecimal string to convert
   * @return the array of bytes
   */
  public static byte[] hexStringToBytes(String str) {
    byte[] array = new byte[str.length() / 2];
    for (int i = 0; i < array.length; ++i) {
      hexToNibble(array, i, str.charAt(2 * i));
      array[i] <<= 4;
      hexToNibble(array, i, str.charAt(2 * i + 1));
    }
    return array;
  }

  private static void hexToNibble(byte[] array, int index, char c) {
    if (c >= 'a') {
      array[index] |= 10 + ((c - 'a') & 0xF);
    } else {
      array[index] |= ((c - '0') & 0xF);
    }
  }

  /*
   * The delimiter for key-value pairs in storage.
   */
  private static final char KEY_VALUE_DELIMITER = '=';

  /**
   * Reads the next line from {@code in}, and parses it into a key-value pair.
   * If the read key is equal to {@code expectedKey}, the associated value is
   * returned. Otherwise, a {@link SxseStorageException} is thrown.
   * 
   * @param expectedKey the expected key to read
   * @param in the source to read the key-value pair from
   * @return the associated value
   * @throws SxseStorageException if the read key is incorrect, or an I/O
   *         exception occurs
   */
  public static String readValue(String expectedKey, BufferedReader in)
      throws SxseStorageException {
    String line;
    try {
      line = in.readLine();
    } catch (IOException e) {
      throw new SxseStorageException(e);
    }
    return readValue(expectedKey, line);
  }

  /**
   * Reads the next line from {@code raf}, and parses it into a key-value pair.
   * If the read key is equal to {@code expectedKey}, the associated value is
   * returned. Otherwise, a {@link SxseStorageException} is thrown.
   * 
   * @param expectedKey the expected key to read
   * @param raf the source to read the key-value pair from
   * @return the associated value
   * @throws SxseStorageException if the read key is incorrect, or an I/O
   *         exception occurs
   */
  public static String readValue(String expectedKey, RandomAccessFile raf)
      throws SxseStorageException {
    String line;
    try {
      line = raf.readLine();
    } catch (IOException e) {
      throw new SxseStorageException(e);
    }
    return readValue(expectedKey, line);
  }

  /**
   * Parses {@code line} into a key-value pair. If the read key is equal to
   * {@code expectedKey}, the associated value is returned. Otherwise, a
   * {@link SxseStorageException} is thrown.
   * 
   * @param expectedKey the expected key to read
   * @param line the line to parse
   * @return the associated value
   * @throws SxseStorageException if the read key is incorrect
   */
  public static String readValue(String expectedKey, String line)
      throws SxseStorageException {
    String fullKey = expectedKey + KEY_VALUE_DELIMITER;
    if (!line.startsWith(fullKey)) {
      throw new UnexpectedKeyException(expectedKey, line);
    }
    return line.substring(fullKey.length());
  }

  /**
   * Reads the next line from {@code in}, and parses it into a key-value pair.
   * 
   * @param in the source to read the key-value pair from
   * @return the key-value pair
   * @throws SxseStorageException if an I/O exception occurs
   */
  public static KeyValuePair readValue(BufferedReader in)
      throws SxseStorageException {
    String line;
    try {
      line = in.readLine();
    } catch (IOException e) {
      throw new SxseStorageException(e);
    }
    return makeKeyValuePair(line);
  }

  /**
   * Reads the next line from {@code raf}, and parses it into a key-value pair.
   * 
   * @param raf the source to read the key-value pair from
   * @return the key-value pair
   * @throws SxseStorageException if an I/O exception occurs
   */
  public static KeyValuePair readValue(RandomAccessFile raf)
      throws SxseStorageException {
    String line;
    try {
      line = raf.readLine();
    } catch (IOException e) {
      throw new SxseStorageException(e);
    }
    return makeKeyValuePair(line);
  }

  /**
   * Extracts the key-value pair from {@code line}.
   * 
   * @param line the line containing the key-value pair
   * @return the extracted key-value pair
   * @throws SxseStorageException if the line contains no key-value pair
   */
  public static KeyValuePair makeKeyValuePair(String line)
      throws SxseStorageException {
    if (line == null) {
      throw new SxseStorageException(line);
    }

    int delimiterIndex = line.indexOf(KEY_VALUE_DELIMITER);
    if (delimiterIndex < 0) {
      throw new LineFormatException(line);
    }

    String key = line.substring(0, delimiterIndex);
    String value = line.substring(delimiterIndex + 1, line.length());
    return new KeyValuePair(key, value);
  }

  /**
   * Writes an key, but no value, to {@code out}.
   * 
   * @param key the key to write
   * @param out the sink to write the key-value pair to
   * @throws SxseStorageException if an I/O exception occurs
   */
  public static void writeEmptyValue(String key, Appendable out)
      throws SxseStorageException {
    try {
      out.append(key).append(KEY_VALUE_DELIMITER).append('\n');
    } catch (IOException e) {
      throw new SxseStorageException(e);
    }
  }

  /**
   * Writes a key-value pair to {@code out}.
   * 
   * @param key the key to write
   * @param value the value to write
   * @param out the sink to write the key-value pair to
   * @throws SxseStorageException if an I/O exception occurs
   */
  public static void writeValue(String key, Object value, Appendable out)
      throws SxseStorageException {
    try {
      out.append(key).append(KEY_VALUE_DELIMITER).append(value.toString());
      out.append('\n');
    } catch (IOException e) {
      throw new SxseStorageException(e);
    }
  }

  /**
   * Appends a key-value pair to {@code sb}.
   * 
   * @param key the key to write
   * @param value the value to write
   * @param sb the buffer to append the key-value pair to
   */
  public static void writeValue(String key, Object value, StringBuilder sb) {
    sb.append(key).append(KEY_VALUE_DELIMITER).append(value).append('\n');
  }

  private static final class QueryFormatterKeys {
    private static final String FORMATTER_TYPE = "formatterType";

    private static final class UrlPrefixKeys {
      static final String URL_PREFIX = "urlPrefix";
    }

    private static final class GsaProfileKeys {
      static final String HOST = "host";
      static final String FRONTEND = "frontend";
      static final String COLLECTION = "collection";
      static final String EXTRA_GET_PARAMS = "extraGet";
    }
  }

  /**
   * Appends the given {@link QueryFormatter} to the given string builder.
   * 
   * @param queryFormatter the query formatter
   * @param sb the string builder to append to
   * @throws SxseStorageException 
   */
  public static void writeQueryFormatter(QueryFormatter queryFormatter,
      Appendable sb) throws SxseStorageException {
    FormatterType type = queryFormatter.getFormatterType();
    TextUtil.writeValue(QueryFormatterKeys.FORMATTER_TYPE, type, sb);
    if (type == FormatterType.URL_PREFIX) {
      TextUtil.writeValue(QueryFormatterKeys.UrlPrefixKeys.URL_PREFIX,
          queryFormatter.getUrlPrefix(), sb);
    } else if (type == FormatterType.GSA) {
      HostQueryArgsPair hostQueryArgsPair =
          queryFormatter.getHostQueryArgsPair();

      if (hostQueryArgsPair == HostQueryArgsPair.EMPTY_HOST_QUERY_ARGS) {
        TextUtil.writeValue(QueryFormatterKeys.GsaProfileKeys.HOST, "", sb);
        TextUtil.writeValue(
            QueryFormatterKeys.GsaProfileKeys.COLLECTION, "", sb);
        TextUtil.writeValue(
            QueryFormatterKeys.GsaProfileKeys.FRONTEND, "", sb);
        TextUtil.writeValue(
            QueryFormatterKeys.GsaProfileKeys.EXTRA_GET_PARAMS, "", sb);
      } else {
        TextUtil.writeValue(QueryFormatterKeys.GsaProfileKeys.HOST,
            queryFormatter.getHostQueryArgsPair().getHost(), sb);
        QueryArguments queryArgs =
          queryFormatter.getHostQueryArgsPair().getQueryArguments();
        TextUtil.writeValue(QueryFormatterKeys.GsaProfileKeys.COLLECTION,
            queryArgs.getCollection(), sb);
        TextUtil.writeValue(QueryFormatterKeys.GsaProfileKeys.FRONTEND,
            queryArgs.getFrontend(), sb);
        TextUtil.writeValue(QueryFormatterKeys.GsaProfileKeys.EXTRA_GET_PARAMS,
            queryArgs.getExtraParams(), sb);
      }
    }
  }

  /**
   * Reads the {@link QueryFormatter} from the given file.
   * 
   * @param raf the file to read from
   * @return the {@link QueryFormatter}
   * @throws PrematureEofException if the EOF is reached prematurely
   */
  public static QueryFormatter readQueryFormatter(RandomAccessFile raf)
      throws SxseStorageException {
    FormatterType type = FormatterType.valueOf(
        TextUtil.readValue(QueryFormatterKeys.FORMATTER_TYPE, raf));
    if (type == QueryFormatter.FormatterType.EMPTY) {
      return QueryFormatter.EMPTY_FORMATTER;
    } else if (type == QueryFormatter.FormatterType.URL_PREFIX) {
      String urlPrefix = TextUtil.readValue(
          QueryFormatterKeys.UrlPrefixKeys.URL_PREFIX, raf);
      return QueryFormatter.createUrlPrefixFormatter(urlPrefix);
    } else if (type == QueryFormatter.FormatterType.GSA) {
      String host = TextUtil.readValue(
          QueryFormatterKeys.GsaProfileKeys.HOST, raf);
      String collection = TextUtil.readValue(
          QueryFormatterKeys.GsaProfileKeys.COLLECTION, raf);
      String frontend = TextUtil.readValue(
          QueryFormatterKeys.GsaProfileKeys.FRONTEND, raf);
      String extraParams = TextUtil.readValue(
          QueryFormatterKeys.GsaProfileKeys.EXTRA_GET_PARAMS, raf);
      if ((host == null) || (collection == null) ||
          (frontend == null) || (extraParams == null)) {
        throw new PrematureEofException("readPreferences");
      } else if ((host.length() == 0) && (collection.length() == 0) &&
          (frontend.length() == 0) && (extraParams.length() == 0)) {
        return QueryFormatter.createGsaFormatter(HostQueryArgsPair.EMPTY_HOST_QUERY_ARGS);
      }

      QueryArguments queryArgs = new QueryArguments(collection, frontend,
          extraParams);
      HostQueryArgsPair hostQueryArgsPair = new HostQueryArgsPair(
          host, queryArgs);
      return QueryFormatter.createGsaFormatter(hostQueryArgsPair);
    }

    throw new RuntimeException();
  }
  
  /**
   * Reads the {@link QueryFormatter} from the given reader.
   * 
   * @param in the reader to read from
   * @return the {@link QueryFormatter}
   * @throws PrematureEofException if the EOF is reached prematurely
   */
  public static QueryFormatter readQueryFormatter(BufferedReader in)
      throws SxseStorageException {
    FormatterType type = FormatterType.valueOf(
        TextUtil.readValue(QueryFormatterKeys.FORMATTER_TYPE, in));
    if (type == QueryFormatter.FormatterType.EMPTY) {
      return QueryFormatter.EMPTY_FORMATTER;
    } else if (type == QueryFormatter.FormatterType.URL_PREFIX) {
      String urlPrefix = TextUtil.readValue(
          QueryFormatterKeys.UrlPrefixKeys.URL_PREFIX, in);
      return QueryFormatter.createUrlPrefixFormatter(urlPrefix);
    } else if (type == QueryFormatter.FormatterType.GSA) {
      String host = TextUtil.readValue(
          QueryFormatterKeys.GsaProfileKeys.HOST, in);
      String collection = TextUtil.readValue(
          QueryFormatterKeys.GsaProfileKeys.COLLECTION, in);
      String frontend = TextUtil.readValue(
          QueryFormatterKeys.GsaProfileKeys.FRONTEND, in);
      String extraParams = TextUtil.readValue(
          QueryFormatterKeys.GsaProfileKeys.EXTRA_GET_PARAMS, in);
      if ((host == null) || (collection == null) ||
          (frontend == null) || (extraParams == null)) {
        throw new PrematureEofException("readPreferences");
      } else if ((host.length() == 0) && (collection.length() == 0) &&
          (frontend.length() == 0) && (extraParams.length() == 0)) {
        return QueryFormatter.createGsaFormatter(HostQueryArgsPair.EMPTY_HOST_QUERY_ARGS);
      }

      QueryArguments queryArgs = new QueryArguments(collection, frontend,
          extraParams);
      HostQueryArgsPair hostQueryArgsPair = new HostQueryArgsPair(
          host, queryArgs);
      return QueryFormatter.createGsaFormatter(hostQueryArgsPair);
    }

    throw new RuntimeException();
  }

  /**
   * A key-value pair.
   */
  public static final class KeyValuePair {
    public final String key;
    public final String value;

    public KeyValuePair(String key, String value) {
      this.key = key;
      this.value = value;
    }
  }

  /**
   * Exception thrown when a line with bad formatting is read.
   */
  private static class LineFormatException extends SxseStorageException {
    public LineFormatException(String badLine) {
      super("Line with bad formatting read: " + badLine);
    }
  }

  /**
   * Exception thrown when a line with an unexpected key is read.
   */
  private static class UnexpectedKeyException extends SxseStorageException {
    public UnexpectedKeyException(String expectedKey, String badLine) {
      super("Expected key: " + expectedKey + ", found line: " + badLine);
    }
  }

  /**
   * Exception thrown when the EOF is read prematurely.
   */
  public static class PrematureEofException extends SxseStorageException {
    public PrematureEofException(String methodName) {
      super("Method " + methodName + " read file EOF prematurely");
    }
  }
}
