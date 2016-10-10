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

import com.google.enterprise.quality.sxse.HostQueryArgsPair;
import com.google.enterprise.quality.sxse.QueryArguments;
import com.google.enterprise.quality.sxse.QueryFormatter;
import com.google.enterprise.quality.sxse.ScoringPolicyProfile;
import com.google.enterprise.quality.sxse.QueryFormatter.FormatterType;
import com.google.enterprise.quality.sxse.gxp.PolicyProfiles;
import com.google.enterprise.quality.sxse.input.BooleanInputParser;
import com.google.enterprise.quality.sxse.input.EnumInputParser;
import com.google.enterprise.quality.sxse.input.ErrorTransformer;
import com.google.enterprise.quality.sxse.input.FloatInputParser;
import com.google.enterprise.quality.sxse.input.InputErrors;
import com.google.enterprise.quality.sxse.input.IntegerInputParser;
import com.google.enterprise.quality.sxse.input.ScoringPolicyInputParser;
import com.google.enterprise.quality.sxse.input.StringInputParser;
import com.google.enterprise.quality.sxse.servlet.PolicyProfilesServlet.PolicyProfilesFormContext.PolicyProfileFields;
import com.google.enterprise.quality.sxse.storage.JudgmentStorage;
import com.google.enterprise.quality.sxse.storage.PreferencesStorage;
import com.google.enterprise.quality.sxse.storage.StorageManager;
import com.google.enterprise.quality.sxse.storage.SxseStorageException;
import com.google.gxp.base.GxpContext;

import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet through which the administrator can edit policy profiles.
 */
public class PolicyProfilesServlet extends HttpServlet {
  /**
   * The {@link BannerLink} for this servlet.
   */
  public static final BannerLink BANNER_LINK = new BannerLink() {
    public String getName() {
      return "Policy Profiles";
    }

    public String getUrl() {
      return PATH;
    }
  };

  /**
   * The path for this servlet in the address.
   */
  public static final String PATH = "/profiles";

  private static final Logger LOGGER = Logger.getLogger(
      PolicyProfilesServlet.class.getName());

  private final Banner banner;
  private final StorageManager storageManager;

