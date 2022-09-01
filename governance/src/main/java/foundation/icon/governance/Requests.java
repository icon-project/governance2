/*
 * Copyright 2022 ICON Foundation
 *
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

package foundation.icon.governance;

import score.*;
import scorex.util.ArrayList;

import java.util.List;
import java.util.Map;

public class Requests {
    private Request[] requests;

    public Requests(Request[] requests) {
        this.requests = requests;
    }

    public static void writeObject(ObjectWriter w, Requests c) {
        w.beginList(1);
        w.beginList(c.requests.length);
        for (Request r : c.requests) {
            w.write(r);
        }
        w.end();
        w.end();
    }

    public static Requests readObject(ObjectReader r) {
        r.beginList();
        Request[] requests;
        List<Request> requestList = new ArrayList<>();
        r.beginList();
        while(r.hasNext()) requestList.add(r.read(Request.class));
        r.end();
        var len = requestList.size();
        requests = new Request[len];
        for (int i = 0; i < len; i++) {
            requests[i] = requestList.get(i);
        }
        r.end();
        return new Requests(requests);
    }

    public List<Object> toList(){
        ArrayList<Map<String, Object>> callRequests = new ArrayList<>();
        for (Request request: requests) {
            callRequests.add(request.toMap());
        }
        return List.of(callRequests.toArray());
    }

    public void validateRequests() {
        for (Request cr : requests) cr.validateRequest();
    }

    public void handleRequests(Governance governance) {
        for (Request cr : requests) cr.handleRequest(governance);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter w = Context.newByteArrayObjectWriter("RLPn");
        writeObject(w, this);
        return w.toByteArray();
    }
}
