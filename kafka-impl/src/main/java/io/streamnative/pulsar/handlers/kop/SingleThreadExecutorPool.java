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

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SingleThreadExecutorPool {

    private final AtomicInteger index = new AtomicInteger(0);
    private final ExecutorService[] executors;
    private final String name;

    public SingleThreadExecutorPool(final String name) {
        this(name, Runtime.getRuntime().availableProcessors());
    }

    @VisibleForTesting
    SingleThreadExecutorPool(final String name, final int size) {
        this.name = name;
        this.executors = new ExecutorService[size];
        for (int i = 0; i < executors.length; i++) {
            final String threadName = name + "-" + i;
            executors[i] = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(), r -> new Thread(null, r, threadName));
        }
    }

    public ExecutorService next() {
        return executors[index.getAndIncrement() % executors.length];
    }

    public void close() {
        for (int i = 0; i < executors.length; i++) {
            final List<Runnable> tasks = executors[i].shutdownNow();
            if (!tasks.isEmpty()) {
                log.info("Thread {}-{} cancelled {} tasks", name, i, tasks.size());
            }
        }
    }
}
