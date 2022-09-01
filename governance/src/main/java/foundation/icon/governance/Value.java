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

import com.eclipsesource.json.JsonObject;
import score.Address;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class Value {
    private final int proposalType;
    private String stringValue;
    private Address address;
    private BigInteger type;
    private BigInteger value;
    private StepCosts stepCosts;
    private RewardFunds rewardFunds;
    public static final String CPS_SCORE = "cps";
    public static final String RELAY_SCORE = "relay";
    private byte[] data;
    private Requests requests;

    public Value(int p, String text) {
        // Text
        this.stringValue = text;
        this.proposalType = p;
    }

    public Value(int p, Address address, BigInteger type) {
        // MaliciousScore
        this.address = address;
        this.type = type;
        this.proposalType = p;
    }

    public Value(int p, Address address) {
        // PRepDisQualification
        this.address = address;
        this.proposalType = p;
    }

    public Value(int p, BigInteger value) {
        // StepPrice, IRep, Revision, RewardFund
        this.value = value;
        this.proposalType = p;
    }

    public Value(int p, Address address, byte[] data) {
        // Network SCORE update
        this.proposalType = p;
        this.data = data;
        this.address = address;
    }

    public Value(int p, String text, Address address) {
        // Network SCORE designation
        this.proposalType = p;
        this.address = address;
        this.stringValue = text;
    }

    public Value(int p, StepCosts value) {
        this.proposalType = p;
        this.stepCosts = value;
    }

    public Value(int p, RewardFunds value) {
        this.proposalType = p;
        this.rewardFunds = value;
    }

    public Value(int p, byte[] data) {
        this.proposalType = p;
        this.data = data;
    }

    public Value(int p, Requests callRequests) {
        this.proposalType = p;
        this.requests = callRequests;
    }

    public static void writeObject(ObjectWriter w, Value v) {
        w.beginList(v.size());
        v.set(w);
        w.end();
    }

    public static Value readObject(ObjectReader r) {
        r.beginList();
        int proposalType = r.readInt();
        Value v = Value.make(proposalType, r);
        r.end();
        return v;
    }

    public Requests getRequests() {
        return requests;
    }

    private void set(ObjectWriter w) {
        w.write(proposalType);
        switch (proposalType) {
            case Proposal.TEXT:
                w.write(stringValue);
                return;
            case Proposal.MALICIOUS_SCORE:
                w.write(address);
                w.write(type);
                return;
            case Proposal.PREP_DISQUALIFICATION:
                w.write(address);
                return;
            case Proposal.REVISION:
            case Proposal.STEP_PRICE:
            case Proposal.IREP:
            case Proposal.REWARD_FUND:
                w.write(value);
                return;
            case Proposal.STEP_COSTS:
                w.write(stepCosts);
                return;
            case Proposal.REWARD_FUNDS_ALLOCATION:
                w.write(rewardFunds);
                return;
            case Proposal.NETWORK_PROPOSAL:
                w.write(data);
        }
    }

    public int size() {
        switch (proposalType) {
            case Proposal.TEXT:
            case Proposal.REVISION:
            case Proposal.PREP_DISQUALIFICATION:
            case Proposal.STEP_PRICE:
            case Proposal.IREP:
            case Proposal.STEP_COSTS:
            case Proposal.REWARD_FUNDS_ALLOCATION:
            case Proposal.REWARD_FUND:
            case Proposal.NETWORK_PROPOSAL:
                return 2;
            case Proposal.MALICIOUS_SCORE:
                return 3;
        }
        throw new IllegalArgumentException("proposalType not exist");
    }

    public static Value make(int proposalType, ObjectReader r) {
        switch (proposalType) {
            case Proposal.TEXT:
                return new Value(proposalType, r.readString());
            case Proposal.PREP_DISQUALIFICATION:
                return new Value(proposalType, r.readAddress());
            case Proposal.MALICIOUS_SCORE:
                return new Value(proposalType, r.readAddress(), r.readBigInteger());
            case Proposal.REVISION:
            case Proposal.STEP_PRICE:
            case Proposal.IREP:
            case Proposal.REWARD_FUND:
                return new Value(proposalType, r.readBigInteger());
            case Proposal.STEP_COSTS:
                return new Value(proposalType, r.read(StepCosts.class));
            case Proposal.REWARD_FUNDS_ALLOCATION:
                return new Value(proposalType, r.read(RewardFunds.class));
            case Proposal.NETWORK_PROPOSAL:
                return new Value(proposalType, r.readByteArray());
            case Proposal.EXTERNAL_CALL:
                return new Value(proposalType, r.read(Requests.class));
            default:
                throw new IllegalArgumentException("proposalType not exist");
        }
    }

    public static Value fromJson(int type, JsonObject value) {
        switch (type) {
            case Proposal.TEXT:
                return new Value(type, value.getString("value", null));
            case Proposal.MALICIOUS_SCORE:
                return new Value(
                        type,
                        Converter.toAddress(value.getString("address", null)),
                        Converter.toInteger(value.getString("type", null))
                );
            case Proposal.PREP_DISQUALIFICATION:
                return new Value(type, Converter.toAddress(value.getString("address", null)));
            case Proposal.REVISION:
                var code = value.getString("code", null);
                BigInteger revision;
                if (code != null) {
                    revision = Converter.toInteger(code);
                } else {
                    revision = Converter.toInteger(value.getString("value", null));
                }
                return new Value(type, revision);
            case Proposal.STEP_PRICE:
            case Proposal.IREP:
                return new Value(type, Converter.toInteger(value.getString("value", null)));
            case Proposal.STEP_COSTS:
                return new Value(type, StepCosts.fromJson(value.get("costs").asObject()));
            case Proposal.REWARD_FUND:
                return new Value(type, Converter.toInteger(value.getString("iglobal", null)));
            case Proposal.REWARD_FUNDS_ALLOCATION:
                return new Value(type, RewardFunds.fromJson(value.get("rewardFunds").asObject()));
        }
        throw new IllegalArgumentException("Invalid value type");
    }

    public Map<String, Object> toMap() {
        switch (proposalType) {
            case Proposal.TEXT:
                return Map.of("text", stringValue);
            case Proposal.MALICIOUS_SCORE:
                return Map.of("address", address, "type", type);
            case Proposal.PREP_DISQUALIFICATION:
                return Map.of("address", address);
            case Proposal.REVISION:
                return Map.of("revision", value);
            case Proposal.STEP_PRICE:
                return Map.of("stepPrice", value);
            case Proposal.IREP:
                return Map.of("irep", value);
            case Proposal.REWARD_FUND:
                return Map.of("iglobal", value);
            case Proposal.STEP_COSTS:
                return stepCosts.toMap();
            case Proposal.REWARD_FUNDS_ALLOCATION:
                return rewardFunds.toMap();
            case Proposal.NETWORK_PROPOSAL:
                return Map.of("data", new String(data));
            case Proposal.EXTERNAL_CALL:
                return Map.of("requests", requests.toList());
        }
        throw new IllegalArgumentException("Invalid value type");
    }

    public static class StepCosts {
        final static String STEP_TYPE_SCHEMA = "schema";
        final static String STEP_TYPE_DEFAULT = "default";
        final static String STEP_TYPE_INPUT = "input";
        final static String STEP_TYPE_CONTRACT_CALL = "contractCall";
        final static String STEP_TYPE_CONTRACT_CREATE = "contractCreate";
        final static String STEP_TYPE_CONTRACT_UPDATE = "contractUpdate";
        final static String STEP_TYPE_CONTRACT_SET = "contractSet";
        final static String STEP_TYPE_GET = "get";
        final static String STEP_TYPE_SET = "set";
        final static String STEP_TYPE_DELETE = "delete";
        final static String STEP_TYPE_API_CALL = "apiCall";
        final static String STEP_TYPE_GET_BASE = "getBase";
        final static String STEP_TYPE_SET_BASE = "setBase";
        final static String STEP_TYPE_DELETE_BASE = "deleteBase";
        final static String STEP_TYPE_LOG_BASE = "logBase";
        final static String STEP_TYPE_LOG = "log";
        final static String STEP_TYPE_CONTRACT_DESTRUCT = "contractDestruct";
        final static String STEP_TYPE_REPLACE = "replace";
        final static String STEP_TYPE_EVENT_LOG = "eventLog";
        final static String[] STEP_COSTS = {
                STEP_TYPE_SCHEMA, STEP_TYPE_DEFAULT, STEP_TYPE_CONTRACT_CALL, STEP_TYPE_CONTRACT_CREATE,
                STEP_TYPE_CONTRACT_UPDATE, STEP_TYPE_CONTRACT_SET, STEP_TYPE_GET, STEP_TYPE_SET, STEP_TYPE_DELETE,
                STEP_TYPE_INPUT, STEP_TYPE_API_CALL, STEP_TYPE_GET_BASE, STEP_TYPE_SET_BASE, STEP_TYPE_DELETE_BASE,
                STEP_TYPE_LOG_BASE, STEP_TYPE_LOG, STEP_TYPE_CONTRACT_DESTRUCT, STEP_TYPE_REPLACE, STEP_TYPE_EVENT_LOG,
        };

        private final StepCost[] costs;

        public static class StepCost {
            private final String type;
            private final BigInteger cost;

            public StepCost(String key, BigInteger cost) {
                this.type = key;
                this.cost = cost;
            }

            public static void writeObject(ObjectWriter w, StepCost s) {
                w.beginList(2);
                w.write(s.type);
                w.write(s.cost);
                w.end();
            }

            public static StepCost readObject(ObjectReader r) {
                r.beginList();
                var s = new StepCost(r.readString(), r.readBigInteger());
                r.end();
                return s;
            }

        }

        public StepCosts(StepCost[] costs) {
            this.costs = costs;
        }

        public static StepCosts fromJson(JsonObject object) {
            var keys = object.names();
            var size = keys.size();
            StepCost[] stepCosts = new StepCost[size];
            for (int i = 0; i < size; i++) {
                var key = keys.get(i);
                Context.require(isValidStepType(key), key + " is not valid step type");
                var value = object.getString(key, "");
                var cost = Converter.toInteger(value);
                stepCosts[i] = new StepCost(key, cost);
            }
            return new StepCosts(stepCosts);
        }

        public static void writeObject(ObjectWriter w, StepCosts costs) {
            w.beginList(2);
            w.write(costs.costs.length);
            var stepCostList = costs.costs;
            for (StepCost s : stepCostList) {
                w.write(s);
            }
            w.end();
        }

        public static StepCosts readObject(ObjectReader r) {
            r.beginList();
            int size = r.readInt();
            StepCost[] stepCostList = new StepCost[size];

            for (int i = 0; i < size; i++) {
                StepCost s = r.read(StepCost.class);
                stepCostList[i] = s;
            }
            var stepCosts = new StepCosts(stepCostList);

            r.end();
            return stepCosts;
        }

        static boolean isValidStepType(String type) {
            for (String t : STEP_COSTS) {
                if (type.equals(t)) return true;
            }
            return false;
        }

        public Map<String, Object> toMap() {
            var length = costs.length;
            Map.Entry[] entries = new Map.Entry[length];
            for (int i = 0; i < length; i++) {
                entries[i] = Map.entry(costs[i].type, costs[i].cost);
            }
            return Map.of("costs", Map.ofEntries(entries));
        }
    }

    public static class RewardFunds {
        final static String I_PREP = "iprep";
        final static String I_CPS = "icps";
        final static String I_RELAY = "irelay";
        final static String I_VOTER = "ivoter";
        final static String[] VALUES = {I_PREP, I_CPS, I_RELAY, I_VOTER};
        RewardFund[] rewardFunds;

        public static class RewardFund {
            private final String type;
            private final BigInteger value;

            public RewardFund(String key, BigInteger fund) {
                this.type = key;
                this.value = fund;
            }

            public static void writeObject(ObjectWriter w, RewardFund s) {
                w.beginList(2);
                w.write(s.type);
                w.write(s.value);
                w.end();
            }

            public static RewardFund readObject(ObjectReader r) {
                r.beginList();
                var s = new RewardFund(r.readString(), r.readBigInteger());
                r.end();
                return s;
            }

            public boolean isType(String type) {
                return this.type.equals(type);
            }

            public BigInteger getValue() {
                return value;
            }
        }

        public RewardFunds(RewardFund[] rewardFunds) {
            this.rewardFunds = rewardFunds;
        }

        public static RewardFunds fromJson(JsonObject object) {
            var validKeys = new ArrayList<>(List.of(VALUES));
            var keys = object.names();
            var size = keys.size();
            RewardFund[] rewardFunds = new RewardFund[size];
            for (int i = 0; i < size; i++) {
                var key = keys.get(i);
                Context.require(validKeys.contains(key), "InvalidFundType");
                validKeys.remove(key);
                var value = object.getString(key, "");
                var fund = Converter.toInteger(value);
                rewardFunds[i] = new RewardFund(key, fund);
            }
            Context.require(validKeys.size() == 0, "InvalidFundType");
            return new RewardFunds(rewardFunds);
        }

        public static void writeObject(ObjectWriter w, RewardFunds values) {
            w.beginList(2);
            w.write(values.rewardFunds.length);
            var rewardValues = values.rewardFunds;
            for (RewardFund s : rewardValues) {
                w.write(s);
            }
            w.end();
        }

        public static RewardFunds readObject(ObjectReader r) {
            r.beginList();
            int size = r.readInt();
            RewardFund[] rewardFundList = new RewardFund[size];

            for (int i = 0; i < size; i++) {
                RewardFund s = r.read(RewardFund.class);
                rewardFundList[i] = s;
            }
            var rewardFunds = new RewardFunds(rewardFundList);

            r.end();
            return rewardFunds;
        }

        public Map<String, Object> toMap() {
            var length = rewardFunds.length;
            Map.Entry[] entries = new Map.Entry[length];
            for (int i = 0; i < length; i++) {
                entries[i] = Map.entry(rewardFunds[i].type, rewardFunds[i].value);
            }
            return Map.of("rewardFunds", Map.ofEntries(entries));
        }
    }
}
