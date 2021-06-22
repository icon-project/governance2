package com.icon.governance;

import score.Address;
import score.Context;
import score.annotation.External;

import java.math.BigInteger;
import java.util.List;


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

//    @External(readonly = true)
//    public Map getProposal(byte[] id) {
//        return networkProposal.getProposal(id);
//    }

    @External
    public void registerProposal(
            String title,
            String description,
            int type,
            byte[] value
    ) {
        List<PRepInfo> prepsInfo = chainScore.getMainPRepsInfo();
        Address proposer = Context.getCaller();
//        if (!hasUsable(proposer, prepsInfo)) {
//            Context.revert("No Permission: only main prep");
//        }

        byte[] id = Context.getTransactionHash();
        networkProposal.submitProposal(
                id,
                proposer,
                title,
                description,
                type,
                value,
                prepsInfo,
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
