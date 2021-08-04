package com.icon.governance;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;
import score.Address;
import score.Context;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Payable;

import java.math.BigInteger;
import java.util.Map;


public class Governance {
    private final static ChainScore chainScore = new ChainScore();
    private final static NetworkProposal networkProposal = new NetworkProposal();

    private void setRevision(BigInteger code) {
        chainScore.setRevision(code);
        RevisionChanged(code);
    }

    private void setStepPrice(BigInteger price) {
        chainScore.setStepPrice(price);
        StepPriceChanged(price);
    }

    private void setStepCost(String type, BigInteger cost) {
        chainScore.setStepCost(type, cost);
        StepCostChanged(type, cost);
    }

    private void setIRep(BigInteger irep) {
        chainScore.setIRep(irep);
        IRepChanged(irep);
    }

    private void blockScore(Address address) {
        chainScore.blockScore(address);
        MaliciousScore(address, false);
    }

    private void unblockScore(Address address) {
        chainScore.unblockScore(address);
        MaliciousScore(address, true);
    }

    private void disqualifyPRep(Address address) {
        chainScore.disqualifyPRep(address);
        PRepDisqualified(address, true, "");
    }

    private void processMaliciousProposal(Address address, int type) {
        if (type == Proposal.FREEZE_SCORE) {
            blockScore(address);
        } else {
            unblockScore(address);
        }
    }

    @External
    public void acceptScore(byte[] txHash) {
        chainScore.acceptScore(txHash);
        Accepted(txHash);
    }

    @External
    public void rejectScore(byte[] txHash, String reason) {
        chainScore.rejectScore(txHash, reason);
        Rejected(txHash, reason);
    }

    @External(readonly = true)
    public Map<String, ?> getProposal(byte[] id) {
        Proposal p = networkProposal.getProposal(id);
        if (p == null) {
            return null;
        }
        return p.toMap();
    }


    @Payable
    @External
    public void registerProposal(
            String title,
            String description,
            BigInteger type,
            byte[] value
    ) {
        var fee = Context.getValue();
        if (fee.compareTo(fee()) != 0) {
            Context.revert("have to pay 100ICX to register proposal");
        }
        Address proposer = Context.getCaller();
        PRepInfo[] prepsInfo = chainScore.getMainPRepsInfo();
        var prep = getPRepInfo(proposer, prepsInfo);
        if (prep == null)
            Context.revert("No permission - only for main prep");

        int intTypeValue = type.intValue();
        String stringValue = new String(value);
        JsonValue json = Json.parse(stringValue);
        Value v = Value.makeWithJson(intTypeValue, json.asObject());
        var term = chainScore.getPRepTerm();
        networkProposal.registerProposal(
                title,
                description,
                intTypeValue,
                v,
                prepsInfo,
                term
        );
        NetworkProposalRegistered(title, description, intTypeValue, value, proposer);
    }

    @External
    public void voteProposal(byte[] id, int vote) {
        Address sender = Context.getCaller();
        PRepInfo[] prepsInfo = chainScore.getMainPRepsInfo();
        var prep = getPRepInfo(sender, prepsInfo);
        if (prep == null)
            Context.revert("No permission - only for main prep");

        Proposal p = networkProposal.getProposal(id);
        if (p == null) {
            Context.revert("No registered proposal");
        }
        p.validateVote(vote);

        var status = networkProposal.voteProposal(p, vote, prep);
        NetworkProposalVoted(id, vote, sender);

        if (status == NetworkProposal.APPROVED_STATUS) {
            approveProposal(p.value);
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

    private BigInteger fee() {
        BigInteger result = BigInteger.ONE;
        for (int i = 0; i < 20; i++) {
            result = result.multiply(BigInteger.TEN);
        }
        return result;
    }

    public void approveProposal(Value value) {
        var address = value.address();
        var v = value.value();
        var type = value.type();
        switch (value.type()) {
            case Proposal.TEXT:
                return;
            case Proposal.REVISION:
                setRevision(v);
            case Proposal.MALICIOUS_SCORE:
                processMaliciousProposal(address, type);
            case Proposal.PREP_DISQUALIFICATION:
                disqualifyPRep(address);
            case Proposal.STEP_PRICE:
                setStepPrice(v);
                StepPriceChanged(v);
            case Proposal.IREP:
                setIRep(v);
                IRepChanged(v);
        }
    }

    /*
     * Events
    */
    @EventLog(indexed=1)
    public void Accepted(byte[] txHash) {}

    @EventLog(indexed=1)
    public void Rejected(byte[] txHash, String reason) {}

    @EventLog(indexed=1)
    public void StepPriceChanged(BigInteger stepPrice) {}

    @EventLog(indexed=1)
    public void StepCostChanged(String type, BigInteger stepCost) {}

    @EventLog(indexed=0)
    public void RevisionChanged(BigInteger revisionCode) {}

    @EventLog(indexed=0)
    public void MaliciousScore(Address address, boolean unFreeze) {}

    @EventLog(indexed=0)
    public void PRepDisqualified(Address address, boolean success, String reason) {}

    @EventLog(indexed=1)
    public void IRepChanged(BigInteger irep) {}

    @EventLog(indexed=0)
    public void NetworkProposalRegistered(String title, String description, int type, byte[] value, Address proposer) {}

    @EventLog(indexed=0)
    public void NetworkProposalCanceled(byte[] id) {}

    @EventLog(indexed=0)
    public void NetworkProposalVoted(byte[] id, int vote, Address voter) {}

    @EventLog(indexed=0)
    public void NetworkProposalApproved(byte[] id) {}

    @EventLog(indexed=0)
    public void NetworkProposalDisapproved(byte[] id) {}

}
