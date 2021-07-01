package com.icon.governance;

import score.Address;
import score.Context;

import java.math.BigInteger;
import java.util.Map;
import java.util.List;

class PRepInfo {
    private final String name;
    private final Address address;
    private final BigInteger delegated;
    private final BigInteger bondedDelegation;

    public PRepInfo(
            String name,
            Address address,
            BigInteger delegated,
            BigInteger bondedDelegation
    ) {
        this.name = name;
        this.address = address;
        this.delegated = delegated;
        this.bondedDelegation = bondedDelegation;
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

    public BigInteger getBondedDelegation() {
        return bondedDelegation;
    }
}

class ChainScore {
    private static final Address CHAIN_SCORE = Address.fromString("cx0000000000000000000000000000000000000000");

    private void validateHash(byte[] value) {
        Context.require(value.length == 32);
    }

    public void setRevision(int code) {
        Context.call(CHAIN_SCORE, "setRevision", code);
    }

    public void setStepPrice(BigInteger price) {
        Context.call(CHAIN_SCORE, "setStepPrice", price);
    }

    public void setStepCost(String type, BigInteger cost) {
        Context.call(CHAIN_SCORE, "setStepCost", type, cost);
    }

    public void acceptScore(byte[] txHash) {
        validateHash(txHash);
        Context.call(CHAIN_SCORE, "acceptScore", (Object) txHash);
    }

    public void rejectScore(byte[] txHash) {
        validateHash(txHash);
        Context.call(CHAIN_SCORE, "rejectScore", (Object) txHash);
    }

    // icon.chainscore_iiss... 에 구현
    public PRepInfo[] getMainPRepsInfo() {
        Map<String, Object> mainPreps = this.getMainPReps();
        List<Map<String, Object>> info = (List<Map<String, Object>>) mainPreps.get("preps");

        PRepInfo[] prepInfo = new PRepInfo[info.size()];
        // name, address, delegated, bondedDelegation.
        for (int i = 0; i < info.size(); i++) {
            Map<String, Object> item = info.get(i);
            PRepInfo itemInfo = new PRepInfo(
                    (String) item.get("name"),
                    (Address) item.get("address"),
                    (BigInteger) item.get("delegated"),
                    (BigInteger) item.get("bondedDelegation")
            );

            prepInfo[i] = itemInfo;
        }

        return prepInfo;
    }

    private Map<String, Object> getMainPReps() {
        return (Map<String, Object>) Context.call(CHAIN_SCORE, "getMainPReps");
    }

    public Map<String, Object> getPRepTerm() {
        return (Map<String, Object>) Context.call(CHAIN_SCORE, "getPRepTerm");
    }
}
