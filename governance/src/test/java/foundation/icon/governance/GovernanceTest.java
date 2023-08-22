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
import score.RevertedException;
import score.UserRevertedException;

import java.math.BigInteger;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GovernanceTest extends TestBase {
    private static final BigInteger ONE_HUNDRED = BigInteger.valueOf(100);
    private static final ServiceManager sm = getServiceManager();
    private static final Account owner = sm.createAccount(10000);
    private static final Account alice = sm.createAccount(1000);
    private static Score govScore;

    private static final Map<String, String> validProposals = Map.ofEntries(
            Map.entry("Text", "[{\"name\": \"text\", \"value\": {\"text\": \"test proposal\"}}]"),
            Map.entry("Revision", "[{\"name\": \"revision\", \"value\": {\"revision\": \"0x15\"}}]"),
            Map.entry("NetworkScoreDesignation", "[{\"name\": \"networkScoreDesignation\", \"value\":" +
                    "{\"networkScores\":[{\"role\":\"cps\",\"address\":\"cxdca1178010b5368aea929ad5c06abee64b91acc2\"}]}}]"),
            Map.entry("RewardFundAllocation", "[{\"name\": \"rewardFundsAllocation\", \"value\":" +
                    "{\"rewardFunds\":{\"iprep\":\"0xd\",\"icps\":\"0xa\",\"irelay\":\"0x0\",\"ivoter\":\"0x4d\"}}}]")
    );
    private static final Map<String, String> callProposals = Map.ofEntries(
            Map.entry("openBTPNetwork", "[{\"name\":\"call\",\"value\":{\"to\":\"cx0000000000000000000000000000000000000000\",\"method\":\"openBTPNetwork\",\"params\":[{\"type\":\"str\",\"value\":\"eth\"},{\"type\":\"str\",\"value\":\"hardhat-123\"},{\"type\":\"Address\",\"value\":\"cxf1b0808f09138fffdb890772315aeabb37072a8a\"}]}}]"),
            Map.entry("penalizeNonvoters", "[{\"name\":\"call\",\"value\":{\"to\":\"cx0000000000000000000000000000000000000000\",\"method\":\"penalizeNonvoters\",\"params\":[{\"type\":\"[]Address\",\"value\":[\"hx0000000000000000000000000000000000000100\",\"hx0000000000000000000000000000000000000101\"]}]}}]"),
            Map.entry("setDelegation", "[{\"name\":\"call\",\"value\":{\"to\":\"cx0000000000000000000000000000000000000000\",\"method\":\"setDelegation\",\"params\":[{\"fields\":{\"address\":\"Address\",\"value\":\"int\"},\"type\":\"[]struct\",\"value\":[{\"address\":\"hx0000000000000000000000000000000000000100\",\"value\":\"0x1000000000\"},{\"address\":\"hx0000000000000000000000000000000000000101\",\"value\":\"0x2000000000\"}]}]}}]"),
            Map.entry("setRewardFundAllocation", "[{\"name\":\"call\",\"value\":{\"to\":\"cx0000000000000000000000000000000000000000\",\"method\":\"setRewardFundAllocation\",\"params\":[{\"type\":\"int\",\"value\":\"0x10\"},{\"type\":\"int\",\"value\":\"0xa\"},{\"type\":\"int\",\"value\":\"0\"},{\"type\":\"int\",\"value\":\"0x4a\"}]}}]"),
            Map.entry("claimIScore", "[{\"name\":\"call\",\"value\":{\"to\":\"cx0000000000000000000000000000000000000000\",\"method\":\"claimIScore\",\"params\":[]}}]"),
            Map.entry("testParamCall", "[{\"name\":\"call\",\"value\":{\"to\":\"cx0000000000000000000000000000000000000000\",\"method\":\"testParamCall\",\"params\":[{\"type\":\"bool\",\"value\":\"0x1\"},{\"type\":\"bytes\",\"value\":\"0x1234567890abcdef\"}]}}]"),
            Map.entry("testStructCall", "[{\"name\":\"call\",\"value\":{\"to\":\"cx0000000000000000000000000000000000000000\",\"method\":\"testStructCall\",\"params\":[{\"fields\":{\"address\":\"Address\",\"value\":\"int\"},\"type\":\"struct\",\"value\":{\"address\":\"hx0000000000000000000000000000000000000100\",\"value\":\"0x1000000000\"}}]}}]"),
            Map.entry("testArrayCall", "[{\"name\":\"call\",\"value\":{\"to\":\"cx0000000000000000000000000000000000000000\",\"method\":\"testArrayCall\",\"params\":[{\"type\":\"[]str\",\"value\":[\"alice\",\"bob\"]},{\"type\":\"[]int\",\"value\":[\"0x1000000000\",\"0x2000000000\"]},{\"type\":\"[]bool\",\"value\":[\"0x0\",\"0x1\"]},{\"type\":\"[]bytes\",\"value\":[\"0x123456\",\"0xabcdef\"]}]}}]"),
            Map.entry("negativeTest", "[{\"name\":\"call\",\"value\":{\"to\":\"cx0000000000000000000000000000000000000000\",\"method\":\"testArrayCall\",\"params\":[{\"type\":\"[]String\",\"value\":[\"alice\",\"bob\"]},{\"type\":\"[]int\",\"value\":[\"0x1000000000\",\"0x2000000000\"]},{\"type\":\"[]bool\",\"value\":[\"0x0\",\"0x1\"]}]}}]")
    );

    @BeforeAll
    public static void setup() throws Exception {
        // install ChainScore mock
        sm.deploy(ChainScore.ADDRESS, owner, new ChainScore());
        // then deploy gov score
        govScore = sm.deploy(owner, Governance.class);
    }

    byte[] registerProposal(String key) {
        return registerProposal(key, validProposals);
    }

    byte[] registerCallProposal(String key) {
        return registerProposal(key, callProposals);
    }

    byte[] registerProposal(String key, Map<String, String> proposals) {
        System.out.println("[registerProposal] " + key + "=" + proposals.get(key));
        govScore.invoke(owner, ONE_HUNDRED.multiply(ICX),
                "registerProposal", key, "test proposal for " + key, proposals.get(key).getBytes());
        return sm.getBlock().hashOfTransactionAt(0);
    }

    @Test
    void registerProposal() {
        for (String key : validProposals.keySet()) {
            assertDoesNotThrow(() -> registerProposal(key));
        }
    }

    @Test
    void voteProposal() {
        // registerProposal first
        var id = registerProposal("Text");
        // 1st vote (success)
        assertDoesNotThrow(() ->
                govScore.invoke(owner, "voteProposal", id, 1));
        // 2nd vote from same prep (should revert)
        var reverted = assertThrows(UserRevertedException.class, () ->
                govScore.invoke(owner, "voteProposal", id, 1));
        assertTrue(reverted.getMessage().contains("Already voted"));

        // early applyProposal should fail
        assertThrows(UserRevertedException.class, () ->
                govScore.invoke(owner, "applyProposal", (Object) id));
        // 2nd vote with alice (success)
        assertDoesNotThrow(() ->
                govScore.invoke(alice, "voteProposal", id, 1));
        // applyProposal should success
        assertDoesNotThrow(() ->
                govScore.invoke(alice, "applyProposal", (Object) id));
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
    void callProposal() {
        for (String key : callProposals.keySet()) {
            if (key.startsWith("negative")) {
                assertThrows(RevertedException.class, () -> registerCallProposal(key));
            } else {
                var id = registerCallProposal(key);
                govScore.invoke(owner, "voteProposal", id, 1);
                govScore.invoke(alice, "voteProposal", id, 1);
                govScore.invoke(owner, "applyProposal", (Object) id);
            }
        }
    }
}