  /**
   * Creates a new {@code AdminPasswordServlet} that uses the given
   * {@link StorageManager}. Note that for this servlet to demonstrate correct
   * behavior, using its {@link PreferencesStorage} as a mutex in a
   * {@code synchronized} block must ensure that only one thread can enter any
   * one of its methods at any time. For a decorator class that provides this
   * functionality, use
   * {@link com.google.enterprise.quality.sxse.storage.SynchronizedStorageManager}.
   * 
   * @param banner the banner to display across the top of the page
   * @param storageManager the storage manager
   * @see com.google.enterprise.quality.sxse.storage.SynchronizedStorageManager
   */
  public PolicyProfilesServlet(Banner banner, StorageManager storageManager) {
    this.banner = banner;
    this.storageManager = storageManager;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException {
    try {
      write(req, res, newDefaultFormContext());
    } catch (SxseStorageException e) {
      throw new ServletException(e);
    }
  }

  private PolicyProfilesFormContext newDefaultFormContext()
      throws SxseStorageException {
    return new PolicyProfilesFormContext(
        storageManager.getPreferencesStorage(),
        storageManager.getJudgmentStorage());
  }

  /**
   * The action to take on the specified scoring policy profile.
   */
  public static enum PolicyProfileAction {
    /**
     * Update the active policy profiles.
     */
    UPDATE_ACTIVE,

    /**
     * Create the policy profile.
     */
    CREATE,

    /**
     * Edit the policy profile.
     */
    EDIT,

    /**
     * Delete the policy profile.
     */
    DELETE,
  }

  public static final class PostKeys {
    /**
     * The action to take. May be {@code null} if a GET request.
     */
    public static final String ACTION = "profileAction";

    /**
     * The first active scoring policy profile.
     */
    public static final String FIRST_PROFILE = "firstProfile";

    /**
     * The second active scoring policy profile.
     */
    public static final String SECOND_PROFILE = "secondProfile";

    /**
     * Whether results from policies A and B should be randomly swapped.
     */
    public static final String SHOULD_SWAP = "shouldSwap";

    /**
     * Whether results should be stored with each judgment.
     */
    public static final String STORE_RESULTS = "storeResults";

    /**
     * Whether judgments should be submitted automatically if results are equal.
     */
    public static final String AUTO_SUBMIT = "autoSubmit";

    /**
     * The maximum amount of time, in seconds, to wait for results from a host.
     */
    public static final String RETRIEVAL_TIMEOUT = "retrievalTimeout";

    /**
     * The maximum number of results a policy should return for judgment.
     */
    public static final String MAX_RESULTS = "maxResults";

    /**
     * The name of the scoring policy profile to take action on. If
     * {@code EDIT}, the profile to edit. If {@code DELETE}, the profile to
     * delete.
     */
    public static final String EDITED_PROFILE_NAME = "updatedProfileName";

    /**
     * The root key for all properties of the new scoring policy profile.
     */
    public static final String NEW_PROFILE = "newProfile";

    /**
     * The root key for all properties of the updated scoring policy profile.
     */
    public static final String UPDATED_PROFILE = "updatedProfile";
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    EnumInputParser.ParsedInput<PolicyProfileAction> actionInput =
        new EnumInputParser<PolicyProfileAction>(PolicyProfileAction.class).parse(
          PostKeys.ACTION, req.getParameterMap());
    PolicyProfileAction action = actionInput.getResult();

    try {
      PolicyProfilesFormContext fc = null;
      if (action != null) {
        switch (action) {
        case UPDATE_ACTIVE:
          fc = editActivePolicyProfiles(req);
          break;
        case CREATE:
          fc = createPolicyProfile(req);
          break;
        case EDIT:
          fc = editPolicyProfile(req);
          break;
        case DELETE:
          fc = deletePolicyProfile(req);
          break;
        default:
          // It's okay to pass through.
        }
      } else {
        LOGGER.warning("doPost did not find any specified action");
      }

      write(req, res, fc);
    } catch (SxseStorageException e) {
      throw new ServletException(e);
    }
  }

  private static final DecimalFormat RETRIEVAL_TIMEOUT_FORMATTER =
      new DecimalFormat("##0.0##");

  private static String getResultRetrievalTimeout(
      JudgmentStorage judgmentStorage) throws SxseStorageException {
    return RETRIEVAL_TIMEOUT_FORMATTER.format(
        judgmentStorage.getResultRetrievalTimeout() / 1000.0);
  }

  /**
   * Structure for holding all data to initialize the Policy Profiles page and
   * forms.
   */
  public static final class PolicyProfilesFormContext {
    public static final class PolicyProfileFields {
      public static final PolicyProfileFields EMPTY =
        new PolicyProfileFields("", FormatterType.URL_PREFIX, "", "", "", "", "");

      public final String profileName;
      public final FormatterType formatterType;

      public final String urlPrefix;
      public final String host;
      public final String frontend;
      public final String collection;
      public final String extraGetParams;

      private PolicyProfileFields(ScoringPolicyInputParser.ParsedInput input) {
        this.profileName = input.getName();
        this.formatterType = FormatterType.valueOf(input.getFormatterType());

        this.urlPrefix = input.getUrlPrefix();
        this.host = input.getHost();
        this.frontend = input.getFrontend();
        this.collection = input.getCollection();
        this.extraGetParams = input.getExtraGetParams();
      }

      private PolicyProfileFields(String profileName, FormatterType formatterType,
          String urlPrefix, String host, String frontend, String collection,
          String extraGetParams) {
        this.profileName = profileName;
        this.formatterType = formatterType;

        this.urlPrefix = urlPrefix;
        this.host = host;
        this.frontend = frontend;
        this.collection = collection;
        this.extraGetParams = extraGetParams;
      }

      @Override
      public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("profileName=").append(profileName);
        sb.append(", formatterType=").append(formatterType);
        sb.append(", urlPrefix=").append(urlPrefix);
        sb.append(", host=").append(host);
        sb.append(", frontend=").append(frontend);
        sb.append(", collection=").append(extraGetParams);
        sb.append(", extraGetParams=").append(extraGetParams).append('}');
        return sb.toString();
      }
    }

    public final String firstActive;
    public final String secondActive;
    public final boolean shouldSwap;
    public final boolean storeResults;
    public final boolean autoSubmit;
    public final String retrievalTimeout;
    public final String maxResults;

    public final PolicyProfileFields newProfile;
    public final String prevProfileName;
    public final PolicyProfileFields updatedProfile;

    public static enum Error {
      FIRST_PROFILE_INVALID,
      SECOND_PROFILE_INVALID,
      NEW_PROFILE_INCOMPLETE,
      NEW_FRONTEND_INVALID,
      NEW_COLLECTION_INVALID,
      NEW_PROFILE_NAME_EXISTS,
      EDITED_PROFILE_INCOMPLETE,
      EDITED_FRONTEND_INVALID,
      EDITED_COLLECTION_INVALID,
      EDITED_PROFILE_NAME_EXISTS,
      DELETE_FAILED,
      RETRIEVAL_TIMEOUT_INVALID,
      MAX_RESULTS_INVALID,
    }

    public final InputErrors<Error> errors;

    // Uses the default values, retrieved from storage.
    public PolicyProfilesFormContext(PreferencesStorage prefsStorage,
        JudgmentStorage judgmentStorage) throws SxseStorageException {
      this(prefsStorage, judgmentStorage, InputErrors.<Error>getEmpty());
    }
    public PolicyProfilesFormContext(PreferencesStorage prefsStorage,
        JudgmentStorage judgmentStorage, InputErrors<Error> errors)
        throws SxseStorageException {
      ScoringPolicyProfile firstProfile = prefsStorage.getFirstProfile();
      ScoringPolicyProfile secondProfile = prefsStorage.getSecondProfile();

      firstActive = (firstProfile != ScoringPolicyProfile.EMPTY_PROFILE) ?
          firstProfile.getName() : null;
      secondActive = (secondProfile != ScoringPolicyProfile.EMPTY_PROFILE) ?
          secondProfile.getName() : null;
      shouldSwap = judgmentStorage.isRandomSwapping();
      storeResults = judgmentStorage.isStoringResults();
      autoSubmit = judgmentStorage.isSubmittingAutomatically();
      retrievalTimeout = getResultRetrievalTimeout(judgmentStorage);
      maxResults = String.valueOf(judgmentStorage.getMaxResults());

      newProfile = PolicyProfileFields.EMPTY;
      prevProfileName = null;
      updatedProfile = PolicyProfileFields.EMPTY;

      this.errors = errors;
    }
    
    // Uses the values submitted from the form.
    public PolicyProfilesFormContext(String firstActive, String secondActive,
        boolean shouldSwap, boolean storeResults, boolean autoSubmit,
        String retrievalTimeout, String maxResults,
        PolicyProfileFields newProfile, String prevProfileName,
        PolicyProfileFields updatedProfile,
        InputErrors<Error> errors) {
      this.firstActive = firstActive;
      this.secondActive = secondActive;
      this.shouldSwap = shouldSwap;
      this.storeResults = storeResults;
      this.autoSubmit = autoSubmit;
      this.retrievalTimeout = retrievalTimeout;
      this.maxResults = maxResults;

      this.newProfile = newProfile;
      this.prevProfileName = prevProfileName;
      this.updatedProfile = updatedProfile;

      this.errors = errors;
    }

    // If true, then the div for editing a profile should be displayed.
    public boolean hasEditError() {
      return (errors.hasError(Error.EDITED_PROFILE_INCOMPLETE)
          || errors.hasError(Error.EDITED_FRONTEND_INVALID)
          || errors.hasError(Error.EDITED_COLLECTION_INVALID)
          || errors.hasError(Error.EDITED_PROFILE_NAME_EXISTS));
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append('{');
      sb.append("firstActive=").append(firstActive);
      sb.append(", secondActive=").append(secondActive);
      sb.append(", shouldSwap=").append(shouldSwap);
      sb.append(", storeResults=").append(storeResults);
      sb.append(", autoSubmit=").append(autoSubmit);
      sb.append(", retrievalTimeout=").append(retrievalTimeout);
      sb.append(", maxResults=").append(maxResults);
      sb.append(", newProfile=").append(newProfile);
      sb.append(", prevProfileName=").append(prevProfileName);
      sb.append(", updatedProfile=").append(updatedProfile);
      sb.append(", errors=").append(errors);
      sb.append('}');
      return sb.toString();
    }
  }

