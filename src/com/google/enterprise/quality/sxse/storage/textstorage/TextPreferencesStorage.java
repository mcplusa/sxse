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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSortedSet;
import com.google.enterprise.quality.sxse.QueryFormatter;
import com.google.enterprise.quality.sxse.ScoringPolicyProfile;
import com.google.enterprise.quality.sxse.hashers.Hasher;
import com.google.enterprise.quality.sxse.hashers.PasswordHasher;
import com.google.enterprise.quality.sxse.storage.PreferencesStorage;
import com.google.enterprise.quality.sxse.storage.SxseStorageConstants;
import com.google.enterprise.quality.sxse.storage.SxseStorageException;
import com.google.enterprise.quality.sxse.storage.textstorage.TextUtil.KeyValuePair;
import com.google.enterprise.quality.sxse.storage.textstorage.TextUtil.PrematureEofException;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Implementation of the {@link PreferencesStorage} interface where all data is
 * saved in text format, with the exception of the password.
 */
class TextPreferencesStorage implements PreferencesStorage {
  private static final Logger LOGGER = Logger.getLogger(
      TextPreferencesStorage.class.getName());
  private static final int HASH_SALT_SIZE = 4;
  private static final String ADMIN_DELIMETER = ",";

  private final PasswordHasher passwordHasher;
  private final File prefsFile;
  private final boolean existed;
  private final RandomAccessFile raf;

  private byte[] passwordHash;
  private String passwordHint;
  private Set<String> administrators;
  private ScoringPolicyProfile firstProfile;
  private ScoringPolicyProfile secondProfile;

  private final List<ScoringPolicyProfile> profiles;

  /**
   * Keys associated with values in the text file saving general preferences.
   */
  private static final class TextPreferencesKeys {
    static final String PASSWORD_SALT = "passwordSalt";
    static final String PASSWORD_HASH = "passwordHash";
    static final String PASSWORD_HINT = "passwordHint";
    static final String ADMINISTRATORS = "administrators";
    static final String FIRST_PROFILE = "firstProfile";
    static final String SECOND_PROFILE = "secondProfile";
    static final String PROFILES_SIZE = "numProfiles";
    static final String NAME = "name";
    static final String TYPE = "type";
  }

  protected TextPreferencesStorage(File prefsFile, Hasher passwordHasher)
      throws SxseStorageException {
    this.passwordHasher = new PasswordHasher(passwordHasher);
    this.prefsFile = prefsFile;
    existed = this.prefsFile.exists();
    try {
      if (!existed) {
        // Make empty preferences file if it does not exist yet.
        this.prefsFile.createNewFile();
      }
      raf = new RandomAccessFile(this.prefsFile, "rw");
    } catch (IOException e) {
      throw new SxseStorageException(e);
    }

    // Create unmodifiable list of profiles backed by the real list.
    profiles = new LinkedList<ScoringPolicyProfile>();

    // Initialize all members.
    resetState();
    // Read in the saved preferences.
    if (existed && (this.prefsFile.length() > 0)) {
      LOGGER.info("Existing preferences file found");
      readPreferences();
    }
  }

  /*
   * Resets the in-memory state of the preferences. Do this to safely recover
   * from an IOException when writing to disk.
   */
  private void resetState() {
    passwordHasher.newSalt(HASH_SALT_SIZE);
    passwordHash = passwordHasher.hash(
        SxseStorageConstants.PreferencesStorageDefaults.PASSWORD);
    passwordHint = SxseStorageConstants.PreferencesStorageDefaults.PASSWORD_HINT;
    administrators = ImmutableSortedSet.of();
    firstProfile = ScoringPolicyProfile.EMPTY_PROFILE;
    secondProfile = ScoringPolicyProfile.EMPTY_PROFILE;
    profiles.clear();
  }

  public boolean addProfile(ScoringPolicyProfile spp)
      throws SxseStorageException {
    // Do not allow adding a profile that already exists.
    if (getProfile(spp.getName()) != null) {
      return false;
    }
    profiles.add(spp);

    writePreferences();
    return true;
  }

  public List<ScoringPolicyProfile> getProfiles() {
    return Collections.unmodifiableList(
        new ArrayList<ScoringPolicyProfile>(profiles));
  }

