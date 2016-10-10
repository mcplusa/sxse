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

package com.google.enterprise.quality.sxse.input;

import com.google.enterprise.quality.sxse.HostQueryArgsPair;
import com.google.enterprise.quality.sxse.QueryArguments;
import com.google.enterprise.quality.sxse.QueryFormatter;
import com.google.enterprise.quality.sxse.ScoringPolicyProfile;
import com.google.enterprise.quality.sxse.QueryFormatter.FormatterType;

import java.util.Map;

/**
 * An {@link InputParser} for {@link ScoringPolicyProfile} instances.
 */
public class ScoringPolicyInputParser implements InputParser<ScoringPolicyProfile> {
  public static enum ParseError {
    MISSING_NAME,
    MISSING_HOST,
    INVALID_FRONTEND,
    INVALID_COLLECTION,
    MISSING_URL_PREFIX,
  }

  public static final ScoringPolicyInputParser INSTANCE =
      new ScoringPolicyInputParser();

  private static final ErrorTransformer<StringInputParser.ParseError, ParseError> hostTranslator =
      ErrorTransformer.builder(StringInputParser.ParseError.class, ParseError.class)
        .addAll(StringInputParser.ParseError.values(), ParseError.MISSING_HOST,
          "Invalid host: ")
        .build();
  private static final ErrorTransformer<FrontendInputParser.ParseError, ParseError> frontendTranslator =
      ErrorTransformer.builder(FrontendInputParser.ParseError.class, ParseError.class)
        .addAll(FrontendInputParser.ParseError.values(), ParseError.INVALID_FRONTEND)
        .build();
  private static final ErrorTransformer<CollectionInputParser.ParseError, ParseError> collectionTranslator =
      ErrorTransformer.builder(CollectionInputParser.ParseError.class, ParseError.class)
        .addAll(CollectionInputParser.ParseError.values(), ParseError.INVALID_COLLECTION)
        .build();

  private ScoringPolicyInputParser() {
  }

  private static final class KeyNames {
    public static final String NAME = "name";
    public static final String FORMATTER_TYPE = "formatterType";
    public static final String URL_PREFIX = "urlPrefix";
    public static final String HOST = "host";
    public static final String FRONTEND = "frontend";
    public static final String COLLECTION = "collection";
    public static final String EXTRA_GET = "extraGet";
  }

  private static String makeKey(String root, String keyName) {
    return root + "." + keyName;
  }

  public ParsedInput parse(String paramName, Map paramMap) {
    InputErrors.Builder<ParseError> errorsBuilder =
        InputErrors.builder(ParseError.class);

    // Parse each parameter.
    StringInputParser.ParsedInput nameInput = StringInputParser.allowAll().parse(
        makeKey(paramName, KeyNames.NAME), paramMap);
    if (!nameInput.hasResult()) {
      errorsBuilder.setError(ParseError.MISSING_NAME, "Name not provided");
    }
    EnumInputParser.ParsedInput<QueryFormatter.FormatterType> queryFormatterInput =
        new EnumInputParser<QueryFormatter.FormatterType>(QueryFormatter.FormatterType.class).parse(
            makeKey(paramName, KeyNames.FORMATTER_TYPE), paramMap);
    QueryFormatter.FormatterType formatterType = queryFormatterInput.getResult();

    if (formatterType == FormatterType.URL_PREFIX) {
      // Extract all URL parameters for the URL prefix query formatter.
      StringInputParser.ParsedInput urlPrefixInput = StringInputParser.allowAll().parse(
          makeKey(paramName, KeyNames.URL_PREFIX), paramMap);

      if (!urlPrefixInput.hasResult()) {
        errorsBuilder.setError(
            ParseError.MISSING_URL_PREFIX, "URL prefix not provided");
      } else if (!errorsBuilder.setAny()) {
        // Name was valid earlier, so construct the valid result.
        String profileName = nameInput.getResult();
        String urlPrefix = urlPrefixInput.getResult();

        QueryFormatter queryFormatter =
            QueryFormatter.createUrlPrefixFormatter(urlPrefix);
        ScoringPolicyProfile profile = new ScoringPolicyProfile(
            profileName, queryFormatter);
        return new ParsedInput(profileName, queryFormatterInput.getInputValue(),
            urlPrefix, "", "", "", "", profile);
      }

      // At least one error, return without constructing the profile.
      return new ParsedInput(nameInput.getInputValue(), queryFormatterInput.getInputValue(),
          urlPrefixInput.getInputValue(), "", "", "", "", errorsBuilder.build());
    } else if (formatterType == formatterType.GSA) {
      // Extract all URL parameters for the GSA query formatter.
      StringInputParser.ParsedInput hostInput =
          StringInputParser.allowAll().parse(makeKey(paramName, KeyNames.HOST), paramMap);
      if (!hostInput.hasResult()) {
        hostTranslator.transform(hostInput.getErrors(), errorsBuilder);
      }
      FrontendInputParser.ParsedInput frontendInput =
          FrontendInputParser.INSTANCE.parse(makeKey(paramName, KeyNames.FRONTEND), paramMap);
      if (!frontendInput.hasResult() &&
          !frontendInput.getErrors().hasError(FrontendInputParser.ParseError.NO_VALUE)) {
        frontendTranslator.transform(frontendInput.getErrors(), errorsBuilder);
      }
      CollectionInputParser.ParsedInput collectionInput =
          CollectionInputParser.INSTANCE.parse(makeKey(paramName, KeyNames.COLLECTION), paramMap);
      if (!collectionInput.hasResult() &&
          !collectionInput.getErrors().hasError(CollectionInputParser.ParseError.NO_VALUE)) {
        collectionTranslator.transform(collectionInput.getErrors(), errorsBuilder);
      }

      StringInputParser.ParsedInput extraGetInput =
          StringInputParser.allowAll().parse(makeKey(paramName, KeyNames.EXTRA_GET), paramMap);

      if (!errorsBuilder.setAny()) {
        // Name was valid earlier, so construct the valid result.
        String profileName = nameInput.getResult();
        String host = hostInput.getResult();
        String frontend = frontendInput.hasResult() ?
            frontendInput.getResult() : "";
        String collection = collectionInput.hasResult() ?
            collectionInput.getResult() : "";
        String extraGet = extraGetInput.hasResult() ?
            extraGetInput.getResult() : "";

        QueryArguments queryArguments = new QueryArguments(collection, frontend, extraGet);
        HostQueryArgsPair hostQueryArgs = new HostQueryArgsPair(host, queryArguments);
        QueryFormatter queryFormatter = QueryFormatter.createGsaFormatter(hostQueryArgs);
        ScoringPolicyProfile profile = new ScoringPolicyProfile(
            profileName, queryFormatter);
        return new ParsedInput(profileName, queryFormatterInput.getInputValue(), "",
            host, frontend, collection, extraGet, profile);
      }

      // At least one error, return without constructing the profile.
      return new ParsedInput(nameInput.getInputValue(), queryFormatterInput.getInputValue(), "",
          hostInput.getInputValue(), frontendInput.getInputValue(),
          collectionInput.getInputValue(), extraGetInput.getInputValue(),
          errorsBuilder.build());
    }

    // Should not reach here, query formatter field should always be present.
    return null;
  }