  private static final FloatInputParser RETRIEVAL_TIMEOUT_PARSER =
      new FloatInputParser(0.1f, 120f);
  private static final
      ErrorTransformer<FloatInputParser.ParseError, PolicyProfilesFormContext.Error> RETRIEVAL_TIMEOUT_TRANSFORMER =
        ErrorTransformer.builder(FloatInputParser.ParseError.class, PolicyProfilesFormContext.Error.class)
          .addAll(
            FloatInputParser.ParseError.values(), PolicyProfilesFormContext.Error.RETRIEVAL_TIMEOUT_INVALID,
            "Invalid timeout: ")
          .build();

  private static final IntegerInputParser MAX_RESULTS_PARSER =
      new IntegerInputParser(1, 100);
  private static final
      ErrorTransformer<IntegerInputParser.ParseError, PolicyProfilesFormContext.Error> MAX_RESULTS_TRANSFORMER =
        ErrorTransformer.builder(IntegerInputParser.ParseError.class, PolicyProfilesFormContext.Error.class)
          .addAll(
            IntegerInputParser.ParseError.values(), PolicyProfilesFormContext.Error.MAX_RESULTS_INVALID,
            "Invalid maximum: ")
          .build();

  PolicyProfilesFormContext editActivePolicyProfiles(
      HttpServletRequest req) throws SxseStorageException {
    InputErrors.Builder<PolicyProfilesFormContext.Error> errorsBuilder =
        InputErrors.builder(PolicyProfilesFormContext.Error.class);

    PreferencesStorage prefsStorage = storageManager.getPreferencesStorage();
    StringInputParser.ParsedInput firstProfileInput = StringInputParser.allowAll().parse(
        PostKeys.FIRST_PROFILE, req.getParameterMap());
    String firstProfile = firstProfileInput.getResult();
    StringInputParser.ParsedInput secondProfileInput = StringInputParser.allowAll().parse(
        PostKeys.SECOND_PROFILE, req.getParameterMap());
    String secondProfile = secondProfileInput.getResult();
    if (!prefsStorage.setFirstProfile(firstProfile)) {
      firstProfile = null;
      errorsBuilder.setError(
          PolicyProfilesFormContext.Error.FIRST_PROFILE_INVALID,
          "Invalid first profile");
    }
    if (!prefsStorage.setSecondProfile(secondProfile)) {
      secondProfile = null;
      errorsBuilder.setError(
          PolicyProfilesFormContext.Error.SECOND_PROFILE_INVALID,
          "Invalid second profile");
    }

    boolean shouldSwap = BooleanInputParser.INSTANCE.parse(
        PostKeys.SHOULD_SWAP, req.getParameterMap()).getResult();
    boolean storeResults = BooleanInputParser.INSTANCE.parse(
        PostKeys.STORE_RESULTS, req.getParameterMap()).getResult();
    JudgmentStorage judgmentStorage = storageManager.getJudgmentStorage();
    judgmentStorage.setRandomSwapping(shouldSwap);
    judgmentStorage.setStoringResults(storeResults);

    boolean submitAutomatically;
    String retrievalTimeoutMsec = null;
    if (storeResults) {
      submitAutomatically  = BooleanInputParser.INSTANCE.parse(
          PostKeys.AUTO_SUBMIT, req.getParameterMap()).getResult();
      judgmentStorage.setSubmittingAutomatically(submitAutomatically);

      FloatInputParser.ParsedInput retrievalTimeoutInput = RETRIEVAL_TIMEOUT_PARSER.parse(
          PostKeys.RETRIEVAL_TIMEOUT, req.getParameterMap());
      if (!retrievalTimeoutInput.hasResult()) {
        RETRIEVAL_TIMEOUT_TRANSFORMER.transform(retrievalTimeoutInput.getErrors(), errorsBuilder);
      } else {
        judgmentStorage.setResultRetrievalTimeout(
            (int) (1000 * retrievalTimeoutInput.getResult()));
      }
    } else {
      submitAutomatically = judgmentStorage.isSubmittingAutomatically();
      retrievalTimeoutMsec = getResultRetrievalTimeout(judgmentStorage);
    }

    IntegerInputParser.ParsedInput maxResultsInput = MAX_RESULTS_PARSER.parse(
        PostKeys.MAX_RESULTS, req.getParameterMap());
    int maxResults = 0;
    if (!maxResultsInput.hasResult()) {
      MAX_RESULTS_TRANSFORMER.transform(maxResultsInput.getErrors(), errorsBuilder);
    } else {
      maxResults = maxResultsInput.getResult();
      judgmentStorage.setMaxResults(maxResults);
    }

    InputErrors<PolicyProfilesFormContext.Error> errors = errorsBuilder.build();
    PolicyProfilesFormContext formContext = null;
    if (!errors.isEmpty()) {
      formContext = new PolicyProfilesFormContext(firstProfile, secondProfile,
          shouldSwap, storeResults, submitAutomatically,
          retrievalTimeoutMsec, maxResultsInput.getInputValue(),
          PolicyProfilesFormContext.PolicyProfileFields.EMPTY, null,
          PolicyProfilesFormContext.PolicyProfileFields.EMPTY,
          errors);
    } else {
      formContext = newDefaultFormContext();
    }
    return formContext;
  }

