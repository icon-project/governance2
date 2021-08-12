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

    private void set(ObjectWriter w) {
        w.write(proposalType);

        if (proposalType == Proposal.TEXT) {
            w.write(text);
        } else if (proposalType == Proposal.MALICIOUS_SCORE) {
            w.write(address);
            w.write(type);
        } else if (proposalType == Proposal.PREP_DISQUALIFICATION) {
            w.write(address);
        } else if (
                proposalType == Proposal.REVISION
                        || proposalType == Proposal.STEP_PRICE
                        || proposalType == Proposal.IREP
        ){
            w.write(value);
        } else if (proposalType == Proposal.STEP_COSTS) {
            w.write(stepCosts);
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
                return 2;
            case Proposal.MALICIOUS_SCORE:
                return 3;
        }
        throw new IllegalArgumentException("proposalType not exist");
    }

    public static Value make(int proposalType, ObjectReader r) {
        if (proposalType == Proposal.TEXT) {
            return new Value(proposalType, r.readString());
        } else if (proposalType == Proposal.MALICIOUS_SCORE) {
            return new Value(proposalType, r.readAddress(), r.readBigInteger());
        } else if (proposalType == Proposal.PREP_DISQUALIFICATION) {
            return new Value(proposalType, r.readAddress());
        } else if (proposalType == Proposal.REVISION ||
                proposalType == Proposal.STEP_PRICE ||
                proposalType == Proposal.IREP) {
            return new Value(proposalType, r.readBigInteger());
        } else if (proposalType == Proposal.STEP_COSTS) {
            var v = new Value(proposalType, r.read(StepCosts.class));
            return v;
        } else{
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
                return new Value(type, Converter.hexToInt(value.getString("type", null)));
            case Proposal.STEP_COSTS:
                return new Value(type, StepCosts.fromJson(value.get("costs").asArray()));
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
                return Map.of("value", value);
            case Proposal.STEP_COSTS:
                return stepCosts.toMap();
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

}
