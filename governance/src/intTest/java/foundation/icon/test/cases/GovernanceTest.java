/*
 * Copyright 2020 ICONLOOP Inc.
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

package foundation.icon.test.cases;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.Wallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.Bytes;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.test.*;
import foundation.icon.test.score.ChainScore;
import foundation.icon.test.score.Delegation;
import foundation.icon.test.score.GovernanceScore;
import foundation.icon.test.score.HelloWorld;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class GovernanceTest extends TestBase {
    private static TransactionHandler txHandler;
    private static ChainScore chainScore;
    private static GovernanceScore governanceScore;
    private static HelloWorld helloWorldScore;
    private static KeyWallet[] wallets;
    private static Env.Chain chain = Env.getDefaultChain();

    private static void bond(Wallet wallet, BigInteger amount) throws IOException, ResultTimeoutException {
        //set Stake
        var result0 = chainScore.setStake(wallet, amount);

        // set delegation
        Delegation[] delegations = new Delegation[]{
                new Delegation(wallet.getAddress(), amount.divide(BigInteger.TWO))};
        var result1 = chainScore.setDelegation(wallet, delegations);
        var transactionResult = txHandler.getResult(result0);
        assertSuccess(transactionResult);
        transactionResult = txHandler.getResult(result1);
        assertSuccess(transactionResult);

        // set bond
        Address[] addresses = new Address[]{wallet.getAddress()};
        var result2 = chainScore.setBonderList(wallet, addresses);
        transactionResult = txHandler.getResult(result2);
        assertSuccess(transactionResult);
        var result3 = chainScore.setBond(wallet, delegations);
        transactionResult = txHandler.getResult(result3);

        assertSuccess(transactionResult);

    }

    @BeforeAll
    static void setup() throws Exception {
        chain = Env.getDefaultChain();
        IconService iconService = new IconService(new HttpProvider(chain.getEndpointURL(3)));
        txHandler = new TransactionHandler(iconService, chain);
        chainScore = new ChainScore(txHandler);
        var stakeAmount = ICX.multiply(BigInteger.valueOf(100000000));

        // register P-Rep
        var result = chainScore.registerPRep(
                chain.godWallet,
                "god",
                "god@god.com",
                "USA",
                "NY",
                "https://god.com",
                "https://god.com/god.json",
                "god.com:7100",
                chain.godWallet.getAddress());
       var transactionResult = txHandler.getResult(result);
       assertSuccess(transactionResult);

       bond(chain.godWallet, stakeAmount);

        // init wallets
        wallets = new KeyWallet[2];
        BigInteger amount = ICX.multiply(BigInteger.valueOf(3000));
        for (int i = 0; i < wallets.length; i++) {
            wallets[i] = KeyWallet.create();
            txHandler.transfer(wallets[i].getAddress(), amount);
        }
        for (KeyWallet wallet : wallets) {
            ensureIcxBalance(txHandler, wallet.getAddress(), BigInteger.ZERO, amount);
        }
        stakeAmount = ICX.multiply(BigInteger.valueOf(500));

        // register P-Rep
        result = chainScore.registerPRep(
                wallets[0],
                "prep0",
                "prep0@prep.com",
                "USA",
                "NY",
                "https://prep.com",
                "https://prep.com/prep.json",
                "prep.com:7100",
                wallets[0].getAddress());
        transactionResult = txHandler.getResult(result);
        assertSuccess(transactionResult);

        bond(wallets[0], stakeAmount);

        // update governance
        governanceScore = GovernanceScore.update(txHandler, chain.godWallet);
        //deploy SCORE for malicious proposal test
        helloWorldScore = HelloWorld.install(txHandler, chain.godWallet);
        txHandler.waitNextTerm();
    }

    @Test
    public void testTextProposal() throws IOException, ResultTimeoutException {
        BigInteger textProposalType = BigInteger.valueOf(0);
        String title = "test network proposal";
        String desc = "test network proposal description";
        String valueString = "test";
        JsonObject jsonValue = new JsonObject();
        jsonValue.add("value", valueString);
        //test cancel proposal
        cancelProposal(title, desc, textProposalType, jsonValue, true);

        //test disapprove proposal
        disapproveProposal(title, desc, textProposalType, jsonValue, true);

        //test approve proposal
        approveProposal(title, desc, textProposalType, jsonValue, true);
    }

    @Test
    public void testStepPriceProposal() throws IOException, ResultTimeoutException {
        BigInteger prevPrice = chainScore.getStepPrice();
        BigInteger priceProposalType = BigInteger.valueOf(4);
        String title = "stepPrice proposal";
        String desc = "step proposal";

        JsonObject jsonValue = new JsonObject();
        BigInteger newPrice = prevPrice.multiply(BigInteger.valueOf(120)).divide(BigInteger.valueOf(100));
        jsonValue.add("value", "0x" + newPrice.toString(16));

        JsonObject invalidValue = new JsonObject();
        BigInteger invalidPrice = prevPrice.multiply(BigInteger.valueOf(140)).divide(BigInteger.valueOf(100));
        invalidValue.add("value", "0x" + invalidPrice.toString(16));

        JsonObject invalidValue2 = new JsonObject();
        BigInteger invalidPrice2 = prevPrice.multiply(BigInteger.valueOf(60)).divide(BigInteger.valueOf(100));
        invalidValue2.add("value", "0x" + invalidPrice2.toString(16));

        //test cancel proposal
        cancelProposal(title, desc, priceProposalType, jsonValue, true);
        BigInteger priceResponse = chainScore.getStepPrice();
        assert priceResponse.compareTo(newPrice) != 0;

        //test disapprove proposal
        disapproveProposal(title, desc, priceProposalType, jsonValue, true);
        priceResponse = chainScore.getStepPrice();
        assert priceResponse.compareTo(newPrice) != 0;

        //test approve proposal - invalid case 1
        approveProposal(title, desc, priceProposalType, invalidValue, false);
        priceResponse = chainScore.getStepPrice();
        assert priceResponse.compareTo(invalidPrice) != 0;

        //test approve proposal - invalid case 2
        approveProposal(title, desc, priceProposalType, invalidValue2, false);
        priceResponse = chainScore.getStepPrice();
        assert priceResponse.compareTo(invalidPrice2) != 0;

        //test approve proposal
        approveProposal(title, desc, priceProposalType, jsonValue, true);
        priceResponse = chainScore.getStepPrice();
        assert priceResponse.compareTo(newPrice) == 0;
    }

    @Test
    public void testMaliciousScoreForGovernance() throws IOException, ResultTimeoutException {
        BigInteger freeze = BigInteger.ZERO;
        RpcObject governanceStatus = chainScore.getScoreStatus(Constants.GOVERNANCE_ADDRESS);
        checkScoreStatus(governanceStatus, false);

        // invalid proposal - try to freeze governance
        JsonObject jsonValue = new JsonObject();
        jsonValue.add("address", Constants.GOVERNANCE_ADDRESS.toString());
        jsonValue.add("type", "0x" + freeze.toString(16));
        approveProposal("fail case", "can not freeze governance", BigInteger.TWO, jsonValue, false);

        governanceStatus = chainScore.getScoreStatus(Constants.GOVERNANCE_ADDRESS);
        checkScoreStatus(governanceStatus, false);
    }

    @Test
    public void testMaliciousScore() throws IOException, ResultTimeoutException {
        BigInteger maliciousScoreProposalType = BigInteger.TWO;
        BigInteger freeze = BigInteger.ZERO;
        BigInteger unfreeze = BigInteger.ONE;
        RpcObject helloStatus = chainScore.getScoreStatus(helloWorldScore.getAddress());
        checkScoreStatus(helloStatus, false);

        //freeze score
        JsonObject jsonValue = new JsonObject();
        jsonValue.add("address", helloWorldScore.getAddress().toString());
        jsonValue.add("type", "0x" + freeze.toString(16));
        approveProposal("malicious score", "success", maliciousScoreProposalType, jsonValue, true);

        helloStatus = chainScore.getScoreStatus(helloWorldScore.getAddress());
        checkScoreStatus(helloStatus, true);

        //unfreeze score
        jsonValue = new JsonObject();
        jsonValue.add("address", helloWorldScore.getAddress().toString());
        jsonValue.add("type", "0x" + unfreeze.toString(16));
        approveProposal("unfreeze score", "success", maliciousScoreProposalType, jsonValue, true);

        helloStatus = chainScore.getScoreStatus(helloWorldScore.getAddress());
        checkScoreStatus(helloStatus, false);
    }

    private void checkScoreStatus(RpcObject scoreStatus, boolean blockedExpected) {
        boolean blocked = scoreStatus.getItem("blocked").asBoolean();
        assert blocked == blockedExpected;
    }

    @Test
    public void testDisqualifyPRep () throws IOException, ResultTimeoutException {
        BigInteger disqualifyPRepType = BigInteger.valueOf(3);

        // disqualify Account not prep
        JsonObject jsonValue = new JsonObject();
        jsonValue.add("address", wallets[1].getAddress().toString());
        approveProposal("disqualify prep", "fail", disqualifyPRepType, jsonValue, false);

        // disqualify prep Account
        jsonValue = new JsonObject();
        jsonValue.add("address", wallets[0].getAddress().toString());
        approveProposal("disqualify prep", "success", disqualifyPRepType, jsonValue, true);
    }

    @Test
    public void testIRepProposal() throws IOException, ResultTimeoutException {
        BigInteger prevIRep = chainScore.getIRep();
        BigInteger irepProposalType = BigInteger.valueOf(5);
        String title = "irep proposal";
        String desc = "irep proposal";

        JsonObject jsonValue = new JsonObject();
        BigInteger newIRep = prevIRep.multiply(BigInteger.valueOf(110)).divide(BigInteger.valueOf(100));
        jsonValue.add("value", "0x" + newIRep.toString(16));

        JsonObject invalidValue = new JsonObject();
        BigInteger invalidIRep = prevIRep.multiply(BigInteger.valueOf(140)).divide(BigInteger.valueOf(100));
        invalidValue.add("value", "0x" + invalidIRep.toString(16));

        JsonObject invalidValue2 = new JsonObject();
        BigInteger invalidIRep2 = prevIRep.multiply(BigInteger.valueOf(60)).divide(BigInteger.valueOf(100));
        invalidValue2.add("value", "0x" + invalidIRep2.toString(16));

        //test cancel proposal
        cancelProposal(title, desc, irepProposalType, jsonValue, true);
        BigInteger irepResponse = chainScore.getIRep();
        assert irepResponse.compareTo(newIRep) != 0;

        //test disapprove proposal
        disapproveProposal(title, desc, irepProposalType, jsonValue, true);
        irepResponse = chainScore.getIRep();
        assert irepResponse.compareTo(newIRep) != 0;

        //test approve proposal - invalid case 1
        approveProposal(title, desc, irepProposalType, invalidValue, false);
        irepResponse = chainScore.getIRep();
        assert irepResponse.compareTo(invalidIRep) != 0;

        //test approve proposal - invalid case 2
        approveProposal(title, desc, irepProposalType, invalidValue2, false);
        irepResponse = chainScore.getIRep();
        assert irepResponse.compareTo(invalidIRep2) != 0;

        //test approve proposal
        approveProposal(title, desc, irepProposalType, jsonValue, true);
        irepResponse = chainScore.getIRep();
        assert irepResponse.compareTo(newIRep) == 0;
    }

    @Test
    public void testRevisionProposal() throws IOException, ResultTimeoutException {
        BigInteger prevRevision = chainScore.getRevision();
        BigInteger revisionProposalType = BigInteger.valueOf(1);
        String title = "revision proposal";
        String desc = "revision proposal";

        JsonObject jsonValue = new JsonObject();
        BigInteger newRevision = prevRevision.add(BigInteger.ONE);
        jsonValue.add("value", "0x" + newRevision.toString(16));

        JsonObject invalidValue = new JsonObject();
        BigInteger invalidRevision = prevRevision.subtract(BigInteger.ONE);
        invalidValue.add("value", "0x" + invalidRevision.toString(16));

        //test cancel proposal
        cancelProposal(title, desc, revisionProposalType, jsonValue, true);
        BigInteger revisionResponse = chainScore.getRevision();
        assert revisionResponse.compareTo(newRevision) != 0;

        //test disapprove proposal
        disapproveProposal(title, desc, revisionProposalType, jsonValue, true);
        revisionResponse = chainScore.getRevision();
        assert revisionResponse.compareTo(newRevision) != 0;

        //test approve proposal - invalid case 1
        approveProposal(title, desc, revisionProposalType, invalidValue, false);
        revisionResponse = chainScore.getRevision();
        assert revisionResponse.compareTo(invalidRevision) != 0;

        //test approve proposal
        approveProposal(title, desc, revisionProposalType, jsonValue, true);
        revisionResponse = chainScore.getRevision();
        assert revisionResponse.compareTo(newRevision) == 0;
    }

    @Test
    public void testStepCostsProposal() throws IOException, ResultTimeoutException {
        RpcObject prevCosts = chainScore.getStepCosts();
        var prevDefaultCost = prevCosts.getItem("default").asInteger();
        var prevCallCost = prevCosts.getItem("contractCall").asInteger();
        System.out.println("@@@" + prevDefaultCost);
        System.out.println("@@@" + prevCallCost);

        BigInteger stepCostProposalType = BigInteger.valueOf(6);
        String title = "stepCost proposal";
        String desc = "stepCost proposal";

        JsonObject jsonValue = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        JsonObject defaultMap = new JsonObject();
        BigInteger newDefault = BigInteger.ONE;
        defaultMap.add("default", "0x" + newDefault.toString(16));
        jsonArray.add(defaultMap);
        jsonValue.add("costs", jsonArray);

        approveProposal(title, desc, stepCostProposalType, jsonValue, true);

        RpcObject costs = chainScore.getStepCosts();
        var defaultCost = costs.getItem("default").asInteger();
        var callCost = costs.getItem("contractCall").asInteger();

    }

    private void cancelProposal(String title, String desc, BigInteger type, JsonObject jsonValue, boolean success) throws IOException, ResultTimeoutException {
        var proposalID = registerProposal(title, desc, type, jsonValue, success);
        if (!success) return;
        var txHash = governanceScore.cancelProposal(chain.godWallet, proposalID.toByteArray());
        var transactionResult = txHandler.getResult(txHash);
        assertSuccess(transactionResult);
        var proposal = governanceScore.getProposal(proposalID.toByteArray());
        assert proposal.getItem("status").asInteger().compareTo(BigInteger.valueOf(3)) == 0;
    }

    private void approveProposal(String title, String desc, BigInteger type, JsonObject jsonValue, boolean success) throws IOException, ResultTimeoutException {
        var proposalID = registerProposal(title, desc, type, jsonValue, success);
        if (!success) return;
        var txHash = governanceScore.voteProposal(chain.godWallet, proposalID.toByteArray(), BigInteger.ONE);
        var transactionResult = txHandler.getResult(txHash);
        assertSuccess(transactionResult);
        var proposal = governanceScore.getProposal(proposalID.toByteArray());
        assert proposal.getItem("status").asInteger().compareTo(BigInteger.ONE) == 0;
    }

    private void disapproveProposal(String title, String desc, BigInteger type, JsonObject jsonValue, boolean success) throws IOException, ResultTimeoutException {
        var proposalID = registerProposal(title, desc, type, jsonValue, success);
        if (!success) return;
        var txHash = governanceScore.voteProposal(chain.godWallet, proposalID.toByteArray(), BigInteger.ZERO);
        var transactionResult = txHandler.getResult(txHash);
        assertSuccess(transactionResult);
        var proposal = governanceScore.getProposal(proposalID.toByteArray());
        assert proposal.getItem("status").asInteger().compareTo(BigInteger.TWO) == 0;
    }

    private Bytes registerProposal(
            String title,
            String desc,
            BigInteger type,
            JsonObject jsonValue,
            boolean success
    ) throws IOException, ResultTimeoutException {
        var value = jsonValue.toString().getBytes(StandardCharsets.UTF_8);
        var proposalID = governanceScore.registerProposal(chain.godWallet, title, desc, type, value);
        var transactionResult = txHandler.getResult(proposalID);
        if (!success) {
            assertFailure(transactionResult);
            return null;
        }
        assertSuccess(transactionResult);

        var proposal = governanceScore.getProposal(proposalID.toByteArray());
        assert title.equals(proposal.getItem("title").asString());
        assert desc.equals(proposal.getItem("description").asString());
        RpcObject retProposalValue = proposal.getItem("value").asObject();

//        for (String key : jsonValue.names()) {
//            var val = jsonValue.get(key).asString();
//            assert val.equals(retProposalValue.getItem(key).asString());
//        }
        assert desc.equals(proposal.getItem("description").asString());
        assert proposal.getItem("status").asInteger().compareTo(BigInteger.ZERO) == 0;

        var agree = proposal.getItem("vote").asObject().getItem("agree").asObject();
        var disagree = proposal.getItem("vote").asObject().getItem("disagree").asObject();
        var noVote = proposal.getItem("vote").asObject().getItem("noVote").asObject();
        var totalBondedDelegation = proposal.getItem("total_bonded_delegation").asInteger();

        assert agree.getItem("list").asArray().isEmpty();
        assert disagree.getItem("list").asArray().isEmpty();
        assert noVote.getItem("list").asArray().size() == 1;
        assert agree.getItem("amount").asInteger().compareTo(BigInteger.ZERO) == 0;
        assert disagree.getItem("amount").asInteger().compareTo(BigInteger.ZERO) == 0;
        assert noVote.getItem("amount").asInteger().compareTo(totalBondedDelegation) == 0;
        return proposalID;
    }
}