  private static final ErrorTransformer<ScoringPolicyInputParser.ParseError, PolicyProfilesFormContext.Error> NEW_PROFILE_TRANSLATOR =
      ErrorTransformer.builder(ScoringPolicyInputParser.ParseError.class, PolicyProfilesFormContext.Error.class)
        .add(ScoringPolicyInputParser.ParseError.MISSING_NAME, PolicyProfilesFormContext.Error.NEW_PROFILE_INCOMPLETE)
        .add(ScoringPolicyInputParser.ParseError.MISSING_HOST, PolicyProfilesFormContext.Error.NEW_PROFILE_INCOMPLETE)
        .add(ScoringPolicyInputParser.ParseError.MISSING_URL_PREFIX, PolicyProfilesFormContext.Error.NEW_PROFILE_INCOMPLETE)
        .add(ScoringPolicyInputParser.ParseError.INVALID_COLLECTION, PolicyProfilesFormContext.Error.NEW_COLLECTION_INVALID)
        .add(ScoringPolicyInputParser.ParseError.INVALID_FRONTEND, PolicyProfilesFormContext.Error.NEW_FRONTEND_INVALID)
        .build();

  PolicyProfilesFormContext createPolicyProfile(HttpServletRequest req)
      throws SxseStorageException {
    InputErrors.Builder<PolicyProfilesFormContext.Error> errorsBuilder =
        InputErrors.builder(PolicyProfilesFormContext.Error.class);
    ScoringPolicyProfile scoringPolicyProfile = null;

    // If profile name or host is empty, assign an error.
    ScoringPolicyInputParser.ParsedInput profileInput = ScoringPolicyInputParser.INSTANCE.parse(
        PostKeys.NEW_PROFILE, req.getParameterMap());
    if (!profileInput.hasResult()) {
      NEW_PROFILE_TRANSLATOR.transform(profileInput.getErrors(), errorsBuilder);
      return createPolicyProfileFormContext(
          profileInput, errorsBuilder.build());
    } else {
      scoringPolicyProfile = profileInput.getResult();
    }

    PreferencesStorage prefsStorage = storageManager.getPreferencesStorage();
    synchronized (prefsStorage) {
      boolean wasEmpty = prefsStorage.getProfiles().isEmpty();
      if (!prefsStorage.addProfile(scoringPolicyProfile)) {
        errorsBuilder.setError(PolicyProfilesFormContext.Error.NEW_PROFILE_NAME_EXISTS,
            "Profile name " + scoringPolicyProfile.getName() + " exists");
        return createPolicyProfileFormContext(
            profileInput, errorsBuilder.build());
      } else if (wasEmpty) {
        // If this is the first profile added, compare it against no results.
        prefsStorage.setFirstProfile(scoringPolicyProfile.getName());
      }
    }

    PolicyProfilesFormContext fc = newDefaultFormContext();
    return fc;
  }

