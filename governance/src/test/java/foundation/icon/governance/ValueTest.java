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

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ValueTest {

    @Test
    void fromJson() {
        Map<Integer, List<String>> values = Map.of(
                8, List.of("{\"rewardFunds\": {\"icps\": \"0xa\", \"iprep\": \"0xd\", \"irelay\": \"0x0\", \"ivoter\": \"0x4d\"}}"),
                7, List.of("{\"iglobal\": \"0x27b46536c66c8e3000000\"}"),
                6, List.of("{\"costs\": {\"apiCall\": \"0x2710\", \"contractCall\": \"0x61a8\", " +
                        "\"contractCreate\": \"0x3b9aca00\", \"contractDestruct\": \"0x0\", \"contractSet\": \"0x3a98\", " +
                        "\"contractUpdate\": \"0x3b9aca00\", \"default\": \"0x186a0\", \"delete\": \"-0xf0\", " +
                        "\"deleteBase\": \"0xc8\", \"eventLog\": \"0x0\", \"get\": \"0x19\", \"getBase\": \"0xbb8\", " +
                        "\"input\": \"0xc8\", \"log\": \"0x64\", \"logBase\": \"0x1388\", \"replace\": \"0x0\", " +
                        "\"schema\": \"0x1\", \"set\": \"0x140\", \"setBase\": \"0x2710\"}}"),
                5, List.of("{\"value\": \"10000000000000000000000\"}"),
                4, List.of("{\"value\": \"12500000000\"}"),
                3, List.of("{\"address\": \"hx5796d7fb3442759bee4aaeaa4e6618a034f630b1\"}"),
                2, List.of("{\"address\": \"cxed2bed52e86bc9334446cae7b3834a802cc6ae27\", \"type\": \"0x0\"}"),
                1, List.of("{\"code\": \"0x10\", \"name\": \"Revision16\"}",
                        "{\"code\": \"0xf\", \"name\": \"Revision15\"}",
                        "{\"code\": \"0xe\", \"name\": \"Revision14\"}",
                        "{\"code\": \"13\", \"name\": \"1.9.1\"}",
                        "{\"code\": \"9\", \"name\": \"1.7.3\"}"),
                0, List.of("{\"value\": \"<h1>IISS 3.0 Text Proposal</h1>\\n" +
                        "<p>The ICON Foundation is submitting this proposal to agree upon enhancements to IISS.</p>\"}")
        );

        for (var type : values.keySet()) {
            for (var value : values.get(type)) {
                JsonValue json = Json.parse(value);
                JsonObject jsonObj = json.asObject();
                assertDoesNotThrow(() -> Value.fromJson(type, jsonObj));
            }
        }
    }
}
