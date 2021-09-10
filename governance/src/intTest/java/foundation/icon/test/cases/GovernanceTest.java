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
import foundation.icon.test.score.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class GovernanceTest extends TestBase {
    private static TransactionHandler txHandler;
    private static ChainScore chainScore;
    private static GovernanceScore governanceScore;
    private static HelloWorld helloWorldScore;
    private static KeyWallet[] wallets;
    private static Env.Chain chain = Env.getDefaultChain();
    private static Address cpsAddress;

    private static void stake(Wallet wallet, BigInteger amount) throws IOException, ResultTimeoutException {
        var result = chainScore.setStake(wallet, amount);
        var transactionResult = txHandler.getResult(result);
        assertSuccess(transactionResult);

    }

    private static void delegate(Wallet wallet, BigInteger amount) throws IOException, ResultTimeoutException {
        // set delegation
        Delegation[] delegations = new Delegation[]{
                new Delegation(wallet.getAddress(), amount.divide(BigInteger.TWO))};
        var result = chainScore.setDelegation(wallet, delegations);
        var transactionResult = txHandler.getResult(result);
        assertSuccess(transactionResult);
        transactionResult = txHandler.getResult(result);
        assertSuccess(transactionResult);
    }
    private static void bond(Wallet wallet, BigInteger amount) throws IOException, ResultTimeoutException {
        // set bond
        Address[] addresses = new Address[]{wallet.getAddress()};
        var result0 = chainScore.setBonderList(wallet, addresses);
        var transactionResult = txHandler.getResult(result0);
        assertSuccess(transactionResult);
        Delegation[] delegations = new Delegation[]{
                new Delegation(wallet.getAddress(), amount.divide(BigInteger.TWO))};
        var result1 = chainScore.setBond(wallet, delegations);
        transactionResult = txHandler.getResult(result1);

        assertSuccess(transactionResult);
    }

    @BeforeAll
    static void setup() throws Exception {
        chain = Env.getDefaultChain();
        IconService iconService = new IconService(new HttpProvider(chain.getEndpointURL(3)));
        txHandler = new TransactionHandler(iconService, chain);
        chainScore = new ChainScore(txHandler);
        var stakeAmount = ICX.multiply(BigInteger.valueOf(100000000));
        var delegationAmount = stakeAmount.divide(BigInteger.TWO);

        // register P-Rep
        var txHash = chainScore.registerPRep(
                chain.godWallet,
                "god",
                "god@god.com",
                "USA",
                "NY",
                "https://god.com",
                "https://god.com/god.json",
                "god.com:7100",
                chain.godWallet.getAddress());
       var transactionResult = txHandler.getResult(txHash);
       assertSuccess(transactionResult);

       stake(chain.godWallet, stakeAmount);
       delegate(chain.godWallet, stakeAmount);

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
        delegationAmount = stakeAmount.divide(BigInteger.TWO);

        // register P-Rep
        txHash = chainScore.registerPRep(
                wallets[0],
                "prep0",
                "prep0@prep.com",
                "USA",
                "NY",
                "https://prep.com",
                "https://prep.com/prep.json",
                "prep.com:7100",
                wallets[0].getAddress());
        transactionResult = txHandler.getResult(txHash);
        assertSuccess(transactionResult);

        stake(wallets[0], stakeAmount);
        delegate(wallets[0], delegationAmount);

        // update governance
        governanceScore = GovernanceScore.update(txHandler, chain.godWallet);
        //deploy SCORE for malicious proposal test
        helloWorldScore = HelloWorld.install(txHandler, chain.godWallet);
        txHandler.waitNextTerm();

        var scorePkg = "hello-world";
        var scorePath = Score.getFilePath(scorePkg);
        byte[] data = Files.readAllBytes(Path.of(scorePath));
        txHash = governanceScore.deploySample(chain.godWallet, data);
        transactionResult = txHandler.getResult(txHash);
        var eventlog = transactionResult.getEventLogs().get(0);
        var scoreAddress = eventlog.getIndexed().get(1);
        cpsAddress = scoreAddress.asAddress();
        System.out.println("@@@" + cpsAddress.toString());

        // setRevision ICON2
        BigInteger prevRevision = governanceScore.getRevision();

        BigInteger revisionProposalType = BigInteger.valueOf(1);
        String title = "revision proposal";
        String desc = "revision proposal";

        JsonObject jsonValue = new JsonObject();
        BigInteger newRevision = prevRevision.add(BigInteger.valueOf(1));
        jsonValue.add("value", "0x" + newRevision.toString(16));

        approveProposal(title, desc, revisionProposalType, jsonValue, true);
        var revisionResponse = governanceScore.getRevision();
        assert revisionResponse.compareTo(newRevision) == 0;

        bond(chain.godWallet, delegationAmount);
        txHandler.waitNextTerm();
    }

    @Test
    public void testText() throws IOException, ResultTimeoutException {
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
    public void testStepPrice() throws IOException, ResultTimeoutException {
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
    public void testIRep() throws IOException, ResultTimeoutException {
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
    public void testStepCosts() throws IOException, ResultTimeoutException {
        RpcObject prevCosts = chainScore.getStepCosts();
        var prevDefaultCost = prevCosts.getItem("default").asInteger();
        var prevCallCost = prevCosts.getItem("contractCall").asInteger();

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

    @Test
    public void testRewardFundProposal() throws IOException, ResultTimeoutException {
        RpcObject prevNetworkValue = chainScore.getNetworkValue();
        RpcObject prevRewardFund = prevNetworkValue.getItem("rewardFund").asObject();
        BigInteger prevIglobal = prevRewardFund.getItem("Iglobal").asInteger();
        BigInteger rewardFundProposalType = BigInteger.valueOf(7);
        String title = "rewardFund proposal";
        String desc = "rewardFund proposal";

        JsonObject jsonValue = new JsonObject();
        BigInteger newRewardFund = prevIglobal.multiply(BigInteger.valueOf(110)).divide(BigInteger.valueOf(100));
        jsonValue.add("value", "0x" + newRewardFund.toString(16));

        approveProposal(title, desc, rewardFundProposalType, jsonValue, true);
        var networkValue = chainScore.getNetworkValue();
        RpcObject rewardFund = networkValue.getItem("rewardFund").asObject();
        BigInteger iglobal = rewardFund.getItem("Iglobal").asInteger();
        assert iglobal.compareTo(newRewardFund) == 0;
    }

    @Test
    public void testRewardFundsRate() throws IOException, ResultTimeoutException {
        BigInteger rewardFundProposalType = BigInteger.valueOf(8);
        String title = "rewardFundRate proposal";
        String desc = "rewardFundRate proposal";

        JsonObject jsonValue = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        JsonObject prepMap = new JsonObject();
        JsonObject voterMap = new JsonObject();
        BigInteger iprep = BigInteger.valueOf(30);
        BigInteger ivoter = BigInteger.valueOf(70);
        prepMap.add("Iprep", "0x" + iprep.toString(16));
        voterMap.add("Ivoter", "0x" + ivoter.toString(16));
        jsonArray.add(prepMap);
        jsonArray.add(voterMap);
        jsonValue.add("rewardFunds", jsonArray);

        approveProposal(title, desc, rewardFundProposalType, jsonValue, true);
    }

    @Test
    public void testDesignateNetworkScore() throws IOException, ResultTimeoutException {
        BigInteger designationNetworkScore = BigInteger.valueOf(9);
        String title = "network score designate proposal";
        String desc = "designate network score proposal";

        JsonObject jsonValue = new JsonObject();
        jsonValue.add("role", "cps");
        jsonValue.add("address", cpsAddress.toString());

        approveProposal(title, desc, designationNetworkScore, jsonValue, true);
    }

    private static void cancelProposal(String title, String desc, BigInteger type, JsonObject jsonValue, boolean success) throws IOException, ResultTimeoutException {
        var proposalID = registerProposal(title, desc, type, jsonValue, success);
        if (!success) return;
        var txHash = governanceScore.cancelProposal(chain.godWallet, proposalID.toByteArray());
        var transactionResult = txHandler.getResult(txHash);
        assertSuccess(transactionResult);
        var proposal = governanceScore.getProposal(proposalID.toByteArray());
        assert proposal.getItem("status").asInteger().compareTo(BigInteger.valueOf(3)) == 0;
    }

    private static void approveProposal(String title, String desc, BigInteger type, JsonObject jsonValue, boolean success) throws IOException, ResultTimeoutException {
        var proposalID = registerProposal(title, desc, type, jsonValue, success);
        if (!success) return;
        var txHash = governanceScore.voteProposal(chain.godWallet, proposalID.toByteArray(), BigInteger.ONE);
        var transactionResult = txHandler.getResult(txHash);
        assertSuccess(transactionResult);
        var proposal = governanceScore.getProposal(proposalID.toByteArray());
        assert proposal.getItem("status").asInteger().compareTo(BigInteger.ONE) == 0;
    }

    private static void disapproveProposal(String title, String desc, BigInteger type, JsonObject jsonValue, boolean success) throws IOException, ResultTimeoutException {
        var proposalID = registerProposal(title, desc, type, jsonValue, success);
        if (!success) return;
        var txHash = governanceScore.voteProposal(chain.godWallet, proposalID.toByteArray(), BigInteger.ZERO);
        var transactionResult = txHandler.getResult(txHash);
        assertSuccess(transactionResult);
        var proposal = governanceScore.getProposal(proposalID.toByteArray());
        assert proposal.getItem("status").asInteger().compareTo(BigInteger.TWO) == 0;
    }

    private static Bytes registerProposal(
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
