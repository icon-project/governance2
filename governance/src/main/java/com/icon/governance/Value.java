package com.icon.governance;


import com.eclipsesource.json.JsonObject;
import score.Address;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;
import java.util.Map;

public class Value {
    private final int proposalType;
    private String text;
    private Address address;
    private int type;
    private BigInteger value;

    public Value(int p, String text) {
        // Text
        this.text = text;
        this.proposalType = p;
    }

    public Value(int p, Address address, int type) {
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

    public int type() {
        return proposalType;
    }

    public BigInteger value() {
        return value;
    }

    public Address address() {
        return address;
    }

    private void set(ObjectWriter w) {
        w.write(proposalType);

        if (proposalType == Proposal.TEXT) {
            w.write(text);
        }else if (proposalType == Proposal.MALICIOUS_SCORE) {
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
        }
    }
    public int size() {
        switch (proposalType) {
            case Proposal.TEXT:
            case Proposal.REVISION:
            case Proposal.PREP_DISQUALIFICATION:
            case Proposal.STEP_PRICE:
            case Proposal.IREP:
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
            return new Value(proposalType, r.readAddress(), r.readInt());
        } else if (proposalType == Proposal.PREP_DISQUALIFICATION) {
            return new Value(proposalType, r.readAddress());
        } else if (
                proposalType == Proposal.REVISION
                        || proposalType == Proposal.STEP_PRICE
                        || proposalType == Proposal.IREP
        ) {
            return new Value(proposalType, r.readBigInteger());
        } else {
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
                        Converter.hexToInt(value.getString("type", null)).intValue()
                );
            case Proposal.PREP_DISQUALIFICATION:
                return new Value(type, Converter.strToAddress(value.getString("address", null)));
            case Proposal.REVISION:
            case Proposal.STEP_PRICE:
            case Proposal.IREP:
                var v = value.getString("value", null);
                v = v.substring(2);
                return new Value(type, new BigInteger(v, 16));
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
        }
        throw new IllegalArgumentException("Invalid value type");
    }
}
