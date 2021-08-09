package com.icon.governance;

import score.Address;
import score.ArrayDB;
import score.Context;
import score.DictDB;

import java.math.BigInteger;
import java.util.Map;

/*
    PROPOSAL STATUS
    0 - Voting
    1 - Approved
    2 - DisApproved
    3 - Canceled
*/
public class NetworkProposal {
    private final DictDB<byte[], byte[]> proposalList = Context.newDictDB("proposal_list", byte[].class);
    private final ArrayDB<byte[]> proposalListKeys = Context.newArrayDB("proposal_list_keys", byte[].class);
    private final DictDB<byte[], Proposal> proposalDict = Context.newDictDB("proposals", Proposal.class);
    private final ArrayDB<byte[]> proposalKeys = Context.newArrayDB("proposal_keys", byte[].class);
    public final static int STATUS_MIN = 0;
    public final static int VOTING_STATUS = 0;
    public final static int APPROVED_STATUS = 1;
    public final static int DISAPPROVED_STATUS = 2;
    public final static int CANCELED_STATUS = 3;
    public final static int STATUS_MAX = CANCELED_STATUS;
    public final static int GET_PROPOSALS_FILTER_ALL = 100;
    public final static float APPROVE_RATE = 0.66f;
    public final static float DISAPPROVE_RATE = 0.33f;

    public Proposal getProposal(byte[] id) {
        byte[] data = proposalList.getOrDefault(id, new byte[0]);
        Proposal p;
        if (data.length > 0) {
            p = Proposal.loadJson(data);
        } else {
            p = proposalDict.get(id);
        }
        Context.require(p != null, "No registered proposal");
        return p;
    }

    public Map<String, Object>[] getProposals(int typeCondition, int statusCondition) {
        var listKeySize = proposalListKeys.size();
        var keysSize = proposalKeys.size();
        Map<String, Object>[] proposals = new Map[listKeySize + keysSize];

        for (int i=0; i < listKeySize; i++) {
            var key = proposalListKeys.get(i);
            var proposal = Proposal.loadJson(proposalList.get(key));
            filterProposal(typeCondition, statusCondition, proposals, i, proposal);
        }

        for (int i=0; i < keysSize; i++) {
            var key = proposalKeys.get(i);
            var proposal = proposalDict.get(key);
            filterProposal(typeCondition, statusCondition, proposals, listKeySize + i, proposal);
        }

        return proposals;
    }

    private void filterProposal(int typeCondition, int statusCondition, Map<String, Object>[] proposals, int i, Proposal proposal) {
        int type = proposal.type;
        int status = proposal.status;
        if ((typeCondition == type || typeCondition == NetworkProposal.GET_PROPOSALS_FILTER_ALL) &&
                (statusCondition == status) || statusCondition == NetworkProposal.GET_PROPOSALS_FILTER_ALL) {
            var proposalMap = proposal.toMap();
            proposals[i] = proposalMap;
        }
    }

    public void registerProposal(
            String title,
            String description,
            int type,
            Value value,
            PRepInfo[] prepsInfo,
            Map<String, Object> term
    ) {
        var id = Context.getTransactionHash();
        var proposer = Context.getCaller();
        var blockHeight = BigInteger.valueOf(Context.getBlockHeight());
        BigInteger totalBondedDelegation = BigInteger.ZERO;
        String proposerName = "";
        Address[] preps = new Address[prepsInfo.length];

        Voter v = new Voter();

        for (int i=0; i < prepsInfo.length; i++) {
            var prep = prepsInfo[i];
            if (proposer.equals(prep.getAddress())) {
                proposerName = prep.getName();
            }

            totalBondedDelegation = totalBondedDelegation.add(prep.getBondedDelegation());
            preps[i] = prep.getAddress();
        }

        v.setAmountForNoVote(totalBondedDelegation);
        v.setNoVoteList(preps);

        /*
            currentTermEnd: endBlockHeight
            4-terms: termPeriod * 4
            currentTermEnd + 4terms = 5terms
         */
        BigInteger expireVotingPeriod = (BigInteger) term.get("period");
        expireVotingPeriod = expireVotingPeriod.multiply(BigInteger.valueOf(4));
        expireVotingPeriod = expireVotingPeriod.add((BigInteger) term.get("endBlockHeight"));

        Context.println("NetworkProposal(" + " ExpireVoting: " + expireVotingPeriod);

        Proposal proposal = new Proposal(
                id,
                proposer,
                proposerName,
                title,
                description,
                type,
                value,
                blockHeight,
                expireVotingPeriod,
                VOTING_STATUS,
                v,
                prepsInfo.length,
                totalBondedDelegation
        );
        proposalDict.set(id, proposal);
        proposalKeys.add(id);
    }

    public void cancelProposal(byte[] id, Address sender) {
        Proposal p = getProposal(id);
        var blockHeight = BigInteger.valueOf(Context.getBlockHeight());
        Context.require(p != null, "no registered proposal");
        Context.require(p.expireBlockHeight.compareTo(blockHeight) >= 0, "This proposal has already expired");
        Context.require(sender.equals(p.proposer), "No permission - only for proposer");
        Context.require(p.status == VOTING_STATUS, "Can not be canceled - only voting proposal");
        p.status = CANCELED_STATUS;
        proposalDict.set(id, p);
    }

    public int voteProposal(
            Proposal p,
            int vote,
            PRepInfo prep
    ) {
        var blockHeight = BigInteger.valueOf(Context.getBlockHeight());
        Context.require(p.expireBlockHeight.compareTo(blockHeight) >= 0, "This proposal has already expired");
        Context.require(p.status != CANCELED_STATUS, "This proposal has canceled");

        p.updateVote(prep, vote);
        int status = VOTING_STATUS;
        if (vote == Voter.AGREE_VOTE) {
            if ((float)p.vote.sizeofAgreed() / p.totalVoter >= APPROVE_RATE &&
                    p.vote.amountOfAgreed().divide(p.totalBondedDelegation).floatValue() >= APPROVE_RATE) {
                p.status = APPROVED_STATUS;
                status = APPROVED_STATUS;
            } else if (p.vote.sizeofNoVote() == 0) {
                p.status = DISAPPROVED_STATUS;
                status = DISAPPROVED_STATUS;
            }
        } else {
            if ((float)p.vote.sizeofDisagreed() / p.totalVoter >= DISAPPROVE_RATE &&
            p.vote.amountOfDisagreed().divide(p.totalBondedDelegation).floatValue() >= DISAPPROVE_RATE) {
                p.status = DISAPPROVED_STATUS;
                status = DISAPPROVED_STATUS;
            }
        }
        proposalDict.set(p.id, p);
        return status;
    }
}