  private PolicyProfilesFormContext createPolicyProfileFormContext(
      ScoringPolicyInputParser.ParsedInput newPolicyInput,
      InputErrors<PolicyProfilesFormContext.Error> errors) throws SxseStorageException {
    PreferencesStorage prefsStorage = storageManager.getPreferencesStorage();
    JudgmentStorage judgmentStorage = storageManager.getJudgmentStorage();
    ScoringPolicyProfile firstProfile = prefsStorage.getFirstProfile();
    ScoringPolicyProfile secondProfile = prefsStorage.getSecondProfile();

    PolicyProfilesFormContext.PolicyProfileFields newProfileFields =
        new PolicyProfilesFormContext.PolicyProfileFields(newPolicyInput);
    PolicyProfilesFormContext fc = new PolicyProfilesFormContext(
        firstProfile == ScoringPolicyProfile.EMPTY_PROFILE ?
          null : firstProfile.getName(),
        secondProfile == ScoringPolicyProfile.EMPTY_PROFILE ?
          null : secondProfile.getName(),
        judgmentStorage.isRandomSwapping(),
        judgmentStorage.isStoringResults(),
        judgmentStorage.isSubmittingAutomatically(),
        getResultRetrievalTimeout(judgmentStorage),
        String.valueOf(judgmentStorage.getMaxResults()),
        newProfileFields, null,
        PolicyProfilesFormContext.PolicyProfileFields.EMPTY, errors);
    return fc;
  }

