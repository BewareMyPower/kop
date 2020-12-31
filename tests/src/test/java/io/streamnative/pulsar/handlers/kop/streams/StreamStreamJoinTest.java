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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.NonNull;
import org.apache.kafka.streams.kstream.JoinWindows;
import org.apache.kafka.streams.kstream.KStream;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests all available joins of Kafka Streams DSL.
 */
public class StreamStreamJoinTest extends AbstractJoinTest {
    private KStream<Long, String> leftStream;
    private KStream<Long, String> rightStream;

    @Override
    protected @NonNull String getApplicationIdPrefix() {
        return "stream-stream-join-integration-test";
    }

    @BeforeMethod
    @Override
    protected void setupTestCase() throws Exception {
        super.setupTestCase();
        leftStream = builder.stream(inputTopicLeft);
        rightStream = builder.stream(inputTopicRight);
    }

    @Test
    public void testInner() throws Exception {
        final List<List<String>> expectedResult = Arrays.asList(
                null,
                null,
                null,
                Collections.singletonList("A-a"),
                Collections.singletonList("B-a"),
                Arrays.asList("A-b", "B-b"),
                null,
                null,
                Arrays.asList("C-a", "C-b"),
                Arrays.asList("A-c", "B-c", "C-c"),
                null,
                null,
                null,
                Arrays.asList("A-d", "B-d", "C-d"),
                Arrays.asList("D-a", "D-b", "D-c", "D-d")
        );

        leftStream.join(rightStream, valueJoiner, JoinWindows.of(10000)).to(outputTopic);

        runTest(expectedResult);
    }

    @Test
    public void testInnerRepartitioned() throws Exception {
        final List<List<String>> expectedResult = Arrays.asList(
                null,
                null,
                null,
                Collections.singletonList("A-a"),
                Collections.singletonList("B-a"),
                Arrays.asList("A-b", "B-b"),
                null,
                null,
                Arrays.asList("C-a", "C-b"),
                Arrays.asList("A-c", "B-c", "C-c"),
                null,
                null,
                null,
                Arrays.asList("A-d", "B-d", "C-d"),
                Arrays.asList("D-a", "D-b", "D-c", "D-d")
        );

        leftStream.map(MockMapper.<Long, String>noOpKeyValueMapper())
                .join(rightStream.flatMap(MockMapper.<Long, String>noOpFlatKeyValueMapper())
                                .selectKey(MockMapper.<Long, String>selectKeyKeyValueMapper()),
                        valueJoiner, JoinWindows.of(10000)).to(outputTopic);

        runTest(expectedResult);
    }

    @Test
    public void testLeft() throws Exception {
        final List<List<String>> expectedResult = Arrays.asList(
                null,
                null,
                Collections.singletonList("A-null"),
                Collections.singletonList("A-a"),
                Collections.singletonList("B-a"),
                Arrays.asList("A-b", "B-b"),
                null,
                null,
                Arrays.asList("C-a", "C-b"),
                Arrays.asList("A-c", "B-c", "C-c"),
                null,
                null,
                null,
                Arrays.asList("A-d", "B-d", "C-d"),
                Arrays.asList("D-a", "D-b", "D-c", "D-d")
        );

        leftStream.leftJoin(rightStream, valueJoiner, JoinWindows.of(10000)).to(outputTopic);

        runTest(expectedResult);
    }

    @Test
    public void testLeftRepartitioned() throws Exception {
        final List<List<String>> expectedResult = Arrays.asList(
                null,
                null,
                Collections.singletonList("A-null"),
                Collections.singletonList("A-a"),
                Collections.singletonList("B-a"),
                Arrays.asList("A-b", "B-b"),
                null,
                null,
                Arrays.asList("C-a", "C-b"),
                Arrays.asList("A-c", "B-c", "C-c"),
                null,
                null,
                null,
                Arrays.asList("A-d", "B-d", "C-d"),
                Arrays.asList("D-a", "D-b", "D-c", "D-d")
        );

        leftStream.map(MockMapper.<Long, String>noOpKeyValueMapper())
                .leftJoin(rightStream.flatMap(MockMapper.<Long, String>noOpFlatKeyValueMapper())
                                .selectKey(MockMapper.<Long, String>selectKeyKeyValueMapper()),
                        valueJoiner, JoinWindows.of(10000)).to(outputTopic);

        runTest(expectedResult);
    }

