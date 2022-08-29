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

import score.*;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;


public class Governance {
    private static final BigInteger EXA = BigInteger.valueOf(1_000_000_000_000_000_000L);
    private static final BigInteger PROPOSAL_REGISTRATION_FEE = BigInteger.valueOf(100).multiply(EXA);
    public final static Address address = Address.fromString("cx0000000000000000000000000000000000000001");
    private final static NetworkProposal networkProposal = new NetworkProposal();
    private final ArrayDB<Address> auditors = Context.newArrayDB("auditor_list", Address.class);
    private final DictDB<BigInteger, TimerInfo> timerInfo = Context.newDictDB("timerInfo", TimerInfo.class);

    private boolean isAuditor(Address address) {
        var auditorSize = auditors.size();
        for (int i = 0; i < auditorSize; i++) {
            var auditor = auditors.get(i);
            if (address.equals(auditor)) {
                return true;
            }
        }
        return false;
    }

    @External(readonly = true)
    public BigInteger getRevision() {
        return ChainScore.getRevision();
    }

    @External(readonly = true)
    public String getVersion() {
        return "2.1.3";
    }

    @External(readonly = true)
    public BigInteger getStepPrice() {
        return ChainScore.getStepPrice();
    }

    @External(readonly = true)
    public Map<String, Object> getStepCosts() {
        return ChainScore.getStepCosts();
    }

    @External(readonly = true)
    public BigInteger getMaxStepLimit(String contextType) {
        return ChainScore.getMaxStepLimit(contextType);
    }

    @External(readonly = true)
    public Map<String, Object> getScoreStatus(Address address) {
        return ChainScore.getScoreStatus(address);
    }

    @External
    public void acceptScore(byte[] txHash) {
        var caller = Context.getCaller();
        Context.require(isAuditor(caller), "Invalid sender: no permission");
        ChainScore.acceptScore(txHash);
        Accepted(txHash);
    }

    @External
    public void rejectScore(byte[] txHash, String reason) {
        var caller = Context.getCaller();
        Context.require(isAuditor(caller), "Invalid sender: no permission");
        ChainScore.rejectScore(txHash);
        Rejected(txHash, reason);
    }

    @External
    public void addAuditor(Address address) {
        var caller = Context.getCaller();
        Context.require(!caller.isContract(), "Invalid EOA Address: " + caller );
        Context.require(caller.equals(Context.getOwner()), "Invalid sender: not owner");
        Context.require(!isAuditor(address), "Invalid address: already auditor");
        auditors.add(address);
    }

    @External
    public void removeAuditor(Address address) {
        var caller = Context.getCaller();
        Context.require(!caller.isContract(), "Invalid EOA Address: " + caller );
        Context.require(caller.equals(Context.getOwner()), "Invalid sender: not owner");

        var top = auditors.pop();
        var auditorSize = auditors.size();
        for (int i = 0; i < auditorSize; i++) {
            var auditor = auditors.get(i);
            if (address.equals(auditor)) {
                auditors.set(i, top);
                break;
            }
        }
    }

    @External(readonly = true)
    public boolean isInScoreBlackList(Address address) {
        var addresses = ChainScore.getBlockedScores();
        for (Address addr : addresses) {
            if (address.equals(addr)) return true;
        }
        return false;
    }

    /**
     * Get a proposal info as dict
     *
     * @param id transaction hash to generate when registering proposal
     * @return proposal information in dict
     */
    @External(readonly = true)
    public Map<String, Object> getProposal(byte[] id) {
        Proposal p = networkProposal.getProposal(id);
        if (p == null) {
            return null;
        } else if (p.type == Proposal.NETWORK_PROPOSAL) {
            p.value = new Value(Proposal.NETWORK_PROPOSAL, networkProposal.getProposalValue(id));
        } else if (p.type == Proposal.EXTERNAL_CALL) {
            p.value = new Value(Proposal.EXTERNAL_CALL, networkProposal.getProposalRequest(id));
        }
        return p.toMap(BigInteger.valueOf(Context.getBlockHeight()));
    }

    /**
     * Get a list of proposals filtered by type, status, start and size
     *
     * @param type type of network proposal to filter (optional)
     * @param status status of network proposal to filter (optional)
     * @param start starting index of network proposal to filter. Default is 0, which means the latest (optional)
     * @param size size of network proposal to filter. Default and maximum is 10 (optional)
     * @return proposal list in dict
     */
    @External(readonly = true)
    public Map<String, Object> getProposals(@Optional BigInteger type, @Optional BigInteger status,
                                            @Optional BigInteger start, @Optional BigInteger size) {
        int _type = IS_ZERO(type) ? NetworkProposal.GET_PROPOSALS_FILTER_ALL : type.intValue();
        int _status = IS_ZERO(status) ? NetworkProposal.GET_PROPOSALS_FILTER_ALL : status.intValue();
        int _start = start.intValue();
        int _size = IS_ZERO(size) ? NetworkProposal.GET_PROPOSALS_MAX_SIZE : size.intValue();
        return Map.of("proposals", networkProposal.getProposals(_type, _status, _start, _size));
    }

