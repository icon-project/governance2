package com.icon.governance;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;
import score.Address;
import score.Context;
import score.annotation.EventLog;
import score.annotation.External;

import java.math.BigInteger;
import java.util.Map;


public class Governance {
    private final ChainScore chainScore;
    private final NetworkProposal networkProposal;

    public Governance() {
        chainScore = new ChainScore();
        networkProposal = new NetworkProposal();
    }

    public void setRevision(int code) {
        chainScore.setRevision(code);
        RevisionChanged(code);
    }

    public void setStepPrice(BigInteger price) {
        chainScore.setStepPrice(price);
        StepPriceChanged(price);
    }

    public void setStepCost(String type, BigInteger cost) {
        chainScore.setStepCost(type, cost);
        StepCostChanged(type, cost);
    }

    public void acceptScore(byte[] txHash) {
        chainScore.acceptScore(txHash);
        Accepted(new String(txHash));
    }

    public void rejectScore(byte[] txHash) {
        chainScore.rejectScore(txHash);
//        Accepted();
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
            int type,
            byte[] value
    ) {
        Address proposer = Context.getCaller();
        PRepInfo[] prepsInfo = chainScore.getMainPRepsInfo();
        var prep = getPRepInfo(proposer, prepsInfo);
        if (prep == null)
            Context.revert("No permission - only for main prep");

        String stringValue = new String(value);
        JsonValue json = Json.parse(stringValue);
        Value v = Value.makeWithJson(type, json.asObject());
        BigInteger blockHeight = BigInteger.valueOf(Context.getBlockHeight());
        var term = chainScore.getPRepTerm();
        networkProposal.registerProposal(
                title,
                description,
                type,
                v,
                prepsInfo,
                term
        );
        NetworkProposalRegistered(title, description, type, value, proposer);
    }

    @External
    public void voteProposal(byte[] id, int vote) {
        Address sender = Context.getCaller();
        PRepInfo[] prepsInfo = chainScore.getMainPRepsInfo();
        var prep = getPRepInfo(sender, prepsInfo);
        if (prep == null)
            Context.revert("No permission - only for main prep");

        var status = networkProposal.voteProposal(id, vote, prep, prepsInfo);
        NetworkProposalVoted(id, vote, sender);

        if (status == NetworkProposal.APPROVED_STATUS) {
            NetworkProposalApproved(id);
        } else if (status == NetworkProposal.DISAPPROVED_STATUS) {
            NetworkProposalDisapproved(id);
        }
    }

    @External
    public void cancelProposal(byte[] id) {
        PRepInfo[] prepsInfo = chainScore.getMainPRepsInfo();
        Address sender = Context.getCaller();

        if (getPRepInfo(sender, prepsInfo) == null) Context.revert("No Permission: only main prep");

        networkProposal.cancelProposal(id, sender);
        NetworkProposalCanceled(id);
    }

    private PRepInfo getPRepInfo(Address address, PRepInfo[] prepsInfo) {
        for (PRepInfo prep : prepsInfo) {
            if (address.equals(prep.getAddress())) {
                return prep;
            }
        }
        return null;
    }

    /*
    * Events
    */
    @EventLog(indexed=1)
    public void Accepted(String txHash) {}

    @EventLog(indexed=1)
    public void Rejected(String txHash, String reason) {}

    @EventLog(indexed=1)
    public void StepPriceChanged(BigInteger stepPrice) {}

    @EventLog(indexed=1)
    public void StepCostChanged(String type, BigInteger stepCost) {}

    @EventLog(indexed=0)
    public void RevisionChanged(int revisionCode) {}

    @EventLog(indexed=0)
    public void MaliciousScore(Address address, int unFreeze) {}

    @EventLog(indexed=0)
    public void PRepDisqualified(Address address, boolean success, String reason) {}

    @EventLog(indexed=1)
    public void IRepChanged(int irep) {}

    @EventLog(indexed=0)
    public void NetworkProposalRegistered(
            String title,
            String description,
            int type,
            byte[] value,
            Address proposer
    ) {}

    @EventLog(indexed=0)
    public void NetworkProposalCanceled(byte[] id) {}

    @EventLog(indexed=0)
    public void NetworkProposalVoted(byte[] id, int vote, Address voter) {}

    @EventLog(indexed=0)
    public void NetworkProposalApproved(byte[] id) {}

    @EventLog(indexed=0)
    public void NetworkProposalDisapproved(byte[] id) {}

}
