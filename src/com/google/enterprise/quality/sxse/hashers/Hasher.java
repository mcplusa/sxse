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

package com.google.enterprise.quality.sxse.hashers;

/**
 * A generic interface for an object that computes a hash value. The interface
 * of this is modeled on {@link java.security.MessageDigest} from the JRE, but
 * we do not want to imply that the underlying hash function is as strong as a
 * message digest.
 */
public interface Hasher {
  /**
   * Resets the internal state, so that it is equal to when it was first
   * created.
   */
  public void reset();

  /**
   * Completes the hash computation by performing final operations such as
   * padding. This method implicitly calls {@link #reset()}.
   * 
   * @return the array of bytes for the resulting hash value
   */
  public byte[] finish();

  /**
   * Updates the hash value using the specified array of bytes.
   * 
   * @param input the array of bytes
   */
  void update(byte[] input);

  /**
   * @return the number of bytes returned in each call to <code>finish</code>
   */
  public int getHashSize();
}