  public boolean removeProfile(String name) throws SxseStorageException {
    boolean removed = false;
    for (Iterator<ScoringPolicyProfile> i = profiles.iterator(); i.hasNext();) {
      ScoringPolicyProfile spp = i.next();
      if (name.equals(spp.getName())) {
        i.remove();
        removed = true;
        break;
      }
    }
    if (!removed) {
      // Profile having given name does not exist.
      return false;
    }

    // Clear firstProfile or secondProfile if equal to removed profile.
    if ((firstProfile != ScoringPolicyProfile.EMPTY_PROFILE) &&
        name.equals(firstProfile.getName())) {
      firstProfile = ScoringPolicyProfile.EMPTY_PROFILE;
    }
    if ((secondProfile != ScoringPolicyProfile.EMPTY_PROFILE) &&
        name.equals(secondProfile.getName())) {
      secondProfile = ScoringPolicyProfile.EMPTY_PROFILE;
    }

    writePreferences();
    return true;
  }

  public ScoringPolicyProfile getFirstProfile() {
    return firstProfile;
  }

  public ScoringPolicyProfile getSecondProfile() {
    return secondProfile;
  }

  public boolean setFirstProfile(String name) throws SxseStorageException {
    if (name == null) {
      firstProfile = ScoringPolicyProfile.EMPTY_PROFILE;
    } else {
      // Do not allow setting a profile that does not exist.
      final ScoringPolicyProfile foundProfile = getProfile(name);
      if (foundProfile == null) {
        return false;
      }
      firstProfile = foundProfile;
    }

    writePreferences();
    return true;
  }

  public boolean setSecondProfile(String name) throws SxseStorageException {
    if (name == null) {
      secondProfile = ScoringPolicyProfile.EMPTY_PROFILE;
    } else {
      // Do not allow setting a profile that does not exist.
      ScoringPolicyProfile foundProfile = getProfile(name);
      if (foundProfile == null) {
        return false;
      }
      secondProfile = foundProfile;
    }

    writePreferences();
    return true;
  }

  public ScoringPolicyProfile getProfile(String name) {
    for (ScoringPolicyProfile profile : profiles) {
      if (name.equals(profile.getName())) {
        return profile;
      }
    }
    return null;
  }

  public boolean isPasswordCorrect(String passwordGuess) {
    byte[] passwordGuessHash = passwordHasher.hash(passwordGuess);
    return Arrays.equals(passwordHash, passwordGuessHash);
  }

  public void setNewPassword(String newPassword) throws SxseStorageException {
    // Generate new salt, then use on new password.
    passwordHasher.newSalt(HASH_SALT_SIZE);
    passwordHash = passwordHasher.hash(newPassword);

    writePreferences();
  }

  public void setPasswordHint(String passwordHint) throws SxseStorageException {
    this.passwordHint = passwordHint;

    writePreferences();
  }

  public String getPasswordHint() {
    return passwordHint;
  }

  public void setAdministrators(Set<String> administrators)
      throws SxseStorageException {
    this.administrators = ImmutableSortedSet.copyOf(administrators);

    writePreferences();
  }

  public Set<String> getAdministrators() {
    return administrators;
  }

  private void readPreferences() throws SxseStorageException {
    try {
      // Move to beginning of file.
      raf.seek(0L);
    } catch (IOException e) {
      throw new SxseStorageException(e);
    }

    String firstProfileName = null;
    String secondProfileName = null;
    while (true) {
      String line;
      try {
        line = raf.readLine();
      } catch (IOException e) {
        throw new SxseStorageException(e);
      }
      if (line == null) {
        // Reached end of file
        break;
      }

      KeyValuePair kvp = TextUtil.makeKeyValuePair(line);
      if (kvp.key.equals(TextPreferencesKeys.PASSWORD_SALT)) {
        passwordHasher.setSalt(TextUtil.hexStringToBytes(kvp.value));
      } else if (kvp.key.equals(TextPreferencesKeys.PASSWORD_HASH)) {
        passwordHash = TextUtil.hexStringToBytes(kvp.value);
      } else if (kvp.key.equals(TextPreferencesKeys.PASSWORD_HINT)) {
        passwordHint = kvp.value;
      } else if (kvp.key.equals(TextPreferencesKeys.ADMINISTRATORS)) {
        administrators = ImmutableSortedSet.of(
            kvp.value.split(ADMIN_DELIMETER));
      } else if (kvp.key.equals(TextPreferencesKeys.FIRST_PROFILE)) {
        firstProfileName = kvp.value;
      } else if (kvp.key.equals(TextPreferencesKeys.SECOND_PROFILE)) {
        secondProfileName = kvp.value;
      } else if (kvp.key.equals(TextPreferencesKeys.PROFILES_SIZE)) {
        final int numProfiles = Integer.valueOf(kvp.value);
        for (int i = 0; i < numProfiles; ++i) {
          ScoringPolicyProfile profile = readScoringPolicyProfile(raf);
          if (getProfile(profile.getName()) == null) {
            // Do not allow profiles to have duplicate names.
            profiles.add(profile);
          }
        }
      }
    }

    // Set which two profiles are active.
    firstProfile = getProfile(firstProfileName);
    if (firstProfile == null) {
      firstProfile = ScoringPolicyProfile.EMPTY_PROFILE;
    }
    secondProfile = getProfile(secondProfileName);
    if (secondProfile == null) {
      secondProfile = ScoringPolicyProfile.EMPTY_PROFILE;
    }
  }