  /**
   * The parsed input by a {@link ScoringPolicyInputParser}.
   */
  public static class ParsedInput extends AbstractParsedInput<ScoringPolicyProfile> {
    private final String name;
    private final String formatterType;
    private final String urlPrefix;
    private final String host;
    private final String frontend;
    private final String collection;
    private final String extraGetParams;

    private final InputErrors<ParseError> inputErrors;

    private ParsedInput(String name, String formatterType, String urlPrefix,
        String host, String frontend, String collection, String extraGetParams,
        InputErrors<ParseError> inputErrors) {
      super(null);

      this.name = name;
      this.formatterType = formatterType;
      this.urlPrefix = urlPrefix;
      this.host = host;
      this.frontend = frontend;
      this.collection = collection;
      this.extraGetParams = extraGetParams;

      this.inputErrors = inputErrors;
    }

    private ParsedInput(String name, String formatterType, String urlPrefix,
        String host, String frontend, String collection, String extraGetParams,
        ScoringPolicyProfile scoringPolicyProfile) {
      super(scoringPolicyProfile);

      this.name = name;
      this.formatterType = formatterType;
      this.urlPrefix = urlPrefix;
      this.host = host;
      this.frontend = frontend;
      this.collection = collection;
      this.extraGetParams = extraGetParams;

      this.inputErrors = InputErrors.getEmpty();
    }

    /**
     * @return the scoring policy name
     */
    public String getName() {
      return name;
    }

    /**
     * @return the query formatter type
     */
    public String getFormatterType() {
      return formatterType;
    }

    /**
     * @return the URL prefix, if the query formatter type is URL prefix.
     */
    public String getUrlPrefix() {
      return urlPrefix;
    }

    /**
     * @return the GSA hostname, if the query formatter type is GSA
     */
    public String getHost() {
      return host;
    }

    /**
     * @return the GSA frontend, if the query formatter type is GSA
     */
    public String getFrontend() {
      return frontend;
    }

    /**
     * @return the GSA collection, if the query formatter type is GSA
     */
    public String getCollection() {
      return collection;
    }

    /**
     * @return the extra GET parameters, if the query formatter type is GSA
     */
    public String getExtraGetParams() {
      return extraGetParams;
    }

    /**
     * @return the input errors
     */
    public InputErrors<ParseError> getErrors() {
      return inputErrors;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append('{');
      super.appendToString(sb);
      sb.append(", name=").append(name);
      sb.append(", formatterType=").append(formatterType);
      sb.append(", urlPrefix=").append(urlPrefix);
      sb.append(", host=").append(host);
      sb.append(", frontend=").append(frontend);
      sb.append(", collection=").append(collection);
      sb.append(", extraGetParams=").append(extraGetParams);
      sb.append(", inputErrors=").append(inputErrors);
      return sb.toString();
    }
  }
}