    private boolean IS_ZERO(BigInteger value) {
        return value.signum() == 0;
    }

    @Payable
    @External
    public void registerProposal(
            String title,
            String description,
            CallRequest[] requests
    ) {
        Context.require(PROPOSAL_REGISTRATION_FEE.compareTo(Context.getValue()) == 0, "100 ICX required to register proposal");
        ChainScore.burn(PROPOSAL_REGISTRATION_FEE);
        Address proposer = Context.getCaller();
        var prep = ChainScore.getPRepInfoFromList(proposer);
        Context.require(prep != null, "No permission - only for main prep");

        var callRequests = new CallRequests(requests);
        callRequests.validateRequests();
        Value v = new Value(Proposal.EXTERNAL_CALL, callRequests);

        var expireVotingHeight = ChainScore.getExpireVotingHeight();

        networkProposal.registerProposal(
                title,
                description,
                v,
                expireVotingHeight
        );
        setTimerInfo(BigInteger.ONE.add(expireVotingHeight));
        NetworkProposalRegistered(title, description, Proposal.EXTERNAL_CALL, callRequests.toBytes(), proposer);
    }


    @External
    public void voteProposal(byte[] id, int vote) {
        Address sender = Context.getCaller();
        var prep = ChainScore.getPRepInfoFromList(sender);

        Proposal p = networkProposal.getProposal(id);
        Context.require(p != null, "no registered proposal");
        Context.require(vote == VoteInfo.AGREE_VOTE || vote == VoteInfo.DISAGREE_VOTE, "Invalid vote value : " + vote);

        var blockHeight = BigInteger.valueOf(Context.getBlockHeight());
        Context.require(!p.isExpired(blockHeight), "This proposal has already expired");
        Context.require(p.getStatus(blockHeight) != NetworkProposal.CANCELED_STATUS, "This proposal has canceled");

        Context.require(p.isInNoVote(sender), "No permission - only for prep were main prep when network registered");
        Context.require(!p.agreed(sender) && !p.disagreed(sender), "Already voted");

        var event = networkProposal.voteProposal(p, vote, prep);
        NetworkProposalVoted(id, vote, sender);

        if (event == NetworkProposal.EVENT_APPROVED) {
            NetworkProposalApproved(id);
        } else if (event == NetworkProposal.EVENT_DISAPPROVED) {
            NetworkProposalDisapproved(id);
        }
    }

    @External
    public void applyProposal(byte[] id) {
        Address sender = Context.getCaller();
        Proposal p = networkProposal.getProposal(id);
        var prep = ChainScore.getPRepInfoFromList(sender);
        BigInteger blockHeight = BigInteger.valueOf(Context.getBlockHeight());
        Context.require(p.agreed(sender) || p.disagreed(sender), "No permission - only for voted preps");
        Context.require(p.getStatus(blockHeight) == NetworkProposal.APPROVED_STATUS, "Only approved proposal can be applied");
        NetworkProposalApplied(id);
        var proposal = networkProposal.getProposal(id);
        proposal.apply = new ApplyInfo(
                Context.getTransactionHash(), prep.getAddress(), prep.getName(), BigInteger.valueOf(Context.getTransactionTimestamp()));
        networkProposal.applyProposal(proposal);
        var callRequests = networkProposal.getProposalRequest(id);
        callRequests.handleRequests(this);
    }

    @External
    public void cancelProposal(byte[] id) {
        Address sender = Context.getCaller();

        Proposal p = networkProposal.getProposal(id);

        var blockHeight = BigInteger.valueOf(Context.getBlockHeight());
        Context.require(p != null, "no registered proposal");
        Context.require(!p.isExpired(blockHeight), "This proposal has already expired");
        int status = p.getStatus(blockHeight);
        Context.require(status == NetworkProposal.VOTING_STATUS || status == NetworkProposal.APPROVED_STATUS,
                "Can not be canceled - voting/approved proposal can be canceled");
        Context.require(sender.equals(p.proposer), "No permission - only for proposer");

        networkProposal.cancelProposal(p);
        var timerHeight = BigInteger.ONE.add(p.expireBlockHeight);
        var ti = timerInfo.getOrDefault(timerHeight, null);
        if (ti != null) {
            if (ti.proposalIds.ids.length == 1) {
                timerInfo.set(timerHeight, null);
                ChainScore.removeTimer(timerHeight);
            } else {
                ti.removeProposalId(id);
                timerInfo.set(timerHeight, ti);
            }
        }
        NetworkProposalCanceled(id);
    }

