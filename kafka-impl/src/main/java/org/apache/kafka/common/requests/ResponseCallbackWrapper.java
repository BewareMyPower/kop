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
package org.apache.kafka.common.requests;

import io.netty.util.Recycler;
import io.netty.util.Recycler.Handle;
import java.util.Map;
import org.apache.kafka.common.protocol.Errors;
import org.apache.kafka.common.protocol.types.Struct;

/**
 * A wrapper for {@link org.apache.kafka.common.requests.AbstractResponse} that
 * can perform some cleanup tasks after writing to the channel.
 */
public class ResponseCallbackWrapper extends AbstractResponse {

    private final Handle<ResponseCallbackWrapper> recyclerHandle;
    private AbstractResponse abstractResponse;
    private ResponseCallback responseCallback;

    private static final Recycler<ResponseCallbackWrapper> RECYCLER = new Recycler<ResponseCallbackWrapper>() {
        @Override
        protected ResponseCallbackWrapper newObject(Handle<ResponseCallbackWrapper> handle) {
            return new ResponseCallbackWrapper(handle);
        }
    };

    private ResponseCallbackWrapper(Handle<ResponseCallbackWrapper> recyclerHandle) {
        this.recyclerHandle = recyclerHandle;
    }

    public static ResponseCallbackWrapper get(AbstractResponse abstractResponse, ResponseCallback responseCallback) {
        ResponseCallbackWrapper responseCallbackWrapper = RECYCLER.get();
        responseCallbackWrapper.abstractResponse = abstractResponse;
        responseCallbackWrapper.responseCallback = responseCallback;
        return responseCallbackWrapper;
    }

    @Override
    public Map<Errors, Integer> errorCounts() {
        return abstractResponse.errorCounts();
    }

    @Override
    protected Struct toStruct(short i) {
        return abstractResponse.toStruct(i);
    }

    public void responseComplete() {
        responseCallback.responseComplete();
        recycle();
    }

    private void recycle() {
        abstractResponse = null;
        responseCallback = null;
        recyclerHandle.recycle(this);
    }
}
