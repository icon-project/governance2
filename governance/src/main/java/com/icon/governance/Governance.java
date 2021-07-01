package com.icon.governance;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;


import score.Address;
import score.Context;
import score.DictDB;
import score.annotation.External;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;


public class Governance {
    private final ChainScore chainScore;
    private final NetworkProposal networkProposal;
    private final Event event;

    public Governance(String name) {
        chainScore = new ChainScore();
        networkProposal = new NetworkProposal();
        event = new Event();
    }

    @External
    public void setRevision(int code) {
        chainScore.setRevision(code);
        event.RevisionChanged(code);
    }

    @External
    public void setStepPrice(BigInteger price) {
        chainScore.setStepPrice(price);
        event.StepPriceChanged(price);
    }

    @External
    public void setStepCost(String type, BigInteger cost) {
        chainScore.setStepCost(type, cost);
        event.StepCostChanged(type, cost);
    }

    @External
    public void acceptScore(byte[] txHash) {
        chainScore.acceptScore(txHash);
        event.Accepted(new String(txHash));
    }

    @External
    public void rejectScore(byte[] txHash) {
        chainScore.rejectScore(txHash);
//        event.Accepted();
    }

    @External(readonly = true)
    public Map<String, ?> getProposal(String hexString) {
        byte[] id = Convert.hexToBytes(hexString);
        this.networkProposal.getProposal(id);
        return Map.of();
    }


    @External
    public void registerProposal(
            String title,
            String description,
            String type,
            String value
    ) {
        Address proposer = Context.getCaller();
//        if (!hasUsable(proposer, prepsInfo)) {
//            Context.revert("No Permission: only main prep");
//        }

        byte[] id = Context.getTransactionHash();
        int proposalType = Convert.hexToInt(type);

        String stringValue = new String(Convert.hexToBytes(value));
        JsonValue json = Json.parse(stringValue);
        Value v = Value.makeWithJson(proposalType, json.asObject());

        networkProposal.submitProposal(
                id,
                proposer,
                title,
                description,
                proposalType,
                v,
                chainScore
        );
    }

    private boolean hasUsable(Address proposer, List<PRepInfo> prepsInfo) {
        for (PRepInfo prep : prepsInfo) {
            if (proposer.equals(prep.getAddress())) {
                return true;
            }
        }
        return false;
    }
}