    @Test
    public void testOuter() throws Exception {
        final List<List<String>> expectedResult = Arrays.asList(
                null,
                null,
                Collections.singletonList("A-null"),
                Collections.singletonList("A-a"),
                Collections.singletonList("B-a"),
                Arrays.asList("A-b", "B-b"),
                null,
                null,
                Arrays.asList("C-a", "C-b"),
                Arrays.asList("A-c", "B-c", "C-c"),
                null,
                null,
                null,
                Arrays.asList("A-d", "B-d", "C-d"),
                Arrays.asList("D-a", "D-b", "D-c", "D-d")
        );

        leftStream.outerJoin(rightStream, valueJoiner, JoinWindows.of(10000)).to(outputTopic);

        runTest(expectedResult);
    }

    @Test
    public void testOuterRepartitioned() throws Exception {
        final List<List<String>> expectedResult = Arrays.asList(
                null,
                null,
                Collections.singletonList("A-null"),
                Collections.singletonList("A-a"),
                Collections.singletonList("B-a"),
                Arrays.asList("A-b", "B-b"),
                null,
                null,
                Arrays.asList("C-a", "C-b"),
                Arrays.asList("A-c", "B-c", "C-c"),
                null,
                null,
                null,
                Arrays.asList("A-d", "B-d", "C-d"),
                Arrays.asList("D-a", "D-b", "D-c", "D-d")
        );

        leftStream.map(MockMapper.<Long, String>noOpKeyValueMapper())
                .outerJoin(rightStream.flatMap(MockMapper.<Long, String>noOpFlatKeyValueMapper())
                                .selectKey(MockMapper.<Long, String>selectKeyKeyValueMapper()),
                        valueJoiner, JoinWindows.of(10000)).to(outputTopic);

        runTest(expectedResult);
    }

    @Test
    public void testMultiInner() throws Exception {
        final List<List<String>> expectedResult = Arrays.asList(
                null,
                null,
                null,
                Collections.singletonList("A-a-a"),
                Collections.singletonList("B-a-a"),
                Arrays.asList("A-b-a", "B-b-a", "A-a-b", "B-a-b", "A-b-b", "B-b-b"),
                null,
                null,
                Arrays.asList("C-a-a", "C-a-b", "C-b-a", "C-b-b"),
                Arrays.asList("A-c-a", "A-c-b", "B-c-a", "B-c-b", "C-c-a", "C-c-b", "A-a-c", "B-a-c",
                        "A-b-c", "B-b-c", "C-a-c", "C-b-c", "A-c-c", "B-c-c", "C-c-c"),
                null,
                null,
                null,
                Arrays.asList("A-d-a", "A-d-b", "A-d-c", "B-d-a", "B-d-b", "B-d-c", "C-d-a", "C-d-b", "C-d-c",
                        "A-a-d", "B-a-d", "A-b-d", "B-b-d", "C-a-d", "C-b-d", "A-c-d", "B-c-d", "C-c-d",
                        "A-d-d", "B-d-d", "C-d-d"),
                Arrays.asList("D-a-a", "D-a-b", "D-a-c", "D-a-d", "D-b-a", "D-b-b", "D-b-c", "D-b-d", "D-c-a",
                        "D-c-b", "D-c-c", "D-c-d", "D-d-a", "D-d-b", "D-d-c", "D-d-d")
        );

        leftStream.join(rightStream, valueJoiner, JoinWindows.of(10000))
                .join(rightStream, valueJoiner, JoinWindows.of(10000)).to(outputTopic);

        runTest(expectedResult);
    }
}
