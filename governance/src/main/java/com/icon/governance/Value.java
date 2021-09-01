package com.icon.governance;


import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import score.Address;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;
import java.util.Map;

public class Value {
    private final int proposalType;
    private String text;
    private Address address;
    private BigInteger type;
    private BigInteger value;
    private StepCosts stepCosts;
    private RewardFunds rewardFunds;

    public Value(int p, String text) {
        // Text
        this.text = text;
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
        // StepPrice, IRep, Revision
        this.value = value;
        this.proposalType = p;
    }

    public Value(int p, StepCosts value) {
        this.proposalType = p;
        this.stepCosts = value;
    }

    public Value(int p, RewardFunds value) {
        this.proposalType = p;
        this.rewardFunds = value;
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

    public int proposalType() {
        return proposalType;
    }

    public BigInteger value() {
        return value;
    }

    public BigInteger type() {
        return type;
    }

    public Address address() {
        return address;
    }

    public StepCosts stepCosts() {
        return stepCosts;
    }

    public RewardFunds rewardFunds() {
        return rewardFunds;
    }

    private void set(ObjectWriter w) {
        w.write(proposalType);
        switch (proposalType) {
            case Proposal.TEXT:
                w.write(text);
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
            case Proposal.REWARD_FUNDS_RATE:
                w.write(rewardFunds);
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
            case Proposal.REWARD_FUNDS_RATE:
            case Proposal.REWARD_FUND:
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
            case Proposal.REWARD_FUNDS_RATE:
                return new Value(proposalType, r.read(RewardFunds.class));
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
                        Converter.strToAddress(value.getString("address" ,null)),
                        Converter.hexToInt(value.getString("type", null))
                );
            case Proposal.PREP_DISQUALIFICATION:
                return new Value(type, Converter.strToAddress(value.getString("address", null)));
            case Proposal.REVISION:
            case Proposal.STEP_PRICE:
            case Proposal.IREP:
            case Proposal.REWARD_FUND:
                return new Value(type, Converter.hexToInt(value.getString("value", null)));
            case Proposal.STEP_COSTS:
                return new Value(type, StepCosts.fromJson(value.get("costs").asArray()));
            case Proposal.REWARD_FUNDS_RATE:
                return new Value(type, RewardFunds.fromJson(value.get("rewardFunds").asArray()));
        }
        throw new IllegalArgumentException("Invalid value type");
    }

    public Map<String, Object> toMap() {
        switch (proposalType) {
            case Proposal.TEXT:
                return Map.of("value", text);
            case Proposal.MALICIOUS_SCORE:
                return Map.of(
                        "address", address,
                        "type", type
                );
            case Proposal.PREP_DISQUALIFICATION:
                return Map.of("address", address);
            case Proposal.REVISION:
            case Proposal.STEP_PRICE:
            case Proposal.IREP:
            case Proposal.REWARD_FUND:
                return Map.of("value", value);
            case Proposal.STEP_COSTS:
                return stepCosts.toMap();
            case Proposal.REWARD_FUNDS_RATE:
                return rewardFunds.toMap();
        }
        throw new IllegalArgumentException("Invalid value type");
    }

    public static class StepCosts {
        final static String STEP_TYPE_DEFAULT = "default";
        final static String STEP_TYPE_CONTRACT_CALL = "contractCall";
        final static String STEP_TYPE_CONTRACT_CREATE = "contractCreate";
        final static String STEP_TYPE_CONTRACT_UPDATE = "contractUpdate";
        final static String STEP_TYPE_CONTRACT_DESTRUCT = "contractDestruct";
        final static String STEP_TYPE_CONTRACT_SET = "contractSet";
        final static String STEP_TYPE_GET = "get";
        final static String STEP_TYPE_SET = "set";
        final static String STEP_TYPE_REPLACE = "replace";
        final static String STEP_TYPE_DELETE = "delete";
        final static String STEP_TYPE_INPUT = "input";
        final static String STEP_TYPE_EVENT_LOG = "eventLog";
        final static String STEP_TYPE_API_CALL = "apiCall";
        final static String[] STEP_COSTS = {
                STEP_TYPE_DEFAULT, STEP_TYPE_CONTRACT_CALL, STEP_TYPE_CONTRACT_CREATE, STEP_TYPE_CONTRACT_UPDATE, STEP_TYPE_CONTRACT_DESTRUCT, STEP_TYPE_CONTRACT_SET,
                STEP_TYPE_GET, STEP_TYPE_SET, STEP_TYPE_REPLACE, STEP_TYPE_DELETE, STEP_TYPE_INPUT, STEP_TYPE_EVENT_LOG, STEP_TYPE_API_CALL
        };
        StepCost[] stepCosts;

