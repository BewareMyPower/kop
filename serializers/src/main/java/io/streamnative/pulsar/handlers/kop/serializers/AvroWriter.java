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

import io.streamnative.kafka.serializers.common.SchemaVersionHeader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.avro.Schema;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;

public class AvroWriter<T> {

    private final EncoderFactory encoderFactory = EncoderFactory.get();
    private final ReflectDatumWriter<T> writer;

    public AvroWriter(final Schema schema) {
        this.writer = new ReflectDatumWriter<>(schema);
    }

    public byte[] write(final Long schemaVersion, final T value) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (schemaVersion != null) {
            out.write(SchemaVersionHeader.encode(schemaVersion));
        }
        final BinaryEncoder encoder = encoderFactory.directBinaryEncoder(out, null);
        writer.write(value, encoder);
        encoder.flush();
        return out.toByteArray();
    }
}
