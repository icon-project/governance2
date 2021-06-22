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
    private final DictDB<byte[], byte[]> proposalGather;
    private final ArrayDB<byte[]> keyset;

    public NetworkProposal() {
        this.proposalGather = Context.newDictDB("proposal_list", byte[].class);
        this.keyset = Context.newArrayDB("proposal_list_keys", byte[].class);
    }

    public void submitProposal(
            byte[] id,
            Address proposer,
            String title,
            String description,
            int type,
            byte[] value,
            List<PRepInfo> prepsInfo,
            ChainScore chainScore
    ) {
        BigInteger totalBondedDelegation = BigInteger.ZERO;
        String proposerName = new String();

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
                type,
                v,
                prepsInfo.size(),
                totalBondedDelegation
        );

        ByteArrayObjectWriter enc = Context.newByteArrayObjectWriter("RLPn");
        enc.write(proposal);

        // proposal.toEncode();
         proposalGather.set(id, enc.toByteArray());
    }

    public void cancelProposal() {}

    public void votingProposal() {}

    public void approveProposal() {}

    public void rejectProposal() {}

    public void expireProposal() {}

}