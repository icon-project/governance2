package com.icon.governance;

import score.ByteArrayObjectWriter;
import score.Address;
import score.Context;
import score.DictDB;
import score.ArrayDB;

import java.math.BigInteger;
import java.util.Map;
import java.util.List;


public class NetworkProposal {
    private final DictDB<byte[], byte[]> proposalList = Context.newDictDB("proposal_list", byte[].class);
    private final ArrayDB<byte[]> proposalListKeys = Context.newArrayDB("proposal_list_keys", byte[].class);
    private final DictDB<byte[], Proposal> proposalGather = Context.newDictDB("proposal_gather", Proposal.class);

    public Map<String, ?> getProposal(byte[] id) {
        byte[] data = this.proposalList.getOrDefault(id, new byte[0]);
        if (data.length > 0) {
            // python db.get
            Proposal p = Proposal.makeWithJson(data);
            return Map.of();
        } else {
            // java db.get
            Context.println("PROPOSAL GATHER");
            Proposal p = proposalGather.get(id);
            return Map.of();
        }
    }

    public void submitProposal(
            byte[] id,
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
                (BigInteger) term.get("startBlockHeight"),
                expireVotingPeriod,
                0, // status enum,,?
                v,
                prepsInfo.length,
                totalBondedDelegation
        );
        proposalGather.set(id, proposal);
    }

    public void cancelProposal() {}

    public void votingProposal() {}

    public void approveProposal() {}

    public void rejectProposal() {}

    public void expireProposal() {}

}