/*
 * Copyright 2022 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package foundation.icon.governance;

import score.Address;
import score.ArrayDB;
import score.Context;
import score.DictDB;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class NetworkProposal {
    // legacy proposal DB written in Python version
    private final DictDB<byte[], byte[]> proposalList = Context.newDictDB("proposal_list", byte[].class);
    private final ArrayDB<byte[]> proposalListKeys = Context.newArrayDB("proposal_list_keys", byte[].class);
    // new proposal DB after Java migration
    private final DictDB<byte[], Proposal> proposalDict = Context.newDictDB("proposals", Proposal.class);
    private final DictDB<byte[], byte[]> proposalValueDict = Context.newDictDB("proposal_values", byte[].class);
    private final ArrayDB<byte[]> proposalKeys = Context.newArrayDB("proposal_keys", byte[].class);

    public final static int VOTING_STATUS = 0;
    public final static int APPLIED_STATUS = 1;
    public final static int DISAPPROVED_STATUS = 2;
    public final static int CANCELED_STATUS = 3;
    public final static int APPROVED_STATUS = 4;
    public final static int EXPIRED_STATUS = 5;
    public final static int STATUS_MIN = VOTING_STATUS;
    public final static int STATUS_MAX = EXPIRED_STATUS;

    public final static int GET_PROPOSALS_FILTER_ALL = 100;
    public final static int GET_PROPOSALS_MAX_SIZE = 10;

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

    public byte[] getProposalValue(byte[] id) {
        byte[] value = proposalValueDict.get(id);
        Context.require(value != null);
        return value;
    }

    public List<Object> getProposals(int typeCondition, int statusCondition, int start, int size) {
        Context.require(typeCondition == GET_PROPOSALS_FILTER_ALL ||
                        typeCondition >= Proposal.MIN && typeCondition <= Proposal.MAX,
                "Invalid type: " + typeCondition);
        Context.require(statusCondition == GET_PROPOSALS_FILTER_ALL ||
                        statusCondition >= NetworkProposal.STATUS_MIN && statusCondition <= NetworkProposal.STATUS_MAX,
                "Invalid status: " + statusCondition);
        Context.require(start >= 0, "Invalid start parameter: " + start);
        Context.require(size > 0 && size <= GET_PROPOSALS_MAX_SIZE, "Invalid size parameter: " + size);

        var proposalList = new ArrayList<Map<String, Object>>();
        var keySize = proposalKeys.size();
        int count = 0;
        for (int i = keySize - start - 1; i >= 0 && count < size; i--) {
            var key = proposalKeys.get(i);
            var proposal = proposalDict.get(key);
            count += filterProposal(typeCondition, statusCondition, proposalList, proposal);
        }
        if (count == size) {
            return List.of(proposalList.toArray());
        }

        start = start >= keySize ? start - keySize : 0;
        var listKeySize = proposalListKeys.size();
        for (int i = listKeySize - start - 1; i >= 0 && count < size; i--) {
            var key = proposalListKeys.get(i);
            var proposal = Proposal.loadJson(this.proposalList.get(key));
            count += filterProposal(typeCondition, statusCondition, proposalList, proposal);
        }
        return List.of(proposalList.toArray());
    }

    private int filterProposal(int typeCondition, int statusCondition, ArrayList<Map<String, Object>> proposals, Proposal proposal) {
        var blockHeight = BigInteger.valueOf(Context.getBlockHeight());
        int type = proposal.type;
        int status = proposal.getStatus(blockHeight);
        int all = NetworkProposal.GET_PROPOSALS_FILTER_ALL;
        var condition = ((typeCondition == type || typeCondition == all) && (statusCondition == status || statusCondition == all));
        if (condition) {
            var proposalMap = proposal.getSummary(blockHeight);
            proposals.add(proposalMap);
            return 1;
        }
        return 0;
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
                null,
                blockHeight,
                expireHeight,
                VOTING_STATUS,
                v,
                prepsInfo.length,
                totalPower,
                null
        );
        proposalDict.set(id, proposal);
        proposalKeys.add(id);
        proposalValueDict.set(id, value.data());
    }

    public void cancelProposal(Proposal p) {
        p.status = CANCELED_STATUS;
        proposalDict.set(p.id, p);
    }

    public void applyProposal(Proposal p) {
        p.status = APPLIED_STATUS;
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
        if (currentStatus == VOTING_STATUS) {
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
            return votingEvent;
        }
        return EVENT_NONE;
    }
}
