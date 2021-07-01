package com.icon.governance;


import com.eclipsesource.json.JsonObject;
import score.Address;
import score.Context;
import score.ObjectWriter;
import score.ObjectReader;

import java.math.BigInteger;
import java.util.Map;

public class Value {
    private final int proposalType;
    private int code;
    private String text;
    private String name;
    private Address address;
    private int type;
    private BigInteger value;

    public Value(int p, String text) {
        // Text
        this.text = text;
        this.proposalType = p;
    }

    public Value(int p, int code, String name) {
        // Revision
        this.code = code;
        this.name = name;
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
        // StepPrice, IRep
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
        return this.proposalType;
    }

    private void set(ObjectWriter w) {
        w.write(proposalType);

        if (proposalType == 0) {
            w.write(text);
        } else if (proposalType == 1) {
            w.write(code);
            w.write(name);
        } else if (proposalType == 2) {
            w.write(address);
            w.write(type);
        } else if (proposalType == 3) {
            w.write(address);
        } else if (proposalType == 4 || proposalType == 5){
            w.write(value);
        }
    }
    public int size() {
        switch (proposalType) {
            case 0:
            case 3:
            case 4:
            case 5:
                return 2;
            case 1:
            case 2:
                return 3;
        }
        throw new IllegalArgumentException("proposalType not exist");
    }

    public static Value make(int proposalType, ObjectReader r) {
        if (proposalType == 0) {
            return new Value(proposalType, r.readString());
        } else if (proposalType == 1) {
            return new Value(proposalType, r.readInt(), r.readString());
        } else if (proposalType == 2) {
            return new Value(proposalType, r.readAddress(), r.readInt());
        } else if (proposalType == 3) {
            return new Value(proposalType, r.readAddress());
        } else if (proposalType == 4 || proposalType == 5){
            return new Value(proposalType, r.readBigInteger());
        } else {
            throw new IllegalArgumentException("proposalType not exist");
        }
    }

    public static Value makeWithJson(int type, JsonObject value) {
        switch (type) {
            case 0:
                return new Value(type, value.getString("value", null));
            case 1:
                return new Value(
                        type,
                        Integer.parseInt(value.getString("code", null)),
                        value.getString("name", null)
                );
            case 2:
                return new Value(
                        type,
                        Convert.strToAddress(value.getString("address" ,null)),
                        Convert.hexToInt(value.getString("type", null))
                );
            case 3:
                return new Value(type, Convert.strToAddress(value.getString("address", null)));
            case 4:
            case 5:
                return new Value(type, new BigInteger(value.getString("value", null), 10));
        }
        throw new IllegalArgumentException("Invalid value type");
    }

    public Map<String, Object> toMap() {
        switch (proposalType) {
            case 0:
                return Map.of("value", text);
            case 1:
                return Map.of(
                        "code", code,
                        "name", name
                );
            case 2:
                return Map.of(
                        "address", address,
                        "type", type
                );
            case 3:
                return Map.of("address", address);
            case 4:
            case 5:
                return Map.of("value", value);
        }
        throw new IllegalArgumentException("Invalid value type");
    }
}