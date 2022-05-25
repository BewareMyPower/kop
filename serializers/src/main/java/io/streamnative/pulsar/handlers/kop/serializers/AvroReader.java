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
package io.streamnative.pulsar.handlers.kop.serializers;

import java.io.IOException;
import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.reflect.ReflectDatumReader;

public class AvroReader<T> {

    private static final ThreadLocal<BinaryDecoder> THREAD_LOCAL_DECODER = new ThreadLocal<>();
    private final ReflectDatumReader<T> reader;

    public AvroReader(final Schema schema) {
        this.reader = new ReflectDatumReader<>(schema);
    }

    public T read(byte[] bytes, int start, int length) throws IOException {
        final BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(
                bytes, start, length, THREAD_LOCAL_DECODER.get());
        if (THREAD_LOCAL_DECODER.get() == null) {
            THREAD_LOCAL_DECODER.set(decoder);
        }
        return reader.read(null, decoder);
    }
}