    @External
    public void onTimer() {
        Address sender = Context.getCaller();
        Context.require(sender.equals(ChainScore.ADDRESS), "only chain SCORE can call onTimer");
        var blockHeight = BigInteger.valueOf(Context.getBlockHeight());
        var ti = timerInfo.getOrDefault(blockHeight, null);
        for (byte[] id : ti.proposalIds.ids) {
            var proposal = networkProposal.getProposal(id);
            var novoters = proposal.getNonVoters();
            ChainScore.penalizeNonvoters(List.of(novoters));
            int status = proposal.getStatus(blockHeight);
            if (status == NetworkProposal.EXPIRED_STATUS) {
                if (proposal.apply != null) {
                    networkProposal.applyProposal(proposal);
                } else {
                    NetworkProposalExpired(proposal.id);
                }
            } else if (status == NetworkProposal.DISAPPROVED_STATUS) {
                NetworkProposalDisapproved(proposal.id);
            }
        }
        timerInfo.set(blockHeight, null);
    }

    void deployScore(Address address, byte[] content, String[] params) {
        if (params == null) {
            Context.deploy(address, content);
        } else {
            Context.deploy(address, content, params);
        }
    }

    private void setTimerInfo(BigInteger penaltyHeight) {
        TimerInfo ti = timerInfo.getOrDefault(penaltyHeight, null);
        if (ti == null) {
            ti = new TimerInfo(new TimerInfo.ProposalIds());
            ti.addProposalId(Context.getTransactionHash());
            timerInfo.set(penaltyHeight, ti);
            ChainScore.addTimer(penaltyHeight);
        } else {
            ti.addProposalId(Context.getTransactionHash());
            timerInfo.set(penaltyHeight, ti);
        }
    }

    public static class TimerInfo {
        ProposalIds proposalIds;

        public TimerInfo() {}

        public TimerInfo(ProposalIds proposalIds) {
            this.proposalIds = proposalIds;
        }

        void addProposalId(byte[] id) {
            byte[][] ids = new byte[proposalIds.ids.length + 1][];
            System.arraycopy(proposalIds.ids, 0, ids, 0, proposalIds.ids.length);
            ids[proposalIds.ids.length] = id;
            proposalIds.ids = ids;
        }

        void removeProposalId(byte[] id) {
            byte[][] ids = new byte[proposalIds.ids.length - 1][];
            for (int i = 0; i < proposalIds.ids.length; i++) {
                boolean equal = true;
                for (int j = 0; j < id.length; j++) {
                    if (id[j] != proposalIds.ids[i][j]) {
                        equal = false;
                        break;
                    }
                }
                if (equal) {
                    proposalIds.ids[i] = proposalIds.ids[proposalIds.ids.length - 1];
                    break;
                }
            }
            System.arraycopy(proposalIds.ids, 0, ids, 0, proposalIds.ids.length - 1);
            proposalIds.ids = ids;
        }

        public static void writeObject(ObjectWriter w, TimerInfo ti) {
            w.beginList(1);
            w.write(ti.proposalIds);
            w.end();
        }

        public static TimerInfo readObject(ObjectReader r) {
            r.beginList();
            var t = new TimerInfo();
            t.proposalIds = r.read(ProposalIds.class);
            return t;
        }

        public static class ProposalIds {
            byte[][] ids;

            ProposalIds() {
                ids = new byte[0][0];
            }

            public static void writeObject(ObjectWriter w, ProposalIds p) {
                w.beginList(2);
                w.write(p.ids.length);
                for (byte[] id : p.ids) {
                    w.write(id);
                }
                w.end();
            }

            public static ProposalIds readObject(ObjectReader r) {
                r.beginList();
                var p = new ProposalIds();
                int length = r.readInt();
                byte[][] ids = new byte[length][];

                for (int i = 0; i < length; i++) {
                    byte[] id = r.read(byte[].class);
                    ids[i] = id;
                }
                r.end();
                p.ids = ids;
                return p;
            }
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
    public void MaliciousScore(Address address, int unFreeze) {}

    @EventLog(indexed=0)
    public void PRepDisqualified(Address address, boolean success, String reason) {}

    @EventLog(indexed=0)
    public void RewardFundSettingChanged(BigInteger rewardFund) {}

    @EventLog(indexed=0)
    public void RewardFundAllocationChanged(BigInteger iprep, BigInteger icps, BigInteger irelay, BigInteger ivoter) {}

    @EventLog(indexed=1)
    public void NetworkScoreUpdated(Address address) {}

    @EventLog(indexed=1)
    public void NetworkScoreDesignated(String role, Address address) {}

    @EventLog(indexed=1)
    public void NetworkScoreDeallocated(String role) {}

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

    @EventLog(indexed=0)
    public void NetworkProposalApplied(byte[] id) {}

    @EventLog(indexed=0)
    public void NetworkProposalExpired(byte[] id) {}

}
