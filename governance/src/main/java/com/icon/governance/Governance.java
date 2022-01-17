package com.icon.governance;

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
    private final static ChainScore chainScore = new ChainScore();
    private final static Address address = Address.fromString("cx0000000000000000000000000000000000000001");
    private final static NetworkProposal networkProposal = new NetworkProposal();
    public final static BigInteger proposalRegisterFee = Governance.proposalRegisterFee();
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
    public BigInteger getMaxStepLimit(String t) {
        return chainScore.getMaxStepLimit(t);
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
        chainScore.rejectScore(txHash, reason);
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

    @External(readonly = true)
    public boolean isInImportWhiteList(String importStmt) {
        importStmt = importStmt.replace("'", "\"");
        var stmtJson = Json.parse(importStmt).asObject();
        if (stmtJson.names().size() != 1) {
            return false;
        }
        var value = stmtJson.get("iconservice");
        if (value == null) {
            return false;
        } else {
            var all = value.asArray();
            if (all.size() != 1) {
                return false;
            }
            return all.get(0).asString().equals("*");
        }
    }

    @External(readonly = true)
    public Map<String, ?> getProposal(byte[] id) {
        Proposal p = networkProposal.getProposal(id);
        if (p == null) {
            return null;
        }
        return p.toMap(BigInteger.valueOf(Context.getBlockHeight()));
    }

    @External(readonly = true)
    public List<Map<String, ?>> getProposals(@Optional String type, @Optional String status) {
        int typeIntValue;
        int statusIntValue;
        if (type == null) {
            typeIntValue = NetworkProposal.GET_PROPOSALS_FILTER_ALL;
        } else {
            typeIntValue = Converter.hexToInt(type).intValue();
            Context.require(typeIntValue >= Proposal.MIN && typeIntValue <= Proposal.MAX, "invalid type : " + typeIntValue);
        }
        if (status == null) {
            statusIntValue = NetworkProposal.GET_PROPOSALS_FILTER_ALL;
        } else {
            statusIntValue = Converter.hexToInt(status).intValue();
            Context.require(statusIntValue >= NetworkProposal.STATUS_MIN && statusIntValue <= NetworkProposal.STATUS_MAX,
                    "invalid status : " + statusIntValue);
        }
        return List.of(networkProposal.getProposals(typeIntValue, statusIntValue));
    }

    @Payable
    @External
    public void registerProposal(
            String title,
            String description,
            byte[] value
    ) {
        var fee = Context.getValue();
        Context.require(fee.compareTo(Governance.proposalRegisterFee) == 0, "have to pay 100ICX to register Proposal");
        chainScore.burn();
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
        PRepInfo[] prepsInfo = chainScore.getMainPRepsInfo();
        var prep = getPRepInfoFromList(sender, prepsInfo);

        Proposal p = networkProposal.getProposal(id);
        Context.require(p != null, "no registered proposal");
        Context.require(vote == VoteInfo.AGREE_VOTE || vote == VoteInfo.DISAGREE_VOTE, "Invalid vote value : " + vote);

        var blockHeight = BigInteger.valueOf(Context.getBlockHeight());
        Context.require(!p.isExpired(blockHeight), "This proposal has already expired");
        Context.require(p.getStatus(blockHeight) != NetworkProposal.CANCELED_STATUS, "This proposal has canceled");

        Context.require(p.isInNoVote(sender), "No permission - only for main prep were main prep when network registered");
        Context.require(!p.agreed(sender) && !p.disagreed(sender), "Already voted");

        var event = networkProposal.voteProposal(p, vote, prep);
        NetworkProposalVoted(id, vote, sender);

        if (event == NetworkProposal.EVENT_APPROVED) {
            NetworkProposalApproved(id);
            approveProposal(p.value);
        } else if (event == NetworkProposal.EVENT_DISAPPROVED) {
            NetworkProposalDisapproved(id);
        }
    }

    @External
    public void cancelProposal(byte[] id) {
        Address sender = Context.getCaller();

        Proposal p = networkProposal.getProposal(id);

        var blockHeight = BigInteger.valueOf(Context.getBlockHeight());
        Context.require(p != null, "no registered proposal");
        Context.require(p.isExpired(blockHeight) == false, "This proposal has already expired");
        Context.require(p.getStatus(blockHeight) == NetworkProposal.VOTING_STATUS, "Can not be canceled - only voting proposal");
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
            if (proposal.isExpired(blockHeight)) {
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

    private static BigInteger proposalRegisterFee() {
        BigInteger result = BigInteger.ONE;
        for (int i = 0; i < 20; i++) {
            result = result.multiply(BigInteger.TEN);
        }
        return result;
    }

    public void approveProposal(Value value) {
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
                    var revision = Converter.hexToInt(valueObject.getString("revision", null));
                    setRevision(revision);
                    return;
                case Value.MALICIOUS_SCORE_TYPE:
                    var type = Converter.hexToInt(valueObject.getString("type", null));
                    processMaliciousProposal(Converter.strToAddress(valueObject.getString("address", null)), type);
                    continue;
                case Value.PREP_DISQUALIFICATION_TYPE:
                    disqualifyPRep(Converter.strToAddress(valueObject.getString("address", null)));
                    continue;
                case Value.STEP_PRICE_TYPE:
                    var price = Converter.hexToInt(valueObject.getString("price", ""));
                    setStepPrice(price);
                    continue;
                case Value.STEP_COSTS_TYPE:
                    var stepCosts = Value.StepCosts.fromJson(valueObject.get("costs").asObject());
                    for (Value.StepCosts.StepCost s : stepCosts.costs) {
                        setStepCosts(s.type, s.cost);
                    }
                    continue;
                case Value.REWARD_FUND_TYPE:
                    var iglobal = Converter.hexToInt(valueObject.getString("iglobal", null));
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
                        if (rewardRateInfo.type.equals(Value.RewardFunds.I_PREP)) iprep = rewardRateInfo.value;
                        if (rewardRateInfo.type.equals(Value.RewardFunds.I_CPS)) icps = rewardRateInfo.value;
                        if (rewardRateInfo.type.equals(Value.RewardFunds.I_RELAY))
                            irelay = rewardRateInfo.value;
                        if (rewardRateInfo.type.equals(Value.RewardFunds.I_VOTER))
                            ivoter = rewardRateInfo.value;
                    }
                    setRewardFundsRate(iprep, icps, irelay, ivoter);
                    continue;
                case Value.NETWORK_SCORE_DESIGNATION_TYPE: {
                    var networkScores = valueObject.get("networkScores").asArray();
                    int l = networkScores.size();
                    for (int j = 0; j < l; j++) {
                        var v = networkScores.get(j).asObject();
                        String role = v.getString("role", null);
                        Address address = Converter.strToAddress(v.getString("address", null));
                        chainScore.setNetworkScore(role, address);
                        if (address != null) NetWorkScoreDesignated(role, address);
                        else NetWorkScoreDeallocated(role);
                    }
                    continue;
                }
                case Value.NETWORK_SCORE_UPDATE_TYPE:
                    Address addr = Converter.strToAddress(valueObject.getString("address", null));
                    var content = Converter.hexToBytes(valueObject.getString("content", null));
                    Context.deploy(addr, content);
                    NetWorkScoreUpdated(addr);
                    continue;
                case Value.ACCUMULATED_VALIDATION_FAILURE_PENALTY: {
                    var rate = Converter.hexToInt(valueObject.getString("slashingRate", null));
                    chainScore.setConsistentValidationSlashingRate(rate);
                    continue;
                }
                case Value.MISSED_NETWORK_PROPOSAL_PENALTY: {
                    var rate = Converter.hexToInt(valueObject.getString("slashingRate", null));
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
            switch (name) {
                case Value.TEXT_TYPE:
                    Context.require(value.getString("text", null) != null);
                    continue;
                case Value.REVISION_TYPE:
                    var revision = Converter.hexToInt(value.getString("revision", null));
                    validateRevision(revision);
                    continue;
                case Value.STEP_COSTS_TYPE:
                    Value.StepCosts.fromJson(value.get("costs").asObject());
                    continue;
                case Value.MALICIOUS_SCORE_TYPE:
                    var type = Converter.hexToInt(value.getString("type", null));
                    validateMaliciousScore(Converter.strToAddress(value.getString("address", null)), type.intValue());
                    continue;
                case Value.PREP_DISQUALIFICATION_TYPE:
                    validateDisqualifyPRep(Converter.strToAddress(value.getString("address", null)));
                    continue;
                case Value.STEP_PRICE_TYPE:
                    var price = Converter.hexToInt(value.getString("price", null));
                    validateStepPRice(price);
                    continue;
                case Value.REWARD_FUND_TYPE:
                    var iglobal = Converter.hexToInt(value.getString("iglobal", null));
                    chainScore.validateRewardFund(iglobal);
                    continue;
                case Value.REWARD_FUNDS_ALLOCATION:
                    var funds = Value.RewardFunds.fromJson(value.get("rewardFunds").asObject());
                    validateRewardFundsRate(funds);
                    continue;
                case Value.NETWORK_SCORE_DESIGNATION_TYPE:
                    validateDesignationProposal(value);
                    continue;
                case Value.NETWORK_SCORE_UPDATE_TYPE:
                    Converter.strToAddress(value.getString("address", null));
                    Converter.hexToBytes(value.getString("content", null));
                    continue;
                case Value.ACCUMULATED_VALIDATION_FAILURE_PENALTY:
                case Value.MISSED_NETWORK_PROPOSAL_PENALTY:
                    var slashingRate = Converter.hexToInt(value.getString("slashingRate", null));
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
        PRepInfo[] mainPreps = chainScore.getMainPRepsInfo();
        PRepInfo[] subPreps = chainScore.getSubPRepsInfo();
        Context.require(getPRepInfoFromList(address, mainPreps) != null || getPRepInfoFromList(address, subPreps) != null,
                address.toString() + "is not p-rep");
    }

    private void validateStepPRice(BigInteger price) {
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
            if (BigInteger.ZERO.compareTo(value.value) > 0) {
                Context.revert("reward fund < 0");
            }
            sum = sum.add(value.value);
        }
        Context.require(sum.compareTo(BigInteger.valueOf(100)) == 0, "sum of reward funds must be 100");
    }

    private void validateDesignationProposal(JsonObject value) {
        var networkScores = value.get("networkScores").asArray();
        int length = networkScores.size();
        for (int i = 0; i < length; i++) {
            var v = networkScores.get(i).asObject();
            String role = v.getString("role", null);
            Address address = Converter.strToAddress(v.getString("address", null));
            var ret = role.equals(Value.CPS_SCORE) || role.equals(Value.RELAY_SCORE) || role.equals(Value.GOVERNANCE_SCORE);
            Context.require(ret, "invalid network SCORE role : " + role);
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
    public void NetWorkScoreUpdated(Address address) {}

    @EventLog(indexed=1)
    public void NetWorkScoreDesignated(String role, Address address) {}

    @EventLog(indexed=1)
    public void NetWorkScoreDeallocated(String role) {}

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

}
