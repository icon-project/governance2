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
import com.iconloop.score.test.Account;
import com.iconloop.score.test.ManualRevertException;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import foundation.icon.governance.mock.ChainScore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GovernanceTest extends TestBase {
    private static final BigInteger ONE_HUNDRED = BigInteger.valueOf(100);
    private static final ServiceManager sm = getServiceManager();
    private static final Account owner = sm.createAccount(1000);
    private static Score govScore;

    private static final Map<String, String> validProposals = Map.ofEntries(
            Map.entry("Text", "[{\"name\": \"text\", \"value\": {\"text\": \"test proposal\"}}]"),
            Map.entry("Revision", "[{\"name\": \"revision\", \"value\": {\"revision\": \"0x15\"}}]"),
            Map.entry("NetworkScoreDesignation", "[{\"name\": \"networkScoreDesignation\", \"value\":" +
                    "{\"networkScores\":[{\"role\":\"cps\",\"address\":\"cxdca1178010b5368aea929ad5c06abee64b91acc2\"}]}}]"),
            Map.entry("RewardFundAllocation", "[{\"name\": \"rewardFundsAllocation\", \"value\":" +
                    "{\"rewardFunds\":{\"iprep\":\"0xd\",\"icps\":\"0xa\",\"irelay\":\"0x0\",\"ivoter\":\"0x4d\"}}}]")
    );

    @BeforeAll
    public static void setup() throws Exception {
        // install ChainScore mock
        sm.deploy(ChainScore.ADDRESS, owner, new ChainScore());
        // then deploy gov score
        govScore = sm.deploy(owner, Governance.class);
    }

    byte[] registerProposal(String key) {
        System.out.println("[registerProposal] " + key + "=" + validProposals.get(key));
        govScore.invoke(owner, ONE_HUNDRED.multiply(ICX),
                "registerProposal", key, "test proposal for " + key, validProposals.get(key).getBytes());
        return sm.getBlock().hashOfTransactionAt(0);
    }

    @Test
    void registerProposal() {
        for (String key : validProposals.keySet()) {
            assertDoesNotThrow(() -> registerProposal(key));
        }
    }

    @Test
    void validateProposalsNegative() {
        String[] invalid = new String[]{
                "[{\"name\": \"rewardFundsAllocation\", \"value\":" + "{\"rewardFunds\": {\"iprep\": \"0x64\"}}}]",
                "[{\"name\": \"rewardFundsAllocation\", \"value\":" + "{\"rewardFunds\": {" +
                        "\"iprep\":\"0x10\", \"icps\":\"0x10\", \"irelay\":\"0xa\", \"ivoter\":\"0x40\"}}}]",
                "[{\"name\": \"text\", \"value\": {\"text\": \"test proposal\"}, \"invalidKey\": \"invalid value\"}]",
        };
        Governance gov = new Governance();
        for (String test: invalid) {
            JsonValue json = Json.parse(test);
            JsonArray values = json.asArray();
            assertThrows(ManualRevertException.class, () -> gov.validateProposals(values));
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
