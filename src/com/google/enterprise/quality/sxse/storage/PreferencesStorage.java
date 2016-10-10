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

package com.google.enterprise.quality.sxse.storage;

import com.google.enterprise.quality.sxse.ScoringPolicyProfile;

import java.util.List;
import java.util.Set;

/**
 * Interface for reading and writing the general preferences of the side-by-side
 * evaluation tool.
 */
public interface PreferencesStorage {
  /**
   * Returns whether the given password is correct.
   * 
   * @param password the password to verify
   * @return {@code true} if the password is correct, {@code false} otherwise
   * @throws SxseStorageException if an error occurs
   */
  public boolean isPasswordCorrect(String password) throws SxseStorageException;

  /**
   * Sets the new password as the given password.
   * 
   * @param password the new password
   * @throws SxseStorageException if an error occurs
   */
  public void setNewPassword(String password) throws SxseStorageException;

  /**
   * Sets the hint for the password, to help recover the password if it is
   * forgotten.
   * 
   * @param passwordHint the hint for the password
   * @throws SxseStorageException if an error occurs
   */
  public void setPasswordHint(String passwordHint) throws SxseStorageException;

  /**
   * @return the hint for the password
   * @throws SxseStorageException if an error occurs
   */
  public String getPasswordHint() throws SxseStorageException;

  /**
   * Returns an immutable set containing the usernames of all administrators.
   * This is only used if users are identified by SSO.
   * 
   * @return a set containing the usernames of all administrators
   * @throws SxseStorageException if an error occurs
   */
  public Set<String> getAdministrators() throws SxseStorageException;

  /**
   * Sets the usernames of all administrators. This is only used if users are
   * identified by SSO.
   * 
   * @param administators a set containing the usernames of all administrators
   * @throws SxseStorageException if an error occurs
   */
  public void setAdministrators(Set<String> administators)
      throws SxseStorageException;

  /**
   * Adds the given scoring policy profile.
   * 
   * @param spp the profile to add
   * @return {@code true} if the profile was added, {@code false} if a profile
   *         having the same name already exists
   * @throws SxseStorageException if an error occurs
   */
  public boolean addProfile(ScoringPolicyProfile spp)
      throws SxseStorageException;

  /**
   * Removes the scoring policy profile having the given name. If the profile
   * having the given name is either the first or second profile used for
   * side-by-side comparison, then that profile is reset to
   * {@link ScoringPolicyProfile#EMPTY_PROFILE}.
   * 
   * @param name the name of the profile to remove
   * @return {@code true} if a profile with the given name was found and
   *         removed, {@code false} otherwise
   * @throws SxseStorageException if an error occurs
   */
  public boolean removeProfile(String name) throws SxseStorageException;

  /**
   * Returns the scoring policy profile having the given name.
   * 
   * @param name the name of the profile to retrieve
   * @return the profile having the given name, or {@code null} if no such
   *         profile exists
   * @throws SxseStorageException if an error occurs
   */
  public ScoringPolicyProfile getProfile(String name)
      throws SxseStorageException;

  /**
   * @return an immutable list of all scoring policy profiles.
   * @throws SxseStorageException if an error occurs
   */
  public List<ScoringPolicyProfile> getProfiles() throws SxseStorageException;

  /**
   * Sets the first scoring policy profile used for comparision. If {@code null},
   * then the profile {@link ScoringPolicyProfile#EMPTY_PROFILE} is to be used
   * for comparison.
   * 
   * @param name the name of the policy to make active
   * @return {@code true} if a profile with the given name was found and set,
   *         {@code false} otherwise
   * @throws SxseStorageException if an error occurs
   */
  public boolean setFirstProfile(String name) throws SxseStorageException;

  /**
   * @return the first scoring policy profile used for comparision, possibly
   *         {@link ScoringPolicyProfile#EMPTY_PROFILE}
   * @throws SxseStorageException if an error occurs
   */
  public ScoringPolicyProfile getFirstProfile() throws SxseStorageException;

  /**
   * Sets the second scoring policy profile used for comparision. If
   * {@code null}, then the profile {@link ScoringPolicyProfile#EMPTY_PROFILE}
   * is to be used for comparison.
   * 
   * @param name the name of the policy to make active
   * @return {@code true} if a profile with the given name was found and set,
   *         {@code false} otherwise
   * @throws SxseStorageException if an error occurs
   */
  public boolean setSecondProfile(String name) throws SxseStorageException;

  /**
   * @return the second scoring policy profile used for comparision, possibly
   *         {@link ScoringPolicyProfile#EMPTY_PROFILE}
   * @throws SxseStorageException if an error occurs
   */
  public ScoringPolicyProfile getSecondProfile() throws SxseStorageException;
}
