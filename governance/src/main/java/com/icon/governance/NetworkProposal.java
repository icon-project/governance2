package com.icon.governance;

import score.Address;
import score.ArrayDB;
import score.Context;
import score.DictDB;
import scorex.util.ArrayList;

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
    public final static int GET_PROPOSALS_MAX_SIZE = 10;
    public final static int GET_PROPOSAL_DEFAULT_START = 0;
    public final static int EVENT_NONE = 0;
    public final static int EVENT_APPROVED = 1;
    public final static int EVENT_DISAPPROVED = 2;

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

    public Map<String, Object>[] getProposals(int typeCondition, int statusCondition, int start, int size) {
        var listKeySize = proposalListKeys.size();
        var keysSize = proposalKeys.size();
        var proposalList = new ArrayList<Map<String, Object>>();
        Map<String, Object>[] proposals;
        int proposalMaxIndex = keysSize + listKeySize - 1;
        Context.require(
                typeCondition == GET_PROPOSALS_FILTER_ALL || typeCondition >= Proposal.MIN && typeCondition <= Proposal.MAX, "invalid type : " + typeCondition);
        Context.require(
                statusCondition == GET_PROPOSALS_FILTER_ALL || statusCondition >= NetworkProposal.STATUS_MIN && statusCondition <= NetworkProposal.STATUS_MAX, "invalid status : " + statusCondition);
        Context.require(start >= 0, "Invalid start parameter: " + start);
        Context.require(size > 0, "Invalid size parameter: " + size);
        Context.require(
                start <= proposalMaxIndex, "Invalid start parameter: " + start + ">" + proposalMaxIndex);

        for (int i = 0; i < listKeySize; i++) {
            var key = proposalListKeys.get(i);
            var proposal = Proposal.loadJson(this.proposalList.get(key));
            filterProposal(typeCondition, statusCondition, proposalList, proposal);
        }

        for (int i = 0; i < keysSize; i++) {
            var key = proposalKeys.get(i);
            var proposal = proposalDict.get(key);
            filterProposal(typeCondition, statusCondition, proposalList, proposal);
        }
        size = Integer.min(size, proposalList.size() - start);

        proposals = new Map[size];
        for (int i = 0; i < size; i++) {
            proposals[i] = proposalList.get(start + i);
        }
        return proposals;
    }

    private void filterProposal(int typeCondition, int statusCondition, ArrayList<Map<String, Object>> proposals, Proposal proposal) {
        var blockHeight = BigInteger.valueOf(Context.getBlockHeight());
        int type = proposal.type;
        int status = proposal.getStatus(blockHeight);
        int all = NetworkProposal.GET_PROPOSALS_FILTER_ALL;
        var condition = ((typeCondition == type || typeCondition == all) && (statusCondition == status || statusCondition == all));
        if (condition) {
            var proposalMap = proposal.getSummary(blockHeight);
            proposals.add(proposalMap);
        }
    }

    public void registerProposal(
            String title,
            String description,
            Value value,
            PRepInfo[] prepsInfo,
            BigInteger expireHeight
    ) {
        var id = Context.getTransactionHash();
        var proposer = Context.getCaller();
        var blockHeight = BigInteger.valueOf(Context.getBlockHeight());
        BigInteger totalPower = BigInteger.ZERO;
        String proposerName = "";
        Address[] preps = new Address[prepsInfo.length];

        VoteInfo v = new VoteInfo();

        for (int i = 0; i < prepsInfo.length; i++) {
            var prep = prepsInfo[i];
            if (proposer.equals(prep.getAddress())) {
                proposerName = prep.getName();
            }

            totalPower = totalPower.add(prep.power());
            preps[i] = prep.getAddress();
        }

        v.setAmountForNoVote(totalPower);
        v.setNoVoteList(preps);

        Context.println("NetworkProposal(" + " ExpireVoting: " + expireHeight);

        Proposal proposal = new Proposal(
                id,
                proposer,
                proposerName,
                title,
                description,
                Proposal.NETWORK_PROPOSAL,
                value,
                blockHeight,
                expireHeight,
                VOTING_STATUS,
                v,
                prepsInfo.length,
                totalPower
        );
        proposalDict.set(id, proposal);
        proposalKeys.add(id);
    }

    public void cancelProposal(Proposal p) {
        p.status = CANCELED_STATUS;
        proposalDict.set(p.id, p);
    }

    public int voteProposal(
            Proposal p,
            int vote,
            PRepInfo prep
    ) {
        p.updateVote(prep, vote);
        int votingEvent = EVENT_NONE;
        int currentStatus = p.status;
        if (vote == VoteInfo.AGREE_VOTE) {
            if (p.sizeofAgreed() * 3 >= p.totalVoter * 2 &&
                    p.amountOfAgreed().multiply(BigInteger.valueOf(3)).compareTo(p.totalPower.multiply(BigInteger.TWO)) >= 0) {
                p.status = APPROVED_STATUS;
                votingEvent = EVENT_APPROVED;
            } else if (p.sizeofNoVote() == 0) {
                p.status = DISAPPROVED_STATUS;
                votingEvent = EVENT_DISAPPROVED;
            }
        } else {
            if (p.sizeofDisagreed() * 3 >= p.totalVoter &&
                    p.amountOfDisagreed().multiply(BigInteger.valueOf(3)).compareTo(p.totalPower) >= 0) {
                p.status = DISAPPROVED_STATUS;
                votingEvent = EVENT_DISAPPROVED;
            }
        }
        proposalDict.set(p.id, p);
        if (currentStatus == VOTING_STATUS) {
            return votingEvent;
        }
        return EVENT_NONE;
    }
}
