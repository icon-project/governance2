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
    static final Address ADDRESS = Address.fromString("cx0000000000000000000000000000000000000000");

    private void validateHash(byte[] value) {
        Context.require(value.length == 32);
    }

    static BigInteger getRevision() {
        return (BigInteger) Context.call(ADDRESS, "getRevision");
    }

    static BigInteger getStepPrice() {
        return (BigInteger) Context.call(ADDRESS, "getStepPrice");
    }

    List<Address> getBlockedScores() {
        return (List<Address>) Context.call(ADDRESS, "getBlockedScores");
    }

    void acceptScore(byte[] txHash) {
        validateHash(txHash);
        Context.call(ADDRESS, "acceptScore", (Object) txHash);
    }

    void rejectScore(byte[] txHash) {
        validateHash(txHash);
        Context.call(ADDRESS, "rejectScore", (Object) txHash);
    }

    void addTimer(BigInteger blockHeight) {
        Context.call(ADDRESS, "addTimer", blockHeight);
    }

    void removeTimer(BigInteger blockHeight) {
        Context.call(ADDRESS, "removeTimer", blockHeight);
    }

    void penalizeNonvoters(List<Address> preps) {
        Context.call(ADDRESS, "penalizeNonvoters", preps);
    }

    static void validateRewardFund(BigInteger rewardFund) {
        Context.call(ADDRESS, "validateRewardFund", rewardFund);
    }

    static Address getScoreOwner(Address address) {
        return (Address)Context.call(ADDRESS, "getScoreOwner", address);
    }

    void burn(BigInteger value) {
        Context.call(value, ADDRESS, "burn");
    }

    Map<String, Object> getMainPReps() {
        return (Map<String, Object>) Context.call(ADDRESS, "getMainPReps");
    }

    static Map<String, Object> getPReps() {
        return (Map<String, Object>) Context.call(ADDRESS, "getPReps");
    }

    Map<String, Object> getPRepTerm() {
        return (Map<String, Object>) Context.call(ADDRESS, "getPRepTerm");
    }

    PRepInfo[] getMainPRepsInfo() {
        Map<String, Object> mainPreps = getMainPReps();
        return getPRepInfolist(mainPreps);
    }

    static PRepInfo[] getPRepsInfo() {
        Map<String, Object> preps = getPReps();
        return getPRepInfolist(preps);
    }

    static private PRepInfo[] getPRepInfolist(Map<String, Object> preps) {
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
