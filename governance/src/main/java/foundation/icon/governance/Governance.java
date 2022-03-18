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

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
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

    private final static ChainScore chainScore = new ChainScore();
    private final static Address address = Address.fromString("cx0000000000000000000000000000000000000001");
    private final static NetworkProposal networkProposal = new NetworkProposal();
    private final ArrayDB<Address> auditors = Context.newArrayDB("auditor_list", Address.class);
    private final DictDB<BigInteger, TimerInfo> timerInfo = Context.newDictDB("timerInfo", TimerInfo.class);

    private void setRevision(BigInteger code) {
        chainScore.setRevision(code);
        RevisionChanged(code);
    }

    private void setStepPrice(BigInteger price) {
        chainScore.setStepPrice(price);
        StepPriceChanged(price);
    }

    private void setStepCosts(String type, BigInteger cost) {
        chainScore.setStepCost(type, cost);
        StepCostChanged(type, cost);
    }

    private void setRewardFund(BigInteger rewardFund) {
        chainScore.setRewardFund(rewardFund);
        RewardFundSettingChanged(rewardFund);
    }

    private void setRewardFundsRate(BigInteger iprep, BigInteger icps, BigInteger irelay, BigInteger ivoter) {
        chainScore.setRewardFundsRate(iprep, icps, irelay, ivoter);
        RewardFundAllocationChanged(iprep, icps, irelay, ivoter);
    }

    private void blockScore(Address address) {
        chainScore.blockScore(address);
        MaliciousScore(address, 0);
    }

    private void unblockScore(Address address) {
        chainScore.unblockScore(address);
        MaliciousScore(address, 1);
    }

    private void disqualifyPRep(Address address) {
        if (_disqualifyPRep(address)) {
            PRepDisqualified(address, true, "");
        } else {
            PRepDisqualified(address, false, "Error raised on chain SCORE");
        }
    }

    private boolean _disqualifyPRep(Address address) {
        try {
            chainScore.disqualifyPRep(address);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void processMaliciousProposal(Address address, BigInteger type) {
        if (type.intValue() == Value.FREEZE_SCORE) {
            blockScore(address);
        } else {
            unblockScore(address);
        }
    }

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
        return chainScore.getRevision();
    }

    @External(readonly = true)
    public String getVersion() {
        return "2.0.0";
    }

    @External(readonly = true)
    public BigInteger getStepPrice() {
        return chainScore.getStepPrice();
    }

    @External(readonly = true)
    public Map<String, Object> getStepCosts() {
        return chainScore.getStepCosts();
    }

    @External(readonly = true)
    public BigInteger getMaxStepLimit(String contextType) {
        return chainScore.getMaxStepLimit(contextType);
    }

    @External(readonly = true)
    public Map<String, Object> getScoreStatus(Address address) {
        return chainScore.getScoreStatus(address);
    }

    @External
    public void acceptScore(byte[] txHash) {
        var caller = Context.getCaller();
        Context.require(isAuditor(caller), "Invalid sender: no permission");
        chainScore.acceptScore(txHash);
        Accepted(txHash);
    }

    @External
    public void rejectScore(byte[] txHash, String reason) {
        var caller = Context.getCaller();
        Context.require(isAuditor(caller), "Invalid sender: no permission");
        chainScore.rejectScore(txHash);
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
        var addresses = chainScore.getBlockedScores();
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
            byte[] value
    ) {
        Context.require(PROPOSAL_REGISTRATION_FEE.compareTo(Context.getValue()) == 0, "100 ICX required to register proposal");
        chainScore.burn(PROPOSAL_REGISTRATION_FEE);
        Address proposer = Context.getCaller();
        PRepInfo[] mainPRepsInfo = chainScore.getMainPRepsInfo();
        var prep = getPRepInfoFromList(proposer, mainPRepsInfo);
        Context.require(prep != null, "No permission - only for main prep");

        String stringValue = new String(value);
        JsonValue json = Json.parse(stringValue);
        JsonArray values = json.asArray();
        validateProposals(values);
        Value v = new Value(Proposal.NETWORK_PROPOSAL, value);

        var term = chainScore.getPRepTerm();

        /*
            currentTermEnd: endBlockHeight
            4-terms: termPeriod * 4
            currentTermEnd + 4terms = 5terms
         */
        BigInteger expireVotingHeight = (BigInteger) term.get("period");
        expireVotingHeight = expireVotingHeight.multiply(BigInteger.valueOf(4));
        expireVotingHeight = expireVotingHeight.add((BigInteger) term.get("endBlockHeight"));

        networkProposal.registerProposal(
                title,
                description,
                v,
                mainPRepsInfo,
                expireVotingHeight
        );
        BigInteger penaltyHeight = BigInteger.ONE.add(expireVotingHeight);
        TimerInfo ti = timerInfo.getOrDefault(penaltyHeight, null);
        if (ti == null) {
            ti = new TimerInfo(new TimerInfo.ProposalIds());
            ti.addProposalId(Context.getTransactionHash());
            timerInfo.set(penaltyHeight, ti);
            chainScore.addTimer(penaltyHeight);
        } else {
            ti.addProposalId(Context.getTransactionHash());
            timerInfo.set(penaltyHeight, ti);
        }
        NetworkProposalRegistered(title, description, Proposal.NETWORK_PROPOSAL, value, proposer);
    }

    @External
    public void voteProposal(byte[] id, int vote) {
        Address sender = Context.getCaller();
        PRepInfo[] prepsInfo = chainScore.getPRepsInfo();
        var prep = getPRepInfoFromList(sender, prepsInfo);

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
    public void applyProposal(byte []id) {
        Address sender = Context.getCaller();
        Proposal p = networkProposal.getProposal(id);
        BigInteger blockHeight = BigInteger.valueOf(Context.getBlockHeight());
        Context.require(p.agreed(sender) || p.disagreed(sender), "No permission - only for voted preps");
        Context.require(p.getStatus(blockHeight) == NetworkProposal.APPROVED_STATUS, "Only approved proposal can be applied");
        NetworkProposalApplied(id);
        applyProposal(p);
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
                chainScore.removeTimer(timerHeight);
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
        Context.require(sender.equals(ChainScore.CHAIN_SCORE), "only chain SCORE can call onTimer");
        var blockHeight = BigInteger.valueOf(Context.getBlockHeight());
        var ti = timerInfo.getOrDefault(blockHeight, null);
        for (byte[] id : ti.proposalIds.ids) {
            var proposal = networkProposal.getProposal(id);
            var novoters = proposal.getNonVoters();
            chainScore.penalizeNonvoters(List.of(novoters));
            int status = proposal.getStatus(blockHeight);
            if (status == NetworkProposal.EXPIRED_STATUS) {
                NetworkProposalExpired(proposal.id);
            } else if (status == NetworkProposal.DISAPPROVED_STATUS) {
                NetworkProposalDisapproved(proposal.id);
            }
        }
        timerInfo.set(blockHeight, null);
    }

    private PRepInfo getPRepInfoFromList(Address address, PRepInfo[] prepsInfo) {
        for (PRepInfo prep : prepsInfo) {
            if (address.equals(prep.getAddress())) {
                return prep;
            }
        }
        return null;
    }

    public void applyProposal(Proposal proposal) {
        networkProposal.applyProposal(proposal);
        var value = proposal.value;
        var data = value.data();
        String stringValue = new String(data);
        JsonValue json = Json.parse(stringValue);
        JsonArray values = json.asArray();
        int length = values.size();
        for (int i = 0; i < length; i++) {
            var object = values.get(i).asObject();
            var name = object.getString("name", "");
            var valueObject = object.get("value").asObject();
            switch (name) {
                case Value.TEXT_TYPE:
                    continue;
                case Value.REVISION_TYPE:
                    var revision = Converter.toInteger(valueObject.getString("revision", null));
                    setRevision(revision);
                    return;
                case Value.MALICIOUS_SCORE_TYPE:
                    var type = Converter.toInteger(valueObject.getString("type", null));
                    processMaliciousProposal(Converter.toAddress(valueObject.getString("address", null)), type);
                    continue;
                case Value.PREP_DISQUALIFICATION_TYPE:
                    disqualifyPRep(Converter.toAddress(valueObject.getString("address", null)));
                    continue;
                case Value.STEP_PRICE_TYPE:
                    var price = Converter.toInteger(valueObject.getString("stepPrice", null));
                    setStepPrice(price);
                    continue;
                case Value.STEP_COSTS_TYPE:
                    var stepCosts = Value.StepCosts.fromJson(valueObject.get("costs").asObject());
                    for (Value.StepCosts.StepCost s : stepCosts.getCosts()) {
                        setStepCosts(s.getType(), s.getCost());
                    }
                    continue;
                case Value.REWARD_FUND_TYPE:
                    var iglobal = Converter.toInteger(valueObject.getString("iglobal", null));
                    setRewardFund(iglobal);
                    continue;
                case Value.REWARD_FUNDS_ALLOCATION:
                    var rewardRatio = Value.RewardFunds.fromJson(valueObject.get("rewardFunds").asObject());
                    BigInteger iprep = BigInteger.ZERO;
                    BigInteger icps = BigInteger.ZERO;
                    BigInteger irelay = BigInteger.ZERO;
                    BigInteger ivoter = BigInteger.ZERO;
                    for (int j = 0; j < rewardRatio.rewardFunds.length; j++) {
                        var rewardRateInfo = rewardRatio.rewardFunds[j];
                        if (rewardRateInfo.isType(Value.RewardFunds.I_PREP))
                            iprep = rewardRateInfo.getValue();
                        if (rewardRateInfo.isType(Value.RewardFunds.I_CPS))
                            icps = rewardRateInfo.getValue();
                        if (rewardRateInfo.isType(Value.RewardFunds.I_RELAY))
                            irelay = rewardRateInfo.getValue();
                        if (rewardRateInfo.isType(Value.RewardFunds.I_VOTER))
                            ivoter = rewardRateInfo.getValue();
                    }
                    setRewardFundsRate(iprep, icps, irelay, ivoter);
                    continue;
                case Value.NETWORK_SCORE_DESIGNATION_TYPE: {
                    var networkScores = valueObject.get("networkScores").asArray();
                    int l = networkScores.size();
                    for (int j = 0; j < l; j++) {
                        var v = networkScores.get(j).asObject();
                        String role = v.getString("role", null);
                        Address address = Converter.toAddress(v.getString("address", null));
                        chainScore.setNetworkScore(role, address);
                        if (address != null) NetworkScoreDesignated(role, address);
                        else NetworkScoreDeallocated(role);
                    }
                    continue;
                }
                case Value.NETWORK_SCORE_UPDATE_TYPE:
                    Address addr = Converter.toAddress(valueObject.getString("address", null));
                    var content = Converter.hexToBytes(valueObject.getString("content", null));
                    var params = valueObject.get("params");
                    if (params == null) {
                        Context.deploy(addr, content);
                    } else {
                        var p = params.asArray();
                        var size = p.size();
                        String[] stringParams = new String[size];
                        for (int j = 0; j < size; j++) {
                            stringParams[j] = p.get(j).asString();
                        }
                        Context.deploy(addr, content, (Object) stringParams);
                    }
                    NetworkScoreUpdated(addr);
                    continue;
                case Value.ACCUMULATED_VALIDATION_FAILURE_PENALTY: {
                    var rate = Converter.toInteger(valueObject.getString("slashingRate", null));
                    chainScore.setConsistentValidationSlashingRate(rate);
                    continue;
                }
                case Value.MISSED_NETWORK_PROPOSAL_PENALTY: {
                    var rate = Converter.toInteger(valueObject.getString("slashingRate", null));
                    chainScore.setNonVoteSlashingRate(rate);
                }
            }
        }
    }

    public void validateProposals(JsonArray values) {
        int length = values.size();
        for (int i = 0; i < length; i++) {
            var object = values.get(i).asObject();
            var name = object.getString("name", "");
            Context.require(!name.equals(""), "name field required");
            var value = object.get("value").asObject();
            var keys = object.names();
            Context.require(keys.size() == 2);
            keys = value.names();
            var size = keys.size();
            switch (name) {
                case Value.TEXT_TYPE:
                    Context.require(size == 1);
                    Context.require(value.getString("text", null) != null);
                    continue;
                case Value.REVISION_TYPE:
                    Context.require(size == 1);
                    var revision = Converter.toInteger(value.getString("revision", null));
                    validateRevision(revision);
                    continue;
                case Value.MALICIOUS_SCORE_TYPE:
                    Context.require(size == 2);
                    var type = Converter.toInteger(value.getString("type", null));
                    validateMaliciousScore(Converter.toAddress(value.getString("address", null)), type.intValue());
                    continue;
                case Value.PREP_DISQUALIFICATION_TYPE:
                    Context.require(size == 1);
                    validateDisqualifyPRep(Converter.toAddress(value.getString("address", null)));
                    continue;
                case Value.STEP_PRICE_TYPE:
                    Context.require(size == 1);
                    var price = Converter.toInteger(value.getString("stepPrice", null));
                    validateStepPrice(price);
                    continue;
                case Value.STEP_COSTS_TYPE:
                    Context.require(size == 1);
                    Value.StepCosts.fromJson(value.get("costs").asObject());
                    continue;
                case Value.REWARD_FUND_TYPE:
                    Context.require(size == 1);
                    var iglobal = Converter.toInteger(value.getString("iglobal", null));
                    chainScore.validateRewardFund(iglobal);
                    continue;
                case Value.REWARD_FUNDS_ALLOCATION:
                    Context.require(size == 1);
                    var funds = Value.RewardFunds.fromJson(value.get("rewardFunds").asObject());
                    validateRewardFundsRate(funds);
                    continue;
                case Value.NETWORK_SCORE_DESIGNATION_TYPE:
                    Context.require(size == 1);
                    validateDesignationProposal(value);
                    continue;
                case Value.NETWORK_SCORE_UPDATE_TYPE:
                    var required = value.get("params") == null ? 2 : 3;
                    Context.require(size == required, "Invalid array size");
                    Context.require(Converter.toAddress(value.getString("address", null)) != null, "Invalid address");
                    Converter.hexToBytes(value.getString("content", null));
                    continue;
                case Value.ACCUMULATED_VALIDATION_FAILURE_PENALTY:
                case Value.MISSED_NETWORK_PROPOSAL_PENALTY:
                    Context.require(size == 1);
                    var slashingRate = Converter.toInteger(value.getString("slashingRate", null));
                    Context.require(slashingRate.compareTo(BigInteger.ZERO) >= 0 && slashingRate.compareTo(BigInteger.valueOf(100)) <= 0,
                            "slashing rate invalid");
                    continue;
                default:
                    Context.revert("undefined proposal type");
            }
        }
    }

    private void validateRevision(BigInteger revision) {
        var prev = chainScore.getRevision();
        Context.require(revision.compareTo(prev) > 0, "can not decrease revision");
    }

    private void validateMaliciousScore(Address address, int type) {
        if (type != Value.FREEZE_SCORE && type != Value.UNFREEZE_SCORE) {
            Context.revert("invalid value type : " + type);
        } else if (type == Value.FREEZE_SCORE) {
            if (address.equals(Governance.address)) {
                Context.revert("Can not freeze governance SCORE");
            }
        }
    }

    private void validateDisqualifyPRep(Address address) {
        PRepInfo[] preps = chainScore.getPRepsInfo();
        Context.require(getPRepInfoFromList(address, preps) != null, address.toString() + "is not p-rep");
    }

    private void validateStepPrice(BigInteger price) {
        var prevPrice = chainScore.getStepPrice();
        var hundred = BigInteger.valueOf(100);
        var max = prevPrice.multiply(BigInteger.valueOf(125)).divide(hundred);
        var min = prevPrice.multiply(BigInteger.valueOf(75)).divide(hundred);

        Context.require(price.compareTo(min) >= 0 && price.compareTo(max) <= 0, "Invalid step price: " + price);
    }

    private void validateRewardFundsRate(Value.RewardFunds rewardFunds) {
        var values = rewardFunds.rewardFunds;
        var sum = BigInteger.ZERO;
        for (Value.RewardFunds.RewardFund value : values) {
            if (BigInteger.ZERO.compareTo(value.getValue()) > 0) {
                Context.revert("reward fund < 0");
            }
            sum = sum.add(value.getValue());
        }
        Context.require(sum.compareTo(BigInteger.valueOf(100)) == 0, "sum of reward funds must be 100");
    }

    private void validateDesignationProposal(JsonObject value) {
        var networkScores = value.get("networkScores").asArray();
        int length = networkScores.size();
        Context.require(0 < length && length <= 2, "Invalid array size");
        for (int i = 0; i < length; i++) {
            var v = networkScores.get(i).asObject();
            String role = v.getString("role", null);
            Address address = Converter.toAddress(v.getString("address", null));
            Context.require(Value.CPS_SCORE.equals(role) || Value.RELAY_SCORE.equals(role),
                    "Invalid network SCORE role: " + role);
            if (address == null) return;
            Address owner = chainScore.getScoreOwner(address);
            Context.require(owner.equals(Governance.address), "Only owned by governance can be designated");
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
