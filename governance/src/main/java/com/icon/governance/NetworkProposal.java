package com.icon.governance;

import score.Address;
import score.Context;
import score.DictDB;
import score.ArrayDB;

import java.math.BigInteger;
import java.util.Map;


public class NetworkProposal {
    private final DictDB<byte[], byte[]> proposalList = Context.newDictDB("proposal_list", byte[].class);
    private final ArrayDB<byte[]> proposalListKeys = Context.newArrayDB("proposal_list_keys", byte[].class);
    private final DictDB<byte[], Proposal> proposalGather = Context.newDictDB("proposal_gather", Proposal.class);
    private final ArrayDB<byte[]> proposalGatherKeys = Context.newArrayDB("proposal_gather_keys", byte[].class);
    private final static int VOTING_STATUS = 0;
    private final static int APPROVED_STATUS = 0;
    private final static int DISAPPROVED_STATUS = 0;
    private final static int CANCELED_STATUS = 0;

    public Proposal getProposal(byte[] id) {
        byte[] data = proposalList.getOrDefault(id, new byte[0]);
        Proposal p;
        if (data.length > 0) {
            // get python governance data
            p = Proposal.loadJson(data);
        } else {
            // get java governance data
            Context.println("PROPOSAL GATHER");
            p = proposalGather.get(id);
        }
        if (p == null) Context.revert("No registered proposal");
        return p;
    }

    public void registerProposal(
            byte[] id,
            BigInteger blockHeight,
            Address proposer,
            String title,
            String description,
            int type,
            Value value,
            ChainScore chainScore
    ) {

        PRepInfo[] prepsInfo = chainScore.getMainPRepsInfo();
        BigInteger totalBondedDelegation = BigInteger.ZERO;
        String proposerName = "";

        Voter v = new Voter();

        for (PRepInfo prep : prepsInfo) {
            if (proposer.equals(prep.getAddress())) {
                proposerName = prep.getName();
            }

            totalBondedDelegation = totalBondedDelegation.add(prep.getBondedDelegation());
        }

        v.setAmountForNoVote(totalBondedDelegation);

        /*
            currentTermEnd: endBlockHeight
            4-terms: termPeriod * 4
            currentTermEnd + 4terms = 5terms
         */
        Map<String, Object> term = chainScore.getPRepTerm();
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
        proposalGather.set(id, proposal);
    }

    public void cancelProposal(byte[] id, Address sender, BigInteger blockHeight) {
        Proposal p = getProposal(id);
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
        proposalGather.set(id, p);
    }

    public void voteProposal(
            byte[] id,
            int vote,
            BigInteger blockHeight,
            byte[] txHash,
            BigInteger timestamp,
            PRepInfo[] prepsInfo
    ) {

    }

    public void expireProposal() {}
}
