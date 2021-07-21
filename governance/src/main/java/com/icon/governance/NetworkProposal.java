package com.icon.governance;

import score.Address;
import score.ArrayDB;
import score.Context;
import score.DictDB;

import java.math.BigInteger;
import java.util.Map;


public class NetworkProposal {
    private final DictDB<byte[], byte[]> proposalList = Context.newDictDB("proposal_list", byte[].class);
    private final ArrayDB<byte[]> proposalListKeys = Context.newArrayDB("proposal_list_keys", byte[].class);
    private final DictDB<byte[], Proposal> proposalDict = Context.newDictDB("proposals", Proposal.class);
    private final ArrayDB<byte[]> proposalKeys = Context.newArrayDB("proposal_keys", byte[].class);
    public final static int VOTING_STATUS = 0;
    public final static int APPROVED_STATUS = 1;
    public final static int DISAPPROVED_STATUS = 2;
    public final static int CANCELED_STATUS = 3;
    public final static float APPROVE_RATE = 0.66f;
    public final static float DISAPPROVE_RATE = 0.33f;

    public Proposal getProposal(byte[] id) {
        byte[] data = proposalList.getOrDefault(id, new byte[0]);
        Proposal p;
        if (data.length > 0) {
            p = Proposal.loadJson(data);
        } else {
            Context.println("PROPOSAL GATHER");
            p = proposalDict.get(id);
        }
        if (p == null) Context.revert("No registered proposal");
        return p;
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
    }

    public void cancelProposal(byte[] id, Address sender) {
        Proposal p = getProposal(id);
        var blockHeight = BigInteger.valueOf(Context.getBlockHeight());
        if (p == null) {
            Context.revert("No registered proposal");
        }
        if (p.expireBlockHeight.compareTo(blockHeight) < 0) {
            Context.revert("This proposal has already expired");
        }
        if (!sender.equals(p.proposer)) {
            Context.revert("No permission - only for proposer");
        }
        if (p.status != VOTING_STATUS) {
            Context.revert("Can not be canceled - only voting proposal");
        }
        p.status = CANCELED_STATUS;
        proposalDict.set(id, p);
    }

    public int voteProposal(
            byte[] id,
            int vote,
            PRepInfo prep,
            PRepInfo[] prepsInfo
    ) {
        Proposal p = getProposal(id);
        if (p == null) {
            Context.revert("No registered proposal");
        }
        p.validateVote(vote);

        var blockHeight = BigInteger.valueOf(Context.getBlockHeight());
        if (p.expireBlockHeight.compareTo(blockHeight) < 0) {
            Context.revert("This proposal has already expired");
        }
        if (p.status == CANCELED_STATUS) {
            Context.revert("This proposal has already canceled");
        }

        p.updateVote(prep, vote);
        int status = VOTING_STATUS;
        if (vote == Voter.AGREE_VOTE) {
            if ((float)p.vote.sizeofAgreed() / p.totalVoter >= APPROVE_RATE &&
                    p.vote.amountOfAgreed().divide(p.totalBondedDelegation).floatValue() >= APPROVE_RATE) {
                p.status = APPROVED_STATUS;
                status = APPROVED_STATUS;
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
