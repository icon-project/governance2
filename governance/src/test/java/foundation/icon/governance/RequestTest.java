/*
 * Copyright 2022 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the \"License\");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an \"AS IS\" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package foundation.icon.governance;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.iconloop.score.test.ManualRevertException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class RequestTest {
    @Test
    void initRequests() {
        String[] valid = new String[]{
                "{\"to\":\"cx0000000000000000000000000000000000000000\",\"method\":\"blockScore\",\"params\":[{\"type\":\"Address\",\"value\":\"cxaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}]}",
                "{\"to\":\"cx0000000000000000000000000000000000000002\",\"method\":\"method2\",\"params\":[{\"type\":\"str\",\"value\":\"Alice\"},{\"type\":\"struct\",\"value\":{\"field1\":\"Bob\",\"field2\":\"hxb6b5791be0b5ef67063b3c10b840fb81514db2fd\"},\"fields\":{\"field1\":\"str\",\"field2\":\"Address\"}}]}",
                "{\"to\":\"cx0000000000000000000000000000000000000000\",\"method\":\"method3\",\"params\":[{\"type\":\"[]struct\",\"value\":[{\"name\":\"bob\",\"address\":\"hx1111111111111111111111111111111111111111\"},{\"name\":\"alice\",\"address\":\"hx2111111111111111111111111111111111111111\"}],\"fields\":{\"name\":\"str\",\"address\":\"Address\"}}]}",
                "{\"to\":\"cx0000000000000000000000000000000000000000\",\"method\":\"setRewardFundAllocation\",\"params\":[{\"type\":\"int\",\"value\":\"0x19\"},{\"type\":\"int\",\"value\":\"0x19\"},{\"type\":\"int\",\"value\":\"0x19\"},{\"type\":\"int\",\"value\":\"0x19\"}]}"
        };
        for (String test: valid) {
            JsonValue json = Json.parse(test);
            JsonObject jsonObject = json.asObject();
            Request.fromJson(jsonObject);
        }

        String[] invalid = new String[]{
                "{\"to\":\"cx0000000000000000000000000000000000000000\",\"method\":\"blockScore\"}",
                "{\"method\":\"blockScore\",\"params\":[{\"type\":\"Address\",\"value\":\"cx0000000000000000000000000000000000000001\"}]}",
                "{\"to\":\"cx0000000000000000000000000000000000000000\",\"params\":[{\"type\":\"Address\",\"value\":\"cx0000000000000000000000000000000000000001\"}]}",
                "{\"to\":\"cx0000000000000000000000000000000000000002\",\"method\":\"method2\",\"params\":[{\"type\":\"str\",\"value\":\"Alice\"},{\"type\":\"struct\",\"value\":{\"field1\":\"Bob\",\"field2\":\"hxb6b5791be0b5ef67063b3c10b840fb81514db2fd\"}}]}",
                "{\"to\":\"cx0000000000000000000000000000000000000002\",\"method\":\"method2\",\"params\":[{\"type\":\"str\",\"value\":\"Alice\",\"fields\": {}}]}",
        };
        for (String test: invalid) {
            JsonValue json = Json.parse(test);
            JsonObject jsonObject = json.asObject();
            assertThrows(ManualRevertException.class, () -> Request.fromJson(jsonObject));
        }
    }
}
