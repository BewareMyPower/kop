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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.ValueJoiner;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.testng.annotations.AfterMethod;

/**
 * Tests all available joins of Kafka Streams DSL.
 */
public abstract class AbstractJoinTest extends KafkaStreamsTestBase {
    protected String inputTopicRight;
    protected String inputTopicLeft;
    protected String outputTopic;
    private final long anyUniqueKey = 0L;

    private static final Properties PRODUCER_CONFIG = new Properties();
    private static final Properties RESULT_CONSUMER_CONFIG = new Properties();
    private KafkaProducer<Long, String> producer;

    private List<Input<String>> input;

    final ValueJoiner<String, String, String> valueJoiner = (value1, value2) -> value1 + "-" + value2;

    @Override
    protected void createTopics() throws Exception {
        inputTopicLeft = "inputTopicLeft-" + getTestNo();
        inputTopicRight = "inputTopicRight-" + getTestNo();
        outputTopic = "outputTopic-" + getTestNo();
        input = Arrays.asList(
                new Input<>(inputTopicLeft, (String) null),
                new Input<>(inputTopicRight, (String) null),
                new Input<>(inputTopicLeft, "A"),
                new Input<>(inputTopicRight, "a"),
                new Input<>(inputTopicLeft, "B"),
                new Input<>(inputTopicRight, "b"),
                new Input<>(inputTopicLeft, (String) null),
                new Input<>(inputTopicRight, (String) null),
                new Input<>(inputTopicLeft, "C"),
                new Input<>(inputTopicRight, "c"),
                new Input<>(inputTopicRight, (String) null),
                new Input<>(inputTopicLeft, (String) null),
                new Input<>(inputTopicRight, (String) null),
                new Input<>(inputTopicRight, "d"),
                new Input<>(inputTopicLeft, "D")
        );
        admin.topics().createPartitionedTopic(inputTopicLeft, 1);
        admin.topics().createPartitionedTopic(inputTopicRight, 1);
        admin.topics().createPartitionedTopic(outputTopic, 1);
    }

    @Override
    protected void extraSetup() throws Exception {
        PRODUCER_CONFIG.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        PRODUCER_CONFIG.put(ProducerConfig.ACKS_CONFIG, "all");
        PRODUCER_CONFIG.put(ProducerConfig.RETRIES_CONFIG, 0);
        PRODUCER_CONFIG.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        PRODUCER_CONFIG.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        RESULT_CONSUMER_CONFIG.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        RESULT_CONSUMER_CONFIG.put(ConsumerConfig.GROUP_ID_CONFIG, getApplicationIdPrefix() + "-result-consumer");
        RESULT_CONSUMER_CONFIG.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        RESULT_CONSUMER_CONFIG.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        RESULT_CONSUMER_CONFIG.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        producer = new KafkaProducer<>(PRODUCER_CONFIG);
    }

    @Override
    protected Class<?> getKeySerdeClass() {
        return Serdes.Long().getClass();
    }

    @Override
    protected Class<?> getValueSerdeClass() {
        return Serdes.String().getClass();
    }

    @AfterMethod
    @Override
    protected void cleanupTestCase() throws Exception {
        producer.close(1, TimeUnit.SECONDS);
        super.cleanupTestCase();
    }

    private void checkResult(final String outputTopic, final List<String> expectedResult) throws InterruptedException {
        final List<String> result = TestUtils.waitUntilMinValuesRecordsReceived(
                RESULT_CONSUMER_CONFIG, outputTopic, expectedResult.size(), 30 * 1000L);
        assertThat(result, is(expectedResult));
    }

    private void checkResult(final String outputTopic,
                             final String expectedFinalResult,
                             final int expectedTotalNumRecords) throws InterruptedException {
        final List<String> result = TestUtils.waitUntilMinValuesRecordsReceived(
                RESULT_CONSUMER_CONFIG, outputTopic, expectedTotalNumRecords, 30 * 1000L);
        assertThat(result.get(result.size() - 1), is(expectedFinalResult));
    }

    /*
     * Runs the actual test. Checks the result after each input record to ensure fixed processing order.
     * If an input tuple does not trigger any result, "expectedResult" should contain a "null" entry
     */
    void runTest(final List<List<String>> expectedResult) throws Exception {
        runTest(expectedResult, null);
    }


    /*
     * Runs the actual test. Checks the result after each input record to ensure fixed processing order.
     * If an input tuple does not trigger any result, "expectedResult" should contain a "null" entry
     */
    void runTest(final List<List<String>> expectedResult, final String storeName) throws Exception {
        assert expectedResult.size() == input.size();

        TestUtils.purgeLocalStreamsState(streamsConfiguration);
        kafkaStreams = new KafkaStreams(builder.build(), streamsConfiguration);

        String expectedFinalResult = null;

        try {
            kafkaStreams.start();

            long ts = System.currentTimeMillis();

            final Iterator<List<String>> resultIterator = expectedResult.iterator();
            for (final Input<String> singleInput : input) {
                producer.send(new ProducerRecord<>(singleInput.topic, null, ++ts, singleInput.record.key,
                        singleInput.record.value)).get();

                List<String> expected = resultIterator.next();

                if (expected != null) {
                    checkResult(outputTopic, expected);
                    expectedFinalResult = expected.get(expected.size() - 1);
                }
            }

            if (storeName != null) {
                checkQueryableStore(storeName, expectedFinalResult);
            }
        } finally {
            kafkaStreams.close();
        }
    }

    /*
     * Checks the embedded queryable state store snapshot
     */
    private void checkQueryableStore(final String queryableName, final String expectedFinalResult) {
        final ReadOnlyKeyValueStore<Long, String> store =
                kafkaStreams.store(queryableName, QueryableStoreTypes.<Long, String>keyValueStore());

        final KeyValueIterator<Long, String> all = store.all();
        final KeyValue<Long, String> onlyEntry = all.next();

        try {
            assertThat(onlyEntry.key, is(anyUniqueKey));
            assertThat(onlyEntry.value, is(expectedFinalResult));
            assertThat(all.hasNext(), is(false));
        } finally {
            all.close();
        }
    }

    private final class Input<V> {
        String topic;
        KeyValue<Long, V> record;

        Input(final String topic, final V value) {
            this.topic = topic;
            record = KeyValue.pair(anyUniqueKey, value);
        }
    }
}
