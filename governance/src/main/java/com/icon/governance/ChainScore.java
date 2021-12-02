package com.icon.governance;

import score.Address;
import score.Context;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

class PRepInfo {
    private final String name;
    private final Address address;
    private final BigInteger delegated;
    private final BigInteger power;

    public PRepInfo(
            String name,
            Address address,
            BigInteger delegated,
            BigInteger power
    ) {
        this.name = name;
        this.address = address;
        this.delegated = delegated;
        this.power = power;
    }

    public String getName() {
        return name;
    }

    public Address getAddress() {
        return address;
    }

    public BigInteger getDelegated() {
        return delegated;
    }

    public BigInteger power() {
        return power;
    }
}

class ChainScore {
    static final Address CHAIN_SCORE = Address.fromString("cx0000000000000000000000000000000000000000");

    private void validateHash(byte[] value) {
        Context.require(value.length == 32);
    }

    void setRevision(BigInteger code) {
        Context.call(CHAIN_SCORE, "setRevision", code);
    }

    BigInteger getRevision() {
        return (BigInteger) Context.call(CHAIN_SCORE, "getRevision");
    }

    void setStepPrice(BigInteger price) {
        Context.call(CHAIN_SCORE, "setStepPrice", price);
    }

    BigInteger getStepPrice() {
        return (BigInteger) Context.call(CHAIN_SCORE, "getStepPrice");
    }

    Map<String, Object> getStepCosts() {
        return (Map<String, Object>) Context.call(CHAIN_SCORE, "getStepCosts");
    }

    BigInteger getMaxStepLimit(String t) {
        return (BigInteger) Context.call(CHAIN_SCORE, "getMaxStepLimit", t);
    }

    Map<String, Object> getScoreStatus(Address address) {
        return (Map<String, Object>) Context.call(CHAIN_SCORE, "getScoreStatus", address);
    }

    List<Address> getBlockedScores() {
        return (List<Address>) Context.call(CHAIN_SCORE, "getBlockedScores");
    }

    void setStepCost(String type, BigInteger cost) {
        Context.call(CHAIN_SCORE, "setStepCost", type, cost);
    }

    void disqualifyPRep(Address address) {
        Context.call(CHAIN_SCORE, "disqualifyPRep", address);
    }

    void setIRep(BigInteger irep) {
        Context.call(CHAIN_SCORE, "setIRep", irep);
    }

    void acceptScore(byte[] txHash) {
        validateHash(txHash);
        Context.call(CHAIN_SCORE, "acceptScore", txHash);
    }

    void rejectScore(byte[] txHash, String reason) {
        validateHash(txHash);
        Context.call(CHAIN_SCORE, "rejectScore", txHash, reason);
    }

    void addTimer(BigInteger blockHeight) {
        Context.call(CHAIN_SCORE, "addTimer", blockHeight);
    }

    void removeTimer(BigInteger blockHeight) {
        Context.call(CHAIN_SCORE, "removeTimer", blockHeight);
    }

    void penalizeNonvoters(List<Address> preps) {
        Context.call(CHAIN_SCORE, "penalizeNonvoters", preps);
    }

    void blockScore(Address address) {
        Context.call(CHAIN_SCORE, "blockScore", address);
    }

    void unblockScore(Address address) {
        Context.call(CHAIN_SCORE, "unblockScore", address);
    }

    void validateIRep(BigInteger irep) {
        Context.call(CHAIN_SCORE, "validateIRep", irep);
    }

    void validateRewardFund(BigInteger rewardFund) {
        Context.call(CHAIN_SCORE, "validateRewardFund", rewardFund);
    }

    void setRewardFund(BigInteger rewardFund) {
        Context.call(CHAIN_SCORE, "setRewardFund", rewardFund);
    }

    void setRewardFundsRate(BigInteger iprep, BigInteger icps, BigInteger irelay, BigInteger ivoter) {
        Context.call(CHAIN_SCORE, "setRewardFundAllocation", iprep, icps, irelay, ivoter);
    }

    void setNetworkScore(String role, Address address) {
        Context.call(CHAIN_SCORE, "setNetworkScore", role, address);
    }

    Map<String, Object> getMainPReps() {
        return (Map<String, Object>) Context.call(CHAIN_SCORE, "getMainPReps");
    }

    Map<String, Object> getSubPReps() {
        return (Map<String, Object>) Context.call(CHAIN_SCORE, "getSubPReps");
    }

    Map<String, Object> getPRepTerm() {
        return (Map<String, Object>) Context.call(CHAIN_SCORE, "getPRepTerm");
    }

    PRepInfo[] getMainPRepsInfo() {
        Map<String, Object> mainPreps = getMainPReps();
        return getPRepInfolist(mainPreps);
    }

    PRepInfo[] getSubPRepsInfo() {
        Map<String, Object> subPReps = getSubPReps();
        return getPRepInfolist(subPReps);
    }

    private PRepInfo[] getPRepInfolist(Map<String, Object> preps) {
        List<Map<String, Object>> info = (List<Map<String, Object>>) preps.get("preps");

        PRepInfo[] prepInfo = new PRepInfo[info.size()];
        for (int i = 0; i < info.size(); i++) {
            Map<String, Object> item = info.get(i);
            PRepInfo itemInfo = new PRepInfo(
                    (String) item.get("name"),
                    (Address) item.get("address"),
                    (BigInteger) item.get("delegated"),
                    (BigInteger) item.get("power")
            );

            prepInfo[i] = itemInfo;
        }

        return prepInfo;
    }

}
