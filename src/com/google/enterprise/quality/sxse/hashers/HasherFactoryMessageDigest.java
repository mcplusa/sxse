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

/**
 * Abstract superclass for all {@link HasherFactory} implementations that return
 * a {@link Hasher} based on a {@link MessageDigest}.
 */
abstract class HasherFactoryMessageDigest implements HasherFactory {
  protected static abstract class MessageDigestHasherAdapter implements Hasher {
    private final MessageDigest md;
    private final int digestLength;

    public MessageDigestHasherAdapter(MessageDigest md) {
      this.md = md;
      digestLength = md.getDigestLength();
    }

    public byte[] finish() {
      return md.digest();
    }

    public void reset() {
      md.reset();
    }

    public void update(byte[] input) {
      md.update(input);
    }

    public int getHashSize() {
      return digestLength;
    }
  }
}
