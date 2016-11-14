/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.activemq.artemis.concurrent;

import java.io.Serializable;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

import io.netty.util.internal.shaded.org.jctools.util.UnsafeAccess;
import sun.misc.Unsafe;

abstract class SameClassFieldsPadding extends Number {

   long p01, p02, p03, p04, p05, p06;
   long p10, p11, p12, p13, p14, p15, p16, p17;
}

abstract class Value extends SameClassFieldsPadding {

   // setup to use Unsafe.compareAndSwapInt for updates
   protected static final Unsafe unsafe = UnsafeAccess.UNSAFE;
   protected static final long valueOffset;

   static {
      try {
         valueOffset = unsafe.objectFieldOffset(Value.class.getDeclaredField("value"));
      } catch (Exception ex) {
         throw new Error(ex);
      }
   }

   protected volatile int value;

}

abstract class NeighbourInstancesPadding extends Value {

   long p01, p02, p03, p04, p05, p06, p07;
   long p10, p11, p12, p13, p14, p15, p16, p17;
}

public final class PaddedAtomicInteger extends NeighbourInstancesPadding implements Serializable {

   private static final long serialVersionUID = 6214790243416807050L;

   /**
    * Creates a new PaddedAtomicInteger with the given initial value.
    *
    * @param initialValue the initial value
    */
   public PaddedAtomicInteger(int initialValue) {
      value = initialValue;
   }

   /**
    * Creates a new PaddedAtomicInteger with initial value {@code 0}.
    */
   public PaddedAtomicInteger() {
   }

   /**
    * Gets the current value.
    *
    * @return the current value
    */
   public int get() {
      return value;
   }

   /**
    * Sets to the given value.
    *
    * @param newValue the new value
    */
   public void set(int newValue) {
      value = newValue;
   }

   /**
    * Eventually sets to the given value.
    *
    * @param newValue the new value
    * @since 1.6
    */
   public void lazySet(int newValue) {
      unsafe.putOrderedInt(this, valueOffset, newValue);
   }

   /**
    * Atomically sets to the given value and returns the old value.
    *
    * @param newValue the new value
    * @return the previous value
    */
   public int getAndSet(int newValue) {
      return unsafe.getAndSetInt(this, valueOffset, newValue);
   }

   /**
    * Atomically sets the value to the given updated value
    * if the current value {@code ==} the expected value.
    *
    * @param expect the expected value
    * @param update the new value
    * @return {@code true} if successful. False return indicates that
    * the actual value was not equal to the expected value.
    */
   public boolean compareAndSet(int expect, int update) {
      return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
   }

   /**
    * Atomically sets the value to the given updated value
    * if the current value {@code ==} the expected value.
    *
    * <p><a href="package-summary.html#weakCompareAndSet">May fail
    * spuriously and does not provide ordering guarantees</a>, so is
    * only rarely an appropriate alternative to {@code compareAndSet}.
    *
    * @param expect the expected value
    * @param update the new value
    * @return {@code true} if successful
    */
   public boolean weakCompareAndSet(int expect, int update) {
      return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
   }

   /**
    * Atomically increments by one the current value.
    *
    * @return the previous value
    */
   public int getAndIncrement() {
      return unsafe.getAndAddInt(this, valueOffset, 1);
   }

   /**
    * Atomically decrements by one the current value.
    *
    * @return the previous value
    */
   public int getAndDecrement() {
      return unsafe.getAndAddInt(this, valueOffset, -1);
   }

   /**
    * Atomically adds the given value to the current value.
    *
    * @param delta the value to add
    * @return the previous value
    */
   public int getAndAdd(int delta) {
      return unsafe.getAndAddInt(this, valueOffset, delta);
   }

   /**
    * Atomically increments by one the current value.
    *
    * @return the updated value
    */
   public int incrementAndGet() {
      return unsafe.getAndAddInt(this, valueOffset, 1) + 1;
   }

   /**
    * Atomically decrements by one the current value.
    *
    * @return the updated value
    */
   public int decrementAndGet() {
      return unsafe.getAndAddInt(this, valueOffset, -1) - 1;
   }

   /**
    * Atomically adds the given value to the current value.
    *
    * @param delta the value to add
    * @return the updated value
    */
   public int addAndGet(int delta) {
      return unsafe.getAndAddInt(this, valueOffset, delta) + delta;
   }

   /**
    * Atomically updates the current value with the results of
    * applying the given function, returning the previous value. The
    * function should be side-effect-free, since it may be re-applied
    * when attempted updates fail due to contention among threads.
    *
    * @param updateFunction a side-effect-free function
    * @return the previous value
    * @since 1.8
    */
   public int getAndUpdate(IntUnaryOperator updateFunction) {
      int prev, next;
      do {
         prev = get();
         next = updateFunction.applyAsInt(prev);
      } while (!compareAndSet(prev, next));
      return prev;
   }

   /**
    * Atomically updates the current value with the results of
    * applying the given function, returning the updated value. The
    * function should be side-effect-free, since it may be re-applied
    * when attempted updates fail due to contention among threads.
    *
    * @param updateFunction a side-effect-free function
    * @return the updated value
    * @since 1.8
    */
   public int updateAndGet(IntUnaryOperator updateFunction) {
      int prev, next;
      do {
         prev = get();
         next = updateFunction.applyAsInt(prev);
      } while (!compareAndSet(prev, next));
      return next;
   }

   /**
    * Atomically updates the current value with the results of
    * applying the given function to the current and given values,
    * returning the previous value. The function should be
    * side-effect-free, since it may be re-applied when attempted
    * updates fail due to contention among threads.  The function
    * is applied with the current value as its first argument,
    * and the given update as the second argument.
    *
    * @param x                   the update value
    * @param accumulatorFunction a side-effect-free function of two arguments
    * @return the previous value
    * @since 1.8
    */
   public int getAndAccumulate(int x, IntBinaryOperator accumulatorFunction) {
      int prev, next;
      do {
         prev = get();
         next = accumulatorFunction.applyAsInt(prev, x);
      } while (!compareAndSet(prev, next));
      return prev;
   }

   /**
    * Atomically updates the current value with the results of
    * applying the given function to the current and given values,
    * returning the updated value. The function should be
    * side-effect-free, since it may be re-applied when attempted
    * updates fail due to contention among threads.  The function
    * is applied with the current value as its first argument,
    * and the given update as the second argument.
    *
    * @param x                   the update value
    * @param accumulatorFunction a side-effect-free function of two arguments
    * @return the updated value
    * @since 1.8
    */
   public int accumulateAndGet(int x, IntBinaryOperator accumulatorFunction) {
      int prev, next;
      do {
         prev = get();
         next = accumulatorFunction.applyAsInt(prev, x);
      } while (!compareAndSet(prev, next));
      return next;
   }

   /**
    * Returns the String representation of the current value.
    *
    * @return the String representation of the current value
    */
   @Override
   public String toString() {
      return Integer.toString(get());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int intValue() {
      return get();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public long longValue() {
      return (long) get();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public float floatValue() {
      return (float) get();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public double doubleValue() {
      return (double) get();
   }

}