  private static final ErrorTransformer<ScoringPolicyInputParser.ParseError, PolicyProfilesFormContext.Error> UPDATED_PROFILE_TRANSLATOR =
      ErrorTransformer.builder(ScoringPolicyInputParser.ParseError.class, PolicyProfilesFormContext.Error.class)
        .add(ScoringPolicyInputParser.ParseError.MISSING_NAME, PolicyProfilesFormContext.Error.EDITED_PROFILE_INCOMPLETE)
        .add(ScoringPolicyInputParser.ParseError.MISSING_HOST, PolicyProfilesFormContext.Error.EDITED_PROFILE_INCOMPLETE)
        .add(ScoringPolicyInputParser.ParseError.MISSING_URL_PREFIX, PolicyProfilesFormContext.Error.EDITED_PROFILE_INCOMPLETE)
        .add(ScoringPolicyInputParser.ParseError.INVALID_COLLECTION, PolicyProfilesFormContext.Error.EDITED_COLLECTION_INVALID)
        .add(ScoringPolicyInputParser.ParseError.INVALID_FRONTEND, PolicyProfilesFormContext.Error.EDITED_FRONTEND_INVALID)
        .build();

  PolicyProfilesFormContext editPolicyProfile(
      HttpServletRequest req) throws SxseStorageException {
    StringInputParser.ParsedInput prevProfileNameInput = StringInputParser.allowAll().parse(
        PostKeys.EDITED_PROFILE_NAME, req.getParameterMap());
    String prevProfileName = prevProfileNameInput.getResult();
    ScoringPolicyInputParser.ParsedInput scoringPolicyInput = ScoringPolicyInputParser.INSTANCE.parse(
        PostKeys.UPDATED_PROFILE, req.getParameterMap());
    if (!scoringPolicyInput.hasResult()) {
      InputErrors<PolicyProfilesFormContext.Error> inputErrors =
          UPDATED_PROFILE_TRANSLATOR.transform(scoringPolicyInput.getErrors());
      return editPolicyProfileFormContext(prevProfileName,
          scoringPolicyInput, inputErrors);
    }

    PreferencesStorage prefsStorage = storageManager.getPreferencesStorage();
    synchronized (prefsStorage) {
      ScoringPolicyProfile prevFirst = prefsStorage.getFirstProfile();
      ScoringPolicyProfile prevSecond = prefsStorage.getSecondProfile();

      ScoringPolicyProfile scoringPolicyProfile = scoringPolicyInput.getResult();
      if (prevProfileName.equals(scoringPolicyProfile.getName())) {
        // The profile name remains unchanged, so must delete first.
        prefsStorage.removeProfile(prevProfileName);
        prefsStorage.addProfile(scoringPolicyProfile);
      } else {
        // The profile name has changed, so try creating the new profile first.
        if (prefsStorage.addProfile(scoringPolicyProfile)) {
          // Now that new profile is created, delete old profile.
          prefsStorage.removeProfile(prevProfileName);
        } else {
          InputErrors<PolicyProfilesFormContext.Error> inputErrors =
              InputErrors.of(
                PolicyProfilesFormContext.Error.EDITED_PROFILE_NAME_EXISTS,
                "Profile name " + scoringPolicyProfile.getName() + " already exists");
          return editPolicyProfileFormContext(
              prevProfileName, scoringPolicyInput, inputErrors);
        }
      }

      // Restore edited profile if it was previously active.
      if ((prevFirst != ScoringPolicyProfile.EMPTY_PROFILE) &&
          prevFirst.getName().equals(prevProfileName)) {
        prefsStorage.setFirstProfile(scoringPolicyProfile.getName());
      }
      if ((prevSecond != ScoringPolicyProfile.EMPTY_PROFILE) &&
          prevSecond.getName().equals(prevProfileName)) {
        prefsStorage.setSecondProfile(scoringPolicyProfile.getName());
      }
    }

    return newDefaultFormContext();
  }

