package com.icon.governance;

import score.Address;
import score.annotation.EventLog;

import java.math.BigInteger;


public class Event {

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
}