        public static class StepCost {
            String type;
            BigInteger cost;
            public StepCost() {

            }

            public static void writeObject(ObjectWriter w, StepCost s) {
                w.beginList(2);
                w.write(s.type);
                w.write(s.cost);
                w.end();
            }

            public static StepCost readObject(ObjectReader r) {
                r.beginList();
                var s = new StepCost();
                s.type = r.readString();
                s.cost = r.readBigInteger();
                r.end();
                return s;
            }

            Map<String, Object> toMap() {
                return Map.of(type, cost);
            }
        }

        public StepCosts(StepCost[] stepCosts) {
            this.stepCosts = stepCosts;
        }

        public static StepCosts fromJson(JsonArray array) {
            StepCost[] stepCosts = new StepCost[array.size()];
            for (int i=0; i < array.size(); i++) {
                var value = array.get(i);
                var object = value.asObject();
                var keys = object.names();
                Context.require(keys.size() == 1, "stepCost map must have one field.");
                var key = keys.get(0);
                Context.require(isValidStepType(key), key + "is not valid step type");
                var cost = Converter.hexToInt(object.getString(key, ""));
                stepCosts[i] = new StepCost();
                stepCosts[i].type = key;
                stepCosts[i].cost = cost;
            }
            return new StepCosts(stepCosts);
        }

        public static void writeObject(ObjectWriter w, StepCosts costs) {
            w.beginList(2);
            w.write(costs.stepCosts.length);
            var stepCostList = costs.stepCosts;
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
            var length = stepCosts.length;
            var entries = new Map[length];
            for (int i = 0; i < length; i++) {
                entries[i] = stepCosts[i].toMap();
            }
            return Map.of("costs", entries);
        }

    }

    public static class RewardFunds {
        final static String I_PREP = "Iprep";
        final static String I_CPS = "Icps";
        final static String I_RELAY = "Irelay";
        final static String I_VOTER = "Ivoter";
        final static String[] VALUES = {I_PREP, I_CPS, I_RELAY, I_VOTER};
        RewardFund[] rewardFunds;

        public static class RewardFund {
            String type;
            BigInteger value;
            public RewardFund() {}

            public static void writeObject(ObjectWriter w, RewardFund s) {
                w.beginList(2);
                w.write(s.type);
                w.write(s.value);
                w.end();
            }

            public static RewardFund readObject(ObjectReader r) {
                r.beginList();
                var s = new RewardFund();
                s.type = r.readString();
                s.value = r.readBigInteger();
                r.end();
                return s;
            }

            Map<String, BigInteger> toMap() {
                return Map.of(type, value);
            }
        }

        public RewardFunds(RewardFund[] rewardFunds) {
            this.rewardFunds = rewardFunds;
        }

        public static RewardFunds fromJson(JsonArray array) {
            RewardFund[] rewardFunds = new RewardFund[array.size()];
            for (int i=0; i < array.size(); i++) {
                var value = array.get(i);
                var object = value.asObject();
                var keys = object.names();
                Context.require(keys.size() == 1, "RewardFund map must have one field.");
                var key = keys.get(0);
                Context.require(isValidFundKey(key), key + "is not valid reward fund type");
                var v = Converter.hexToInt(object.getString(key, ""));
                rewardFunds[i] = new RewardFund();
                rewardFunds[i].type = key;
                rewardFunds[i].value = v;
            }
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

        static boolean isValidFundKey(String type) {
            for (String t : VALUES) {
                if (type.equals(t)) return true;
            }
            return false;
        }

        public Map<String, Object> toMap() {
            var length = rewardFunds.length;
            var entries = new Map[length];
            for (int i = 0; i < length; i++) {
                entries[i] = rewardFunds[i].toMap();
            }
            return Map.of("values", entries);
        }

    }
}