  private PolicyProfilesFormContext editPolicyProfileFormContext(
      String prevProfileName, ScoringPolicyInputParser.ParsedInput updatedPolicyInput,
      InputErrors<PolicyProfilesFormContext.Error> errors) throws SxseStorageException {
    PreferencesStorage prefsStorage = storageManager.getPreferencesStorage();
    JudgmentStorage judgmentStorage = storageManager.getJudgmentStorage();
    ScoringPolicyProfile firstProfile = prefsStorage.getFirstProfile();
    ScoringPolicyProfile secondProfile = prefsStorage.getSecondProfile();

    PolicyProfilesFormContext.PolicyProfileFields updatedProfileFields =
        new PolicyProfilesFormContext.PolicyProfileFields(updatedPolicyInput);
    PolicyProfilesFormContext fc = new PolicyProfilesFormContext(
        firstProfile == ScoringPolicyProfile.EMPTY_PROFILE ?
          null : firstProfile.getName(),
        secondProfile == ScoringPolicyProfile.EMPTY_PROFILE ?
          null : secondProfile.getName(),
        judgmentStorage.isRandomSwapping(),
        judgmentStorage.isStoringResults(),
        judgmentStorage.isSubmittingAutomatically(),
        getResultRetrievalTimeout(judgmentStorage),
        String.valueOf(judgmentStorage.getMaxResults()),
        PolicyProfilesFormContext.PolicyProfileFields.EMPTY, prevProfileName,
        updatedProfileFields, errors);
    return fc;
  }

  PolicyProfilesFormContext deletePolicyProfile(
      HttpServletRequest req) throws SxseStorageException {
    StringInputParser.ParsedInput profileNameInput = StringInputParser.allowAll().parse(
        PostKeys.EDITED_PROFILE_NAME, req.getParameterMap());
    String profileName = profileNameInput.getResult();

    // If the profile is already deleted and this fails, we don't care.
    PreferencesStorage prefsStorage = storageManager.getPreferencesStorage();
    prefsStorage.removeProfile(profileName);

    PolicyProfilesFormContext fc = newDefaultFormContext();
    return fc;
  }

  private void write(HttpServletRequest req, HttpServletResponse res,
      PolicyProfilesFormContext fc) throws SxseStorageException, IOException {
    PreferencesStorage prefsStorage = storageManager.getPreferencesStorage();

    PreparePage.write(res);
    Writer writer = res.getWriter();

    PolicyProfiles.write(
        writer, new GxpContext(req.getLocale()), User.getUser(req), banner,
        prefsStorage.getProfiles(), fc);
  }
}
