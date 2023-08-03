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

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.annotations.Test;

public class SingleThreadExecutorPoolTest {

    @Test(timeOut = 10000)
    public void testRoundRobin() throws ExecutionException, InterruptedException {
        final SingleThreadExecutorPool pool = new SingleThreadExecutorPool("test-thread-name", 2);
        final List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            futures.add(pool.next().submit(() -> Thread.currentThread().getName()));
        }
        final Map<String, Integer> threadNameToCount = new HashMap<>();
        for (Future<String> future : futures) {
            final String name = future.get();
            threadNameToCount.putIfAbsent(name, 0);
            threadNameToCount.put(name, threadNameToCount.get(name) + 1);
        }
        assertEquals(threadNameToCount.keySet(), Sets.newHashSet("test-thread-name-0", "test-thread-name-1"));
        assertEquals(threadNameToCount.values(), Arrays.asList(5, 5)); // next() chooses thread using a round-robin way
    }

    @Test(timeOut = 10000)
    public void testClose() {
        final SingleThreadExecutorPool pool = new SingleThreadExecutorPool("test-close", 1);
        final ExecutorService executor = pool.next();
        final AtomicInteger numSkippedTasks = new AtomicInteger(0);
        for (int i = 0; i < 10; i++) {
            executor.execute(() -> {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    numSkippedTasks.incrementAndGet();
                }
            });
        }
        final long start = System.currentTimeMillis();
        pool.close();
        final long elapsed = System.currentTimeMillis() - start;
        System.out.println("pool.close() took " + elapsed + "ms");
        assertTrue(elapsed < 1000);
        assertEquals(numSkippedTasks.get(), 1);
    }
}
