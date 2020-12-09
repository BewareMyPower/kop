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
package io.streamnative.pulsar.handlers.kop;

import static org.testng.Assert.assertEquals;

import io.streamnative.pulsar.handlers.kop.utils.MessageIdUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.pulsar.client.impl.MessageIdImpl;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


/**
 * Test Kafka only mode.
 */
@Slf4j
public class KafkaOnlyModeTest extends KopProtocolHandlerTestBase {

    @BeforeMethod
    @Override
    protected void setup() throws Exception {
        super.internalSetup();
    }

    @AfterMethod
    @Override
    protected void cleanup() throws Exception {
        super.internalCleanup();
    }

    @Test(timeOut = 30000)
    public void testProduceConsume() {
        final String bootstrapServers = "localhost:" + getKafkaBrokerPort();
        final String topic = "KafkaOnlyModeTest-ProduceConsume";
        final String messagePrefix = "msg-";
        final int numMessages = 10;

        final Properties producerConfig = new Properties();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class);
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerConfig.put(ProducerConfig.BATCH_SIZE_CONFIG, "20");

        @Cleanup
        final KafkaProducer<Integer, String> producer = new KafkaProducer<>(producerConfig);

        for (int i = 0; i < numMessages; i++) {
            final int index = i;
            producer.send(new ProducerRecord<>(topic, i, messagePrefix + i), new Callback() {
                @Override
                public void onCompletion(RecordMetadata metadata, Exception exception) {
                    MessageIdImpl id = (MessageIdImpl) MessageIdUtils.getMessageId(metadata.offset());
                    log.info("Success write message {} to {} ({}, {})", index, metadata.offset(),
                            id.getLedgerId(), id.getEntryId());
                }
            });
        }

        producer.flush();

        log.info("---- Start consume ----");

        final Properties consumerConfig = new Properties();
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, "my-group");
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class);
        consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        final KafkaConsumer<Integer, String> consumer = new KafkaConsumer<>(consumerConfig);
        consumer.subscribe(Collections.singleton(topic));

        for (int i = 0; i < numMessages; ) {
            for (ConsumerRecord<Integer, String> record : consumer.poll(Duration.ofSeconds(1))) {
                log.info("Success read message from {}-{}@{}, key: {}, value: {}",
                        record.topic(), record.partition(), record.offset(), record.key(), record.value());
                assertEquals(record.key().intValue(), i);
                assertEquals(record.value(), messagePrefix + i);
                i++;
            }
        }
    }
}
