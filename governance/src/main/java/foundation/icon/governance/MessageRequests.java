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

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import score.*;
import scorex.util.ArrayList;

import java.util.List;
import java.util.Map;

public class MessageRequests {
    private MessageRequest[] requests;

    public MessageRequests(MessageRequest[] requests) {
        this.requests = requests;
    }

    public static MessageRequests fromJson(JsonValue json) {
        JsonArray jsonArray = json.asArray();
        int length = jsonArray.size();
        MessageRequest[] messageRequests = new MessageRequest[length];
        for (int i = 0; i < length; i++) {
            var jsonValue = jsonArray.get(i);
            messageRequests[i] = MessageRequest.fromJson(jsonValue);
        }
        return new MessageRequests(messageRequests);
    }

    public static void writeObject(ObjectWriter w, MessageRequests m) {
        w.beginList(1);
        w.beginList(m.requests.length);
        for (MessageRequest r : m.requests) {
            w.write(r);
        }
        w.end();
        w.end();
    }

    public static MessageRequests readObject(ObjectReader r) {
        r.beginList();
        MessageRequest[] messageRequests;
        List<MessageRequest> messageList = new ArrayList<>();
        r.beginList();
        while(r.hasNext()) messageList.add(r.read(MessageRequest.class));
        r.end();
        var len = messageList.size();
        messageRequests = new MessageRequest[len];
        for (int i = 0; i < len; i++) {
            messageRequests[i] = messageList.get(i);
        }
        r.end();
        return new MessageRequests(messageRequests);
    }

    public List<Object> toList(){
        ArrayList<Map<String, Object>> messageRequests = new ArrayList<>();
        for (MessageRequest request: requests) {
            messageRequests.add(request.toMap());
        }
        return List.of(messageRequests.toArray());
    }

    public void validateRequests() {
        for (MessageRequest mr : requests) mr.validateRequest();
    }

    public void handleRequests() {
        for (MessageRequest mr : requests) mr.handleRequest();
    }
}

