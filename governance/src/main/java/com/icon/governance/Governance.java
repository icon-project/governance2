package com.icon.governance;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;


import score.Address;
import score.Context;
import score.annotation.External;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;


public class Governance {
    private final ChainScore chainScore;
    private final NetworkProposal networkProposal;
    private final Event event;

    public Governance() {
        chainScore = new ChainScore();
        networkProposal = new NetworkProposal();
        event = new Event();
    }

    public void setRevision(int code) {
        chainScore.setRevision(code);
        event.RevisionChanged(code);
    }

    public void setStepPrice(BigInteger price) {
        chainScore.setStepPrice(price);
        event.StepPriceChanged(price);
    }

    public void setStepCost(String type, BigInteger cost) {
        chainScore.setStepCost(type, cost);
        event.StepCostChanged(type, cost);
    }

    public void acceptScore(byte[] txHash) {
        chainScore.acceptScore(txHash);
        event.Accepted(new String(txHash));
    }

    public void rejectScore(byte[] txHash) {
        chainScore.rejectScore(txHash);
//        event.Accepted();
    }

    @External(readonly = true)
    public Map<String, ?> getProposal(byte[] id) {
        Proposal p = networkProposal.getProposal(id);
        if (p == null) {
            return null;
        }
        return p.toMap();
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
        BigInteger blockHeight = BigInteger.valueOf(Context.getBlockHeight());
        networkProposal.registerProposal(
                id,
                blockHeight,
                proposer,
                title,
                description,
                proposalType,
                v,
                chainScore
        );
    }

    @External
    public void voteProposal(byte[] id, int vote) {
        Address sender = Context.getCaller();
        PRepInfo[] prepsInfo = chainScore.getMainPRepsInfo();
        BigInteger blockHeight = BigInteger.valueOf(Context.getBlockHeight());
        byte[] txHash = Context.getTransactionHash();
        BigInteger timestamp = BigInteger.valueOf(Context.getTransactionTimestamp());
        if (checkMainPRep(sender, prepsInfo))
            Context.revert("No permission - only for main prep");

        networkProposal.voteProposal(id, vote, blockHeight, txHash, timestamp, prepsInfo);

        event.NetworkProposalVoted(id, vote, sender);
    }

    @External
    public void cancelProposal(byte[] id) {
        PRepInfo[] prepsInfo = chainScore.getMainPRepsInfo();
        Address sender = Context.getCaller();
        BigInteger blockHeight = BigInteger.valueOf(Context.getBlockHeight());

        if (!checkMainPRep(sender, prepsInfo)) Context.revert("No Permission: only main prep");

        networkProposal.cancelProposal(id, sender, blockHeight);
        event.NetworkProposalCanceled(id);
    }

    private boolean checkMainPRep(Address proposer, PRepInfo[] prepsInfo) {
        for (PRepInfo prep : prepsInfo) {
            if (proposer.equals(prep.getAddress())) {
                return true;
            }
        }
        return false;
    }
}
