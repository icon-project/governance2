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
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class CallRequest {
    final static String SET_REVISION = "setRevision";
    final static String UNBLOCK_SCORE = "unblockScore";
    final static String BLOCK_SCORE = "blockScore";
    final static String DISQUALIFY_PREP = "disqualifyPRep";
    final static String SET_STEP_PRICE = "setStepPrice";
    final static String SET_STEP_COST = "setStepCost";
    final static String SET_REWARD_FUND = "setRewardFund";
    final static String SET_REWARD_FUND_ALLOCATION = "setRewardFundAllocation";
    final static String SET_NETWORK_SCORE = "setNetworkScore";
    final static String UPDATE_NETWORK_SCORE = "updateNetworkScore";
    final static String CONSISTENT_VALIDATION_PENALTY = "setConsistentValidationSlashingRate";
    final static String NON_VOTE_SLASHING_RATE = "setNonVoteSlashingRate";
    private Address to;
    private String method;
    private Param[] params;

    public void setTo(Address to) {
        this.to = to;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setParams(Param[] params) {
        this.params = params;
    }

    public static void writeObject(ObjectWriter w, CallRequest m) {
        w.beginList(3);
        w.write(m.to);
        w.write(m.method);
        w.beginList(m.params.length);
        for (int i = 0; i < m.params.length; i++) {
            w.write(m.params[i]);
        }
        w.end();
        w.end();
    }

    public static CallRequest readObject(ObjectReader r) {
        r.beginList();
        var to = r.readAddress();
        var method = r.readString();
        r.beginList();
        ArrayList<Param> paramList = new ArrayList<>();
        while (r.hasNext()) {
            paramList.add(r.read(Param.class));
        }
        Param[] params = new Param[paramList.size()];
        for (int i = 0; i < params.length; i++) {
            params[i] = paramList.get(i);
        }
        r.end();
        r.end();
        var cr = new CallRequest();
        cr.setParams(params);
        cr.setMethod(method);
        cr.setTo(to);
        return cr;
    }

    public Object[] getParams() {
        Object[] pArray = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            pArray[i] = params[i].getParam();
        }
        return pArray;
    }

    public Map<String, Object> toMap() {
        ArrayList<Map<String, Object>> paramList = new ArrayList<>();
        for (Param p : params) {
            paramList.add(p.toMap());
        }
        return Map.of(
                "to", to,
                "method", method,
                "params", List.of(paramList.toArray())
        );
    }

    public void validateRequest() {
        var isSystemMethod = to.equals(ChainScore.ADDRESS);
        var isGovernanceMethod = to.equals(Governance.address);
        if (!isSystemMethod && !isGovernanceMethod) return;
        var ps = getParams();
        if (isSystemMethod) {
            switch (method) {
                case SET_REVISION:
                    validateRevision(ps);
                    break;
                case UNBLOCK_SCORE:
                    Context.require(ps[0] instanceof Address);
                    break;
                case BLOCK_SCORE:
                    validateBlockScore(ps);
                    break;
                case DISQUALIFY_PREP:
                    validateDisqualifyPRep(ps);
                    break;
                case SET_STEP_PRICE:
                    validateStepPrice(ps);
                    break;
                case SET_REWARD_FUND:
                    ChainScore.validateRewardFund((BigInteger) ps[0]);
                    break;
                case SET_REWARD_FUND_ALLOCATION:
                    validateRewardFundsRate(ps);
                    break;
                case SET_NETWORK_SCORE:
                    validateSetNetworkScore(ps);
                    break;
                case SET_STEP_COST:
                    Context.require(ps.length == 2);
                    Context.require(ps[0] instanceof String && ps[1] instanceof BigInteger);
                    break;
                case CONSISTENT_VALIDATION_PENALTY:
                case NON_VOTE_SLASHING_RATE:
                    Context.require(ps[0] instanceof BigInteger);
                    break;
            }
        } else {
            // In this version, governance custom call can only call updateNetworkScore()
            Context.require(2 == ps.length || ps.length ==3);
            Context.require(ps[0] instanceof Address && ps[1] instanceof byte[]);
            var owner = ChainScore.getScoreOwner((Address) ps[0]);
            Context.require(owner.equals(Governance.address));
            if (ps.length == 3) Context.require(ps[2] instanceof String[]);
        }
    }

    public void handleRequest(Governance governance) {
        var ps = getParams();
        if (!to.equals(Governance.address)) {
            if (method.equals(DISQUALIFY_PREP)) {
                disqualifyPrep((Address) ps[0], governance);
                return;
            }
            Context.call(to, method, ps);
            switch (method) {
                case SET_REVISION:
                    governance.RevisionChanged((BigInteger) ps[0]);
                    break;
                case UNBLOCK_SCORE:
                    Context.require(ps[0] instanceof Address);
                    governance.MaliciousScore((Address) ps[0], 1);
                    break;
                case BLOCK_SCORE:
                    validateBlockScore(ps);
                    governance.MaliciousScore((Address) ps[0], 0);
                    break;
                case SET_STEP_PRICE:
                    governance.StepPriceChanged((BigInteger) ps[0]);
                    break;
                case SET_REWARD_FUND:
                    governance.RewardFundSettingChanged((BigInteger) ps[0]);
                    break;
                case SET_REWARD_FUND_ALLOCATION:
                    governance.RewardFundAllocationChanged((BigInteger) ps[0], (BigInteger) ps[1], (BigInteger) ps[2], (BigInteger) ps[3]);
                    break;
                case SET_NETWORK_SCORE:
                    if (ps[1] == null) {
                        governance.NetworkScoreDeallocated((String)ps[0]);
                    } else {
                        governance.NetworkScoreDesignated((String)ps[0], (Address) ps[1]);
                    }
                    break;
                case SET_STEP_COST:
                    governance.StepCostChanged((String) ps[0], (BigInteger) ps[1]);
                    break;
                case CONSISTENT_VALIDATION_PENALTY:
                case NON_VOTE_SLASHING_RATE:
                    Context.require(ps[0] instanceof BigInteger);
                    break;
            }
            return;
        }
        if (method.equals(UPDATE_NETWORK_SCORE)) {
            String[] scoreParams = null;
            if (ps.length == 3) {
                var c = (ArrayList<String>)ps[2];
                scoreParams = new String[c.size()];
                for (int i = 0; i < scoreParams.length; i++) scoreParams[i] = c.get(i);
            }
            var address = (Address) ps[0];
            governance.deployScore(address, (byte[]) ps[1], scoreParams);
            governance.NetworkScoreUpdated(address);
        }
    }

    private void disqualifyPrep(Address address, Governance governance) {
        try {
            Context.call(to, method, address);
            governance.PRepDisqualified(address, true, "");
        } catch (Exception e) {
            governance.PRepDisqualified(address, false, e.getMessage());
        }
    }

    private void validateRevision(Object... values) {
        Context.require(values.length == 1);
        var prev = ChainScore.getRevision();
        var revision = (BigInteger)values[0];
        Context.require(revision.compareTo(prev) > 0, "can not decrease revision");
    }

    private void validateBlockScore(Object... values) {
        Context.require(values.length == 1);
        var address = (Address) values[0];
        if (address.equals(Governance.address)) Context.revert("Can not freeze governance SCORE");
    }

    private void validateDisqualifyPRep(Object... values) {
        Context.require(values.length == 1);
        var address = (Address) values[0];
        Context.require(ChainScore.getPRepInfoFromList(address) != null, address.toString() + " is not p-rep");
    }

    private void validateStepPrice(Object... values) {
        Context.require(values.length == 1);
        var price = (BigInteger) values[0];
        var prevPrice = ChainScore.getStepPrice();
        var hundred = BigInteger.valueOf(100);
        var max = prevPrice.multiply(BigInteger.valueOf(125)).divide(hundred);
        var min = prevPrice.multiply(BigInteger.valueOf(75)).divide(hundred);
        Context.require(price.compareTo(min) >= 0 && price.compareTo(max) <= 0, "Invalid step price: " + price);
    }

    private void validateRewardFundsRate(Object... values) {
        Context.require(values.length == 4, "InvalidFundType");
        var sum = BigInteger.ZERO;
        for (Object value : values) {
            var rate = (BigInteger) value;
            if (BigInteger.ZERO.compareTo(rate) > 0) {
                Context.revert("reward fund < 0");
            }
            sum = sum.add(rate);
        }
        Context.require(sum.compareTo(BigInteger.valueOf(100)) == 0, "sum of reward funds must be 100");
    }

    private void validateSetNetworkScore(Object... values) {
        var role = (String) values[0];
        var address = (Address) values[1];
        Context.require(Value.CPS_SCORE.equals(role) || Value.RELAY_SCORE.equals(role),
                "Invalid network SCORE role: " + role);
        if (address == null) return;
        Address owner = ChainScore.getScoreOwner(address);
        Context.require(owner.equals(Governance.address), "Only owned by governance can be designated");
    }
}
