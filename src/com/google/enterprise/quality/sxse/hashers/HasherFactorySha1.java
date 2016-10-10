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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A factory for {@link Hasher} instances that return 160-bit hashes, based on
 * an underlying {@link MessageDigest} instance computing the SHA-1 hash.
 */
public class HasherFactorySha1 extends HasherFactoryMessageDigest {
  /**
   * The singleton instance of {@code HasherFactorySha1}.
   */
  public static final HasherFactory INSTANCE = new HasherFactorySha1();

  // Do not allow instantiation.
  private HasherFactorySha1() {
  }

  public Hasher getHasher() {
    try {
      return new HasherSha1();
    } catch (NoSuchAlgorithmException e) {
      // We lose, throw a run-time exception.
      throw new RuntimeException(
          "Could not create internal SHA-1 MessageDigest instance", e);
    }
  }

  private static final class HasherSha1 extends MessageDigestHasherAdapter {
    public HasherSha1() throws NoSuchAlgorithmException {
      super(MessageDigest.getInstance("SHA1"));
    }
  }
}
