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
package io.streamnative.pulsar.handlers.kop.streams;

import java.util.Collections;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.KeyValueMapper;

/**
 * Mocked mapper used for Kafka Streams.
 */
public class MockMapper {

    private static class NoOpKeyValueMapper<K, V> implements KeyValueMapper<K, V, KeyValue<K, V>> {
        @Override
        public KeyValue<K, V> apply(K key, V value) {
            return KeyValue.pair(key, value);
        }
    }

    private static class NoOpFlatKeyValueMapper<K, V> implements KeyValueMapper<K, V, Iterable<KeyValue<K, V>>> {
        @Override
        public Iterable<KeyValue<K, V>> apply(K key, V value) {
            return Collections.singletonList(KeyValue.pair(key, value));
        }
    }
    private static class SelectKeyMapper<K, V> implements KeyValueMapper<K, V, K> {
        @Override
        public K apply(K key, V value) {
            return key;
        }
    }

    public static <K, V> KeyValueMapper<K, V, Iterable<KeyValue<K, V>>> noOpFlatKeyValueMapper() {
        return new NoOpFlatKeyValueMapper<>();
    }

    public static <K, V> KeyValueMapper<K, V, KeyValue<K, V>> noOpKeyValueMapper() {
        return new NoOpKeyValueMapper<>();
    }

    public static <K, V> KeyValueMapper<K, V, K> selectKeyKeyValueMapper() {
        return new SelectKeyMapper<>();
    }
}
