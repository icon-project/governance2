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
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class GovernanceTest {

    @Test
    void validateProposal() {
        Governance gov = new Governance();
        String[] valid = new String[]{
                "[{\"name\": \"text\", \"value\": {\"text\": \"test proposal\"}}]",
                "[{\"name\": \"rewardFundsAllocation\", \"value\":" + "{\"rewardFunds\": {" +
                        "\"iprep\":\"0x10\", \"icps\":\"0xa\", \"irelay\":\"0xa\", \"ivoter\":\"0x40\"}}}]",
        };
        for (String test: valid) {
            JsonValue json = Json.parse(test);
            JsonArray values = json.asArray();
            gov.validateProposals(values);
        }

        String[] invalid = new String[]{
                "[{\"name\": \"rewardFundsAllocation\", \"value\":" + "{\"rewardFunds\": {\"iprep\": \"0x64\"}}}]",
                "[{\"name\": \"rewardFundsAllocation\", \"value\":" + "{\"rewardFunds\": {" +
                        "\"iprep\":\"0x10\", \"icps\":\"0x10\", \"irelay\":\"0xa\", \"ivoter\":\"0x40\"}}}]",
        };
        for (String test: invalid) {
            JsonValue json = Json.parse(test);
            JsonArray values = json.asArray();
            assertThrows(AssertionError.class, () -> gov.validateProposals(values));
        }
    }
}
