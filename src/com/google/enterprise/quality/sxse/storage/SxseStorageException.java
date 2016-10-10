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


/**
 * Signals that an exception while accessing or possibly modifying a
 * {@link StorageManager} or its elements has occured. Typically these will be
 * I/O in nature, although this is not necessary, and so this class inherits
 * from {@link Exception} directly.
 */
public class SxseStorageException extends Exception {
  /**
   * Constructs a new exception with no detailed message and no cause.
   */
  public SxseStorageException() {
    super();
  }

  /**
   * Constructs a new exception with a detailed message and no cause.
   * 
   * @param message the detailed message
   */
  public SxseStorageException(String message) {
    super(message);
  }

  /**
   * Constructs a new exception with no detailed message and a cause.
   * 
   * @param cause the cause
   */
  public SxseStorageException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a new exception with a detailed message and a cause.
   * 
   * @param message the detailed message
   * @param cause the cause
   */
  public SxseStorageException(String message, Throwable cause) {
    super(message, cause);
  }
}
