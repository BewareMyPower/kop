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
import static org.testng.Assert.assertTrue;

import io.streamnative.pulsar.handlers.kop.utils.KopTopic;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.TopicPartition;
import org.apache.pulsar.broker.service.persistent.PersistentTopic;
import org.apache.pulsar.client.admin.PulsarAdminException;
import org.apache.pulsar.common.naming.NamespaceBundle;
import org.apache.pulsar.common.naming.TopicName;
import org.apache.pulsar.common.policies.data.ConsumerStats;
import org.testng.annotations.Test;

/**
 * Basic end-to-end test with `entryFormat=kafka`.
 */
@Slf4j
public class BasicEndToEndKafkaTest extends BasicEndToEndTestBase {

    public BasicEndToEndKafkaTest() {
        super("kafka");
    }

    @Test(timeOut = 20000)
    public void testNullValueMessages() throws Exception {
        final String topic = "test-produce-null-value";

        @Cleanup
        final KafkaProducer<String, String> kafkaProducer = newKafkaProducer();
        sendSingleMessages(kafkaProducer, topic, Arrays.asList(null, ""));
        sendBatchedMessages(kafkaProducer, topic, Arrays.asList("test", "", null));

        final List<String> expectedMessages = Arrays.asList(null, "", "test", "", null);

        @Cleanup
        final KafkaConsumer<String, String> kafkaConsumer = newKafkaConsumer(topic);
        List<String> kafkaReceives = receiveMessages(kafkaConsumer, expectedMessages.size());
        assertEquals(kafkaReceives, expectedMessages);
    }

    @Test(timeOut = 20000)
    public void testKafkaConsumerMetrics() throws Exception {
        final String topic = "test-kafka-consumer-metrics";
        final List<String> expectedMessages = Arrays.asList("A", "B", "C");

        @Cleanup
        final KafkaProducer<String, String> kafkaProducer = newKafkaProducer();
        sendSingleMessages(kafkaProducer, topic, expectedMessages);

        @Cleanup
        final KafkaConsumer<String, String> kafkaConsumer = newKafkaConsumer(topic);
        List<String> kafkaReceives = receiveMessages(kafkaConsumer, expectedMessages.size());
        assertEquals(kafkaReceives, expectedMessages);

        // Check stats
        final TopicName topicName = TopicName.get(KopTopic.toString(new TopicPartition(topic, 0)));
        final NamespaceBundle bundle = pulsar.getNamespaceService().getBundle(topicName);
        final PersistentTopic persistentTopic = (PersistentTopic) pulsar.getBrokerService().getMultiLayerTopicsMap()
                .get(topicName.getNamespace())
                .get(bundle.toString())
                .get(topicName.toString());
        final ConsumerStats stats =
                persistentTopic.getSubscriptions().get(GROUP_ID).getDispatcher().getConsumers().get(0).getStats();
        log.info("Consumer stats: [msgOutCounter={}] [bytesOutCounter={}]",
                stats.msgOutCounter, stats.bytesOutCounter);
        assertEquals(stats.msgOutCounter, expectedMessages.size());
        assertTrue(stats.bytesOutCounter > 0);
    }

    @Test(timeOut = 20000)
    public void testDeleteClosedTopics() throws Exception {
        final String topic = "test-delete-closed-topics";
        final List<String> expectedMessages = Collections.singletonList("msg");

        final KafkaProducer<String, String> kafkaProducer = newKafkaProducer();
        sendSingleMessages(kafkaProducer, topic, expectedMessages);

        try {
            admin.topics().deletePartitionedTopic(topic);
        } catch (PulsarAdminException e) {
            assertTrue(e.getMessage().contains("Topic has active producers/subscriptions"));
        }

        final KafkaConsumer<String, String> kafkaConsumer1 = newKafkaConsumer(topic, "sub-1");
        assertEquals(receiveMessages(kafkaConsumer1, expectedMessages.size()), expectedMessages);
        try {
            admin.topics().deletePartitionedTopic(topic);
        } catch (PulsarAdminException e) {
            assertTrue(e.getMessage().contains("Topic has active producers/subscriptions"));
        }

        final KafkaConsumer<String, String> kafkaConsumer2 = newKafkaConsumer(topic, "sub-2");
        assertEquals(receiveMessages(kafkaConsumer2, expectedMessages.size()), expectedMessages);

        kafkaProducer.close();
        kafkaConsumer1.close();
        try {
            admin.topics().deletePartitionedTopic(topic);
        } catch (PulsarAdminException e) {
            assertTrue(e.getMessage().contains("Topic has active producers/subscriptions"));
        }

        kafkaConsumer2.close();
        admin.topics().deletePartitionedTopic(topic);
    }
}
