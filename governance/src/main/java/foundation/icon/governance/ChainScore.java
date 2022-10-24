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
import score.Context;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

class PRepInfo {
    public final static BigInteger GRADE_MAIN = BigInteger.ZERO;
    public final static BigInteger GRADE_SUB = BigInteger.ONE;
    public final static BigInteger STATUS_ACTIVE = BigInteger.ZERO;
    private final String name;
    private final Address address;
    private final BigInteger power;
    private final BigInteger grade;
    private final BigInteger status;

    public PRepInfo(
            String name,
            Address address,
            BigInteger power,
            BigInteger grade,
            BigInteger status
    ) {
        this.name = name;
        this.address = address;
        this.power = power;
        this.grade = grade;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public Address getAddress() {
        return address;
    }

    public BigInteger power() {
        return power;
    }

    public BigInteger getGrade() {
        return grade;
    }

    public BigInteger getStatus() {
        return status;
    }

}

class ChainScore {
    static final Address CHAIN_SCORE = Address.fromString("cx0000000000000000000000000000000000000000");

    static private void validateHash(byte[] value) {
        Context.require(value.length == 32);
    }

    static void setRevision(BigInteger code) {
        Context.call(CHAIN_SCORE, "setRevision", code);
    }

    static BigInteger getRevision() {
        return (BigInteger) Context.call(CHAIN_SCORE, "getRevision");
    }

    static void setStepPrice(BigInteger price) {
        Context.call(CHAIN_SCORE, "setStepPrice", price);
    }

    static BigInteger getStepPrice() {
        return (BigInteger) Context.call(CHAIN_SCORE, "getStepPrice");
    }

    static Map<String, Object> getStepCosts() {
        return (Map<String, Object>) Context.call(CHAIN_SCORE, "getStepCosts");
    }

    static BigInteger getMaxStepLimit(String t) {
        return (BigInteger) Context.call(CHAIN_SCORE, "getMaxStepLimit", t);
    }

    static Map<String, Object> getScoreStatus(Address address) {
        return (Map<String, Object>) Context.call(CHAIN_SCORE, "getScoreStatus", address);
    }

    static List<Address> getBlockedScores() {
        return (List<Address>) Context.call(CHAIN_SCORE, "getBlockedScores");
    }

    static void setStepCost(String type, BigInteger cost) {
        Context.call(CHAIN_SCORE, "setStepCost", type, cost);
    }

    static void disqualifyPRep(Address address) {
        Context.call(CHAIN_SCORE, "disqualifyPRep", address);
    }

    static void acceptScore(byte[] txHash) {
        validateHash(txHash);
        Context.call(CHAIN_SCORE, "acceptScore", (Object) txHash);
    }

    static void rejectScore(byte[] txHash) {
        validateHash(txHash);
        Context.call(CHAIN_SCORE, "rejectScore", (Object) txHash);
    }

    static void addTimer(BigInteger blockHeight) {
        Context.call(CHAIN_SCORE, "addTimer", blockHeight);
    }

    static void removeTimer(BigInteger blockHeight) {
        Context.call(CHAIN_SCORE, "removeTimer", blockHeight);
    }

    static void penalizeNonvoters(List<Address> preps) {
        Context.call(CHAIN_SCORE, "penalizeNonvoters", preps);
    }

    static void blockScore(Address address) {
        Context.call(CHAIN_SCORE, "blockScore", address);
    }

    static void unblockScore(Address address) {
        Context.call(CHAIN_SCORE, "unblockScore", address);
    }

    static void validateRewardFund(BigInteger rewardFund) {
        Context.call(CHAIN_SCORE, "validateRewardFund", rewardFund);
    }

    static void setRewardFund(BigInteger rewardFund) {
        Context.call(CHAIN_SCORE, "setRewardFund", rewardFund);
    }

    static void setRewardFundsRate(BigInteger iprep, BigInteger icps, BigInteger irelay, BigInteger ivoter) {
        Context.call(CHAIN_SCORE, "setRewardFundAllocation", iprep, icps, irelay, ivoter);
    }

    static Address getScoreOwner(Address address) {
        return (Address)Context.call(CHAIN_SCORE, "getScoreOwner", address);
    }

    static void burn(BigInteger value) {
        Context.call(value, CHAIN_SCORE, "burn");
    }

    static void setNetworkScore(String role, Address address) {
        Context.call(CHAIN_SCORE, "setNetworkScore", role, address);
    }

    static void setConsistentValidationSlashingRate(BigInteger rate) {
        Context.call(CHAIN_SCORE, "setConsistentValidationSlashingRate", rate);
    }

    static void setNonVoteSlashingRate(BigInteger rate) {
        Context.call(CHAIN_SCORE, "setNonVoteSlashingRate", rate);
    }

    static Map<String, Object> getMainPReps() {
        return (Map<String, Object>) Context.call(CHAIN_SCORE, "getMainPReps");
    }

    static Map<String, Object> getPRep(Address address) {
        return (Map<String, Object>) Context.call(CHAIN_SCORE, "getPRep", address);
    }

    static Map<String, Object> getPRepTerm() {
        return (Map<String, Object>) Context.call(CHAIN_SCORE, "getPRepTerm");
    }

    static BigInteger getExpireVotingHeight() {
        var term = ChainScore.getPRepTerm();

        /*
            currentTermEnd: endBlockHeight
            4-terms: termPeriod * 4
            currentTermEnd + 4terms = 5terms
         */
        BigInteger expireVotingHeight = (BigInteger) term.get("period");
        expireVotingHeight = expireVotingHeight.multiply(BigInteger.valueOf(4));
        return expireVotingHeight.add((BigInteger) term.get("endBlockHeight"));
    }

    static PRepInfo[] getMainPRepsInfo() {
        Map<String, Object> mainPreps = getMainPReps();
        return getPRepInfolist(mainPreps);
    }

    static PRepInfo getPrepInfo(Address address) {
        try{
            Map<String, Object> prep = getPRep(address);
            PRepInfo prepInfo = new PRepInfo(
                    (String) prep.get("name"),
                    (Address) prep.get("address"),
                    (BigInteger) prep.get("power"),
                    (BigInteger) prep.get("grade"),
                    (BigInteger) prep.get("status")
            );
            if (prepInfo.getStatus().compareTo(PRepInfo.STATUS_ACTIVE) != 0) return null;
            return prepInfo;
        } catch (Exception e) {
            return null;
        }
    }

    static private PRepInfo[] getPRepInfolist(Map<String, Object> preps) {
        List<Map<String, Object>> info = (List<Map<String, Object>>) preps.get("preps");

        PRepInfo[] prepInfo = new PRepInfo[info.size()];
        for (int i = 0; i < info.size(); i++) {
            Map<String, Object> item = info.get(i);
            PRepInfo itemInfo = new PRepInfo(
                    (String) item.get("name"),
                    (Address) item.get("address"),
                    (BigInteger) item.get("power"),
                    null,
                    null
            );

            prepInfo[i] = itemInfo;
        }

        return prepInfo;
    }

}
