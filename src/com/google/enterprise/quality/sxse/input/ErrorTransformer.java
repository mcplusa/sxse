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
 * A transformer of one error type to another, useful when composing
 * {@link InputParser} instances.
 */
public class ErrorTransformer<S extends Enum<S>, D extends Enum<D>> {
  private static final class TransformedError<D> {
    private final D newError;
    private final String prependMessage;

    private TransformedError(D newError, String prependMessage) {
      this.newError = newError;
      this.prependMessage = prependMessage;
    }
  }

  private final Map<S, TransformedError<D>> enumMap;
  private final Class<D> errorClass;

  private ErrorTransformer(
      Map<S, TransformedError<D>> enumMap, Class<D> errorClass) {
    this.enumMap = enumMap;
    this.errorClass = errorClass;
  }

  /**
   * Transforms all errors in the given collection of input errors, and returns
   * the transformed errors in a new collection.
   * 
   * @param src the collection of errors to transform
   * @return the collection of transformed errors
   */
  public InputErrors<D> transform(InputErrors<S> src) {
    InputErrors.Builder<D> builder = InputErrors.builder(errorClass);
    transform(src, builder);
    return builder.build();
  }

  /**
   * Transfroms all errors in the given collection of input errors, and adds the
   * transformed errors and their error messages to the given builder.
   * 
   * @param src the collection of errors to transform
   * @param builder the builder to add the transformed errors to
   */
  public void transform(InputErrors<S> src, InputErrors.Builder<D> builder) {
    for (Map.Entry<S, TransformedError<D>> entry : enumMap.entrySet()) {
      String errorMessage = src.getErrorMessage(entry.getKey());
      if (errorMessage != null) {
        TransformedError<D> transformedError = entry.getValue();
        if (transformedError.prependMessage != null) {
          builder.setError(transformedError.newError,
              transformedError.prependMessage + errorMessage);
        } else {
          builder.setError(transformedError.newError, errorMessage);
        }
      }
    }
  }

  /**
   * Returns a new builder for transformations.
   * 
   * @param srcClass the class of errors to transform
   * @param dstClass the class of transformed errors
   * @return a new builder of transformations
   */
  public static <S extends Enum<S>, D extends Enum<D>> Builder<S, D> builder(
      Class<S> srcClass, Class<D> dstClass) {
    return new Builder<S, D>(srcClass, dstClass);
  }

  /**
   * A builder for transformations  
   *
   * @param <S> the type of errors to transform
   * @param <D> the type of transformed errors
   */
  public static class Builder<S extends Enum<S>, D extends Enum<D>> {
    private final Class<D> errorClass;
    private final Map<S, TransformedError<D>> enumMap;

    public Builder(Class<S> srcClass, Class<D> dstClass) {
      this.errorClass = dstClass;
      this.enumMap = new EnumMap<S, TransformedError<D>>(srcClass);
    }

    /**
     * Creates a new transformation from one error to another.
     * 
     * @param from the error to transform
     * @param to the transformed error
     * @return this builder
     */
    public Builder<S, D> add(S from, D to) {
      add(from, to, null);
      return this;
    }

    /**
     * Creates a new transformation from one error to another.
     * 
     * @param from the error to transform
     * @param to the transformed error
     * @param prependMessage text to prepend to the original error message
     * @return this builder
     */
    public Builder<S, D> add(S from, D to, String prependMessage) {
      enumMap.put(from, new TransformedError<D>(to, prependMessage));
      return this;
    }

    /**
     * Creates new transformations from each error in the given collection to a
     * single error.
     * 
     * @param from the errors to transform
     * @param to the transformed error
     * @return this builder
     */
    public Builder<S, D> addAll(Iterable<S> from, D to) {
      addAll(from, to, null);
      return this;
    }

    /**
     * Creates new transformations from each error in the given collection to a
     * single error.
     * 
     * @param from the errors to transform
     * @param to the transformed error
     * @param prependMessage text to prepend to the original error message
     * @return this builder
     */
    public Builder<S, D> addAll(Iterable<S> from, D to, String prependMessage) {
      for (S fromEntry : from) {
        add(fromEntry, to, prependMessage);
      }
      return this;
    }
    
    /**
     * Creates new transformations from each error in the given collection to a
     * single error.
     * 
     * @param from the errors to transform
     * @param to the transformed error
     * @return this builder
     */
    public Builder<S, D> addAll(S[] from, D to) {
      addAll(from, to, null);
      return this;
    }

    /**
     * Creates new transformations from each error in the given collection to a
     * single error.
     * 
     * @param from the errors to transform
     * @param to the transformed error
     * @param prependMessage text to prepend to the original error message
     * @return this builder
     */
    public Builder<S, D> addAll(S[] from, D to, String prependMessage) {
      for (S fromElement : from) {
        add(fromElement, to, prependMessage);
      }
      return this;
    }

    /**
     * Builds the {@link ErrorTransformer} instance.
     * 
     * @return the error translator
     */
    public ErrorTransformer<S, D> build() {
      return new ErrorTransformer<S, D>(
          new EnumMap<S, TransformedError<D>>(enumMap), errorClass);
    }
  }
}