  private ScoringPolicyProfile readScoringPolicyProfile(RandomAccessFile raf)
      throws SxseStorageException {
    String name = TextUtil.readValue(TextPreferencesKeys.NAME, raf);
    if (name == null) {
      throw new PrematureEofException("readScoringPolicyProfile");
    }

    QueryFormatter queryFormatter = TextUtil.readQueryFormatter(raf);
    return new ScoringPolicyProfile(name, queryFormatter);
  }

  private void writePreferences() throws SxseStorageException {
    try {
      // Clear contents of file, flush this change.
      raf.getChannel().truncate(0L);
      raf.getChannel().force(false);
      // Must seek or new preferences prefaced with null bytes.
      raf.seek(0L);

      StringBuilder sb = new StringBuilder();

      // Append salt, hash, and hint.
      TextUtil.writeValue(TextPreferencesKeys.PASSWORD_SALT,
          TextUtil.bytesToHexString(passwordHasher.getSalt()), sb);
      TextUtil.writeValue(TextPreferencesKeys.PASSWORD_HASH,
          TextUtil.bytesToHexString(passwordHash), sb);
      TextUtil.writeValue(TextPreferencesKeys.PASSWORD_HINT,
          passwordHint, sb);

      // Append concatenated list of all administrators.
      TextUtil.writeValue(TextPreferencesKeys.ADMINISTRATORS,
          Joiner.on(ADMIN_DELIMETER).join(administrators), sb);

      // Append names of active scoring policy profiles.
      if (firstProfile != ScoringPolicyProfile.EMPTY_PROFILE) {
        TextUtil.writeValue(
            TextPreferencesKeys.FIRST_PROFILE, firstProfile.getName(), sb);
      } else {
        TextUtil.writeEmptyValue(TextPreferencesKeys.FIRST_PROFILE, sb);
      }
      if (secondProfile != ScoringPolicyProfile.EMPTY_PROFILE) {
        TextUtil.writeValue(
            TextPreferencesKeys.SECOND_PROFILE, secondProfile.getName(), sb);
      } else {
        TextUtil.writeEmptyValue(TextPreferencesKeys.SECOND_PROFILE, sb);
      }

      // Append the number of profiles.
      TextUtil.writeValue(
          TextPreferencesKeys.PROFILES_SIZE, profiles.size(), sb);

      // Flush buffer to file.
      raf.writeBytes(sb.toString());

      // Write each scoring policy profile.
      for (ScoringPolicyProfile profile : profiles) {
        writeScoringPolicyProfile(profile, raf);
      }
    } catch (IOException e) {
      throw new SxseStorageException(e);
    }
  }

  private void writeScoringPolicyProfile(ScoringPolicyProfile profile,
      RandomAccessFile raf) throws IOException, SxseStorageException {
    StringBuilder sb = new StringBuilder();

    TextUtil.writeValue(
        TextPreferencesKeys.NAME, profile.getName(), sb);
    TextUtil.writeQueryFormatter(profile.getQueryFormatter(), sb);

    raf.writeBytes(sb.toString());
  }

  protected void tryDelete() {
    // Only delete the file if we created it.
    if (!existed) {
      prefsFile.delete();
    }
  }
}
