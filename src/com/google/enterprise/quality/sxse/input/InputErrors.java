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

import java.util.EnumMap;
import java.util.Map;

/**
 * A collection of input errors.
 */
public class InputErrors<T extends Enum<T>> {
  private Map<T, String> errorMap;

  private static enum EmptyEnum {
  }
  @SuppressWarnings("unchecked")
  private static final InputErrors errors = new InputErrors(EmptyEnum.class);

  @SuppressWarnings("unchecked")
  public static <T extends Enum<T>> InputErrors<T> getEmpty() {
    return errors;
  }

  private InputErrors() {
    errorMap = null;
  }

  // Only accessible from the builder.
  private InputErrors(Class<T> errorClass) {
    errorMap = new EnumMap<T, String>(errorClass);
  }

  /**
   * Returns whether an error of the given type exists.
   * 
   * @param errorType the type of error
   * @return {@code true} if an error of the given type exists, {@code false}
   *         otherwise
   */
  public boolean hasError(T errorType) {
    return errorMap.containsKey(errorType);
  }

  /**
   * Returns the error message associated with the given type of error, if that
   * error exists
   * 
   * @param errorType the type of error
   * @return the corresponding error message, or {@code null} if the error does
   *         not exist
   */
  public String getErrorMessage(T errorType) {
    return errorMap.get(errorType);
  }

  // Only accessible from the builder.
  private void setError(T errorType, String errorMessage) {
    if (!errorMap.containsKey(errorType)) {
      errorMap.put(errorType, errorMessage);
    }
  }

  /**
   * @return {@code true} if no input errors exist, {@code false} otherwise
   */
  public boolean isEmpty() {
    return errorMap.isEmpty();
  }

  public static <T extends Enum<T>> Builder<T> builder(Class<T> errorClass) {
    return new Builder<T>(errorClass);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('{');
    sb.append("errorMap=").append(errorMap);
    sb.append('}');
    return sb.toString();
  }

  /**
   * Returns an {@link InputErrors} instance containing only the given type of
   * error.
   * 
   * @param errorType the type of error
   * @param errorMessage the corresponding error message
   * @return the input errors
   */
  public static <E extends Enum<E>> InputErrors<E> of(
      E errorType, String errorMessage) {
    Class<E> errorClass = errorType.getDeclaringClass();
    InputErrors<E> inputErrors = new InputErrors<E>(errorClass);
    inputErrors.setError(errorType, errorMessage);
    return inputErrors;
  }

  /**
   * A builder for a new collection of input errors.
   */
  public static class Builder<T extends Enum<T>> {
    private InputErrors<T> inputErrors;

    /**
     * Creates a new builder for a new collection of input errors.
     * 
     * @param errorClass the class of errors
     */
    public Builder(Class<T> errorClass) {
      inputErrors = new InputErrors<T>(errorClass);
    }

    /**
     * Sets the error message associated with the given type of error, if that
     * error exists.
     * 
     * @param errorType the type of error
     * @param errorMessage the corresponding error message
     */
    public void setError(T errorType, String errorMessage) {
      inputErrors.setError(errorType, errorMessage);
    }

    /**
     * @return {@code true} if {@link #setError(Enum, String)} has been called
     *         yet, {@code false} otherwise
     */
    public boolean setAny() {
      return !inputErrors.isEmpty();
    }

    /**
     * @return the built {@link InputErrors} instance
     */
    public InputErrors<T> build() {
      if (inputErrors.isEmpty()) {
        return getEmpty();
      }

      return inputErrors;
    }
  }
}
