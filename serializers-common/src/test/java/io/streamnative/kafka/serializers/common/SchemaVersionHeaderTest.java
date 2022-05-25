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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertThrows;

import org.testng.annotations.Test;

public class SchemaVersionHeaderTest {

    @Test
    public void testEncodeAndDecode() {
        final long value = 1234578L;
        assertEquals(SchemaVersionHeader.encode(value), new byte[]{
                SchemaVersionHeader.MAGIC[0], SchemaVersionHeader.MAGIC[1],
                0, 0, 0, 0, 0, 18, -42, -110 });
        for (int i = 0; i < 100; i++) {
            final byte[] data = SchemaVersionHeader.encode(value + i);
            assertEquals(SchemaVersionHeader.decode(data).orElse(-1L).longValue(), value + i);
        }
    }

    @Test
    public void testInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> SchemaVersionHeader.decode(null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> SchemaVersionHeader.decode(new byte[0], -1, 0));
        assertThrows(IllegalArgumentException.class, () -> SchemaVersionHeader.decode(new byte[0], 0, -1));

        for (int i = 0; i <= SchemaVersionHeader.MAGIC.length; i++) {
            assertFalse(SchemaVersionHeader.decode(new byte[i]).isPresent());
        }
        for (int i = SchemaVersionHeader.MAGIC.length; i < SchemaVersionHeader.HEADER_SIZE; i++) {
            final byte[] data = new byte[i];
            data[0] = SchemaVersionHeader.MAGIC[0];
            data[1] = SchemaVersionHeader.MAGIC[1];
            assertFalse(SchemaVersionHeader.decode(data).isPresent());
        }
        for (int i = 0; i < 5; i++) {
            final byte[] data = new byte[SchemaVersionHeader.HEADER_SIZE + i];
            data[0] = SchemaVersionHeader.MAGIC[0];
            assertFalse(SchemaVersionHeader.decode(data).isPresent());
            data[1] = SchemaVersionHeader.MAGIC[1];
            assertEquals(SchemaVersionHeader.decode(data).orElse(-1L).longValue(), 0L);
            data[0] = (byte) (SchemaVersionHeader.MAGIC[0] + 1);
            assertFalse(SchemaVersionHeader.decode(data).isPresent());
        }
    }
}
