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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test kafka offset commit with `enablePulsarAck=false`.
 */
@Slf4j
public class DisablePulsarAckTest extends KopProtocolHandlerTestBase {

    @BeforeClass
    @Override
    protected void setup() throws Exception {
        super.resetConfig();
        conf.setEnablePulsarAck(false);
        super.internalSetup();
    }

    @AfterClass
    @Override
    protected void cleanup() throws Exception {
        super.internalCleanup();
    }

    @Test(timeOut = 20000)
    public void testCommitOffset() throws Exception {
        final String topic = "test-commit-offset";

        @Cleanup
        final KafkaProducer<String, String> producer = new KafkaProducer<>(newKafkaProducerProperties());
        for (int i = 0; i < 10; i++) {
            producer.send(new ProducerRecord<>(topic, "hello")).get();
        }

        final KafkaConsumer<String, String> consumer1 = new KafkaConsumer<>(newKafkaConsumerProperties());
        consumer1.subscribe(Collections.singleton(topic));
        int numReceived = 0;
        while (numReceived < 10) {
            final ConsumerRecords<String, String> records = consumer1.poll(Duration.ofSeconds(1));
            numReceived += records.count();
        }
        consumer1.close(); // commit offsets automatically

        @Cleanup
        final KafkaConsumer<String, String> consumer2 = new KafkaConsumer<>(newKafkaConsumerProperties());
        consumer2.subscribe(Collections.singleton(topic));

        for (int i = 0; i < 10; i++) {
            producer.send(new ProducerRecord<>(topic, "msg-" + i)).get();
        }

        numReceived = 0;
        final List<String> messages = new ArrayList<>();
        while (numReceived < 10) {
            for (ConsumerRecord<String, String> record : consumer2.poll(Duration.ofSeconds(1))) {
                if (log.isDebugEnabled()) {
                    log.debug("Consumer2 received {}", record.value());
                }
                messages.add(record.value());
                Assert.assertEquals(record.value(), "msg-" + numReceived);
                numReceived++;
            }
        }
        Assert.assertEquals(messages.size(), 10);
    }
}
