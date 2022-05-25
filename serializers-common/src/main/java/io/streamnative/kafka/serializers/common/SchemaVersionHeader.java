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
package io.streamnative.kafka.serializers.common;

import java.util.Optional;

/**
 * A header that contains the schema version in Pulsar.
 *
 * The format is:
 *
 * | MAGIC (2 bytes)  | Schema Version for example (8 bytes)    |
 * | :--------------- | :-------------------------------------- |
 * | 0x03 0x04        | 0x00 0x00 0x00 0x00 0x00 0x00 0x00 0x00 |
 *
 * The methods are migrated from Pulsar's LongSchema. The difference is that it doesn't have strict argument checks.
 */
public class SchemaVersionHeader {

    public static final byte[] MAGIC = { 0x03, 0x04 };
    public static final int HEADER_SIZE = MAGIC.length + 8;

    /**
     * Encode the schema version with the specific format.
     *
     * @param version
     * @return
     */
    public static byte[] encode(final long version) {
        return new byte[] {
                MAGIC[0],
                MAGIC[1],
                (byte) (version >>> 56),
                (byte) (version >>> 48),
                (byte) (version >>> 40),
                (byte) (version >>> 32),
                (byte) (version >>> 24),
                (byte) (version >>> 16),
                (byte) (version >>> 8),
                (byte) version
        };
    }

    /**
     * @see SchemaVersionHeader#decode(byte[], int, int)
     */
    public static Optional<Long> decode(final byte[] data) {
        return decode(data, 0, data.length);
    }

    /**
     * Decode the byte range to the long schema version.
     *
     * @param data
     * @param start
     * @param length
     * @return Optional.empty() in following cases:
     *   1. length is less than {@link SchemaVersionHeader#HEADER_SIZE}
     *   2. the first 2 bytes are not the same with {@link SchemaVersionHeader#MAGIC}
     * @throws IllegalArgumentException if `data[i]` for i in range [start, start+length) is invalid
     */
    public static Optional<Long> decode(final byte[] data, final int start, final int length) {
        if (data == null || start < 0 || length < 0) {
            throw new IllegalArgumentException("data is null: " + (data == null)
                    + ", start: " + start + ", length: " + length);
        }
        if (length < HEADER_SIZE || data[start] != MAGIC[0] || data[start + 1] != MAGIC[1]) {
            return Optional.empty();
        }
        long value = 0L;
        for (int i = 2; i < length; i++) {
            value <<= 8;
            value |= data[start + i] & 0xFF;
        }
        return Optional.of(value);
    }
}
