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

package org.apache.activemq.artemis.core.io.mapped.batch;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.activemq.artemis.core.io.DummyCallback;
import org.apache.activemq.artemis.core.io.IOCallback;

final class BatchAppender implements BatchWriteCommandExecutor {

   private final List<IOCallback> callbacks;
   private SequentialFileWriter observer;
   private boolean closed;
   private boolean requiredSync;

   BatchAppender() {
      this.observer = null;
      this.closed = false;
      this.callbacks = new ArrayList<>();
      this.requiredSync = false;
   }

   @Override
   public void onWrite(ByteBuffer buffer, int index, int length, IOCallback callback) {
      if (closed) {
         if (callback != null) {
            callback.onError(-1, "can't write messages after a close!");
         }
      } else {
         if (this.observer == null) {
            if (callback != null) {
               callback.onError(-1, "can't write messages without a TimedBufferObserver!");
            }
         } else {
            flushBatch(buffer, index, length);
            if (callback != null) {
               assert callback == DummyCallback.getInstance();
               callbacks.add(callback);
            }
         }
      }
   }

   @Override
   public void onWriteAndFlush(ByteBuffer buffer, int index, int length, IOCallback callback) {
      if (closed) {
         if (callback != null) {
            callback.onError(-1, "can't write messages after a close!");
         }
      } else {
         if (this.observer == null) {
            if (callback != null) {
               callback.onError(-1, "can't write messages without a TimedBufferObserver!");
            }
         } else {
            flushBatch(buffer, index, length);
            this.requiredSync = true;
            if (callback != null) {
               callbacks.add(callback);
            }
         }
      }
   }

   @Override
   public void onClose() {
      if (!closed) {
         //force endOfBatch
         endOfBatch();
         this.observer = null;
         this.closed = true;
      }
   }

   @Override
   public void onFlush(IOCallback callback) {
      if (closed) {
         callback.onError(-1, "can't flush messages after a close!");
      } else {
         if (this.observer == null) {
            callback.onError(-1, "can't flush messages without a TimedBufferObserver!");
         } else {
            this.requiredSync = true;
            this.callbacks.add(callback);
         }
      }
   }

   @Override
   public void onChangeObserver(SequentialFileWriter newObserver, IOCallback callback) {
      if (closed) {
         callback.onError(-1, "can't set a TimedBufferObserver after a close!");
      } else {
         try {
            if (this.observer != null) {
               //it is a premature end of batch!
               endOfBatch();
               //alert the observer that no write buffer is active on it!
               this.observer.onDeactivatedTimedBuffer();
            }
         } finally {
            this.observer = newObserver;
            if (this.observer != null) {
               try {
                  callback.done();
               } catch (Exception ex) {
                  callback.onError(-1, ex.getMessage());
               }
            } else {
               callback.done();
            }
         }
      }
   }

   @Override
   public boolean isClosed() {
      return closed;
   }

   @Override
   public void endOfBatch() {
      if (!this.closed & this.observer != null) {
         //IT CAN ISSUE A SYNC WITH NO UNFLUSHED BYTES!!!
         //cases:
         //- already flushed writes (with syncs or pending callbacks to be issued in order with the synced ones)
         //- only syncs
         if (this.requiredSync || !this.callbacks.isEmpty()) {
            try {
               this.observer.flushBuffer(null, 0, 0, requiredSync, callbacks);
            } finally {
               this.callbacks.clear();
               this.requiredSync = false;
            }
         }
      }

   }

   private void flushBatch(ByteBuffer buffer, int index, int length) {
      if (!this.closed & this.observer != null) {
         if (length > 0) {
            this.observer.flushBuffer(buffer, index, length, false, Collections.emptyList());
         }
      }
   }
}
