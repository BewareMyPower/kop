/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.streamnative.pulsar.handlers.kop.format;

import io.netty.buffer.ByteBuf;
import io.netty.util.Recycler;
import io.netty.util.Recycler.Handle;
import lombok.Getter;
import org.apache.kafka.common.record.MemoryRecords;

/**
 * Result of decode in entry formatter.
 */
public class DecodeResult {

    private final Handle<DecodeResult> recyclerHandle;

    private static final Recycler<DecodeResult> RECYCLER = new Recycler<DecodeResult>() {
        @Override
        protected DecodeResult newObject(Handle<DecodeResult> handle) {
            return new DecodeResult(handle);
        }
    };

    @Getter
    private MemoryRecords records;
    private ByteBuf releasedByteBuf;

    private DecodeResult(Handle<DecodeResult> recyclerHandle) {
        this.recyclerHandle = recyclerHandle;
    }

    public static DecodeResult get(MemoryRecords records, ByteBuf byteBuf) {
        DecodeResult decodeResult = RECYCLER.get();
        decodeResult.records = records;
        decodeResult.releasedByteBuf = byteBuf;
        return decodeResult;
    }

    public void release() {
        if (releasedByteBuf != null) {
            releasedByteBuf.release();
        }
        recycle();
    }

    private void recycle() {
        records = null;
        releasedByteBuf = null;
        recyclerHandle.recycle(this);
    }
}
