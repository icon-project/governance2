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
import score.UserRevertedException;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

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
                "[{\"name\": \"text\", \"value\": {\"text\": \"test proposal\"}, \"invalidKey\": \"invalid value\"}]",
        };
        for (String test: invalid) {
            JsonValue json = Json.parse(test);
            JsonArray values = json.asArray();
            assertThrows(UserRevertedException.class, () -> gov.validateProposals(values));
        }
    }

    @Test
    void addProposalId() {
        var pids = new Governance.TimerInfo.ProposalIds();
        var ti = new Governance.TimerInfo(pids);

        var b1 = "1".getBytes();
        ti.addProposalId(b1);
        var contains = Arrays.asList(ti.proposalIds.ids).contains(b1);
        assertTrue(contains);

        var b2 = "2".getBytes();
        ti.addProposalId(b2);
        contains = Arrays.asList(ti.proposalIds.ids).contains(b2);
        assertTrue(contains);

        var b3 = "3".getBytes();
        contains = Arrays.asList(ti.proposalIds.ids).contains(b3);
        assertFalse(contains);
    }

    @Test
    void removeProposalId() {
        var pids = new Governance.TimerInfo.ProposalIds();
        var ti = new Governance.TimerInfo(pids);

        var b1 = "1".getBytes();
        var b2 = "2".getBytes();
        var b3 = "3".getBytes();

        ti.addProposalId(b1);
        ti.addProposalId(b2);
        ti.addProposalId(b3);

        assertTrue(Arrays.asList(ti.proposalIds.ids).contains(b1));
        assertTrue(Arrays.asList(ti.proposalIds.ids).contains(b2));
        assertTrue(Arrays.asList(ti.proposalIds.ids).contains(b3));

        ti.removeProposalId(b2);

        assertTrue(Arrays.asList(ti.proposalIds.ids).contains(b1));
        assertFalse(Arrays.asList(ti.proposalIds.ids).contains(b2));
        assertTrue(Arrays.asList(ti.proposalIds.ids).contains(b3));

        ti.removeProposalId(b3);

        assertTrue(Arrays.asList(ti.proposalIds.ids).contains(b1));
        assertFalse(Arrays.asList(ti.proposalIds.ids).contains(b3));

        ti.removeProposalId(b1);

        assertFalse(Arrays.asList(ti.proposalIds.ids).contains(b1));
    }
}
