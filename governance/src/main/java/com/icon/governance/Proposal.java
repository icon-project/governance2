package com.icon.governance;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import score.Address;
import score.ObjectWriter;
import score.ObjectReader;

import java.math.BigInteger;

final class Value {
    final int proposalType;
    int code;
    String text;
    String name;
    Address address;
    int type;
    BigInteger value;

    public Value(int p_type, String text) {
        // Text
        this.proposalType = p_type;
        this.text = text;
    }

    public Value(int p_type, int code, String name) {
        // Revision
        this.proposalType = p_type;
        this.code = code;
        this.name = name;
    }

    public Value(int p_type, Address address, int type) {
        // MaliciousScore
        this.proposalType = p_type;
        this.address = address;
        this.type = type;
    }

    public Value(int p_type, Address address) {
        // PRepDisQualification
        this.proposalType = p_type;
        this.address = address;
    }

    public Value(int p_type, BigInteger value) {
        // StepPrice, IRep
        this.proposalType = p_type;
        this.value = value;
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

}

/*
    PROPOSAL TYPE
    0 - TEXT
    1 - REVISION
    2 - MALICIOUS_SCORE
    3 - PREP_DISQUALIFICATION
    4 - STEP_PRICE
    5 - IREP

    PROPOSAL STATUS
    0 - Voting
    1 - Approved
    2 - DisApproved
    3 - Canceled
*/
public class Proposal {
    Voter   vote;
    Address proposer;
    String  proposerName;
    String  title;
    String  description;
    byte[]  id;
    Value  value;
    int     status;
    int     type;
    int     totalVoter;
    BigInteger  startBlockHeight;
    BigInteger  expireBlockHeight;
    BigInteger  totalBondedDelegation;

    public Proposal(
            byte[] id,
            Address proposer,
            String proposerName,
            String title,
            String description,
            int type,
            Value value,
            BigInteger  startBlockHeight,
            BigInteger  expireBlockHeight,
            int status,
            Voter vote,
            int         totalVoter,
            BigInteger  totalBondedDelegation
    ){
        this.id = id;
        this.proposer = proposer;
        this.proposerName = proposerName;
        this.title = title;
        this.description = description;
        this.type = type;
        this.value = value;
        this.startBlockHeight = startBlockHeight;
        this.expireBlockHeight = expireBlockHeight;
        this.status = status;
        this.vote = vote;
        this.totalVoter = totalVoter;
        this.totalBondedDelegation = totalBondedDelegation;
    }

//    public Map<String, ?> toMap() {
//        var proposalMap = Map.of(
//                "id", id,
//                "proposer", proposer,
//                "proposer_name", proposerName,
//                "title", title,
//                "description", description,
//                "type", type,
//                "value", value,
//                "start_block_height", startBlockHeight,
//                "end_block_height", expireBlockHeight,
//                "status", status,
//                "vote", vote.toMap(),
//                "total_voter", totalVoter,
//                "total_delegated_amount", totalBondedDelegation
//        );
//        return proposalMap;
//    }

    private void writeWithVote(ObjectWriter w, Voter v) {
        v.writeObject(w, v);
    }

    public static void writeObject(ObjectWriter w, Proposal p) {
        w.beginMap(13);
            w.write("id");
            w.write(p.id);
            w.write("proposer");
            w.write(p.proposer);
            w.write("proposer_name");
            w.write(p.proposerName);
            w.write("title");
            w.write(p.title);
            w.write("description");
            w.write(p.description);
            w.write("type");
            w.write(p.type);
            w.write("value");
            w.write(p.value);

            w.write("start_block_height");
            w.write(p.startBlockHeight);
            w.write("end_block_height");
            w.write(p.expireBlockHeight);
            w.write("status");
            w.write(p.status);
            w.write("vote");
            w.write(p.vote);
            w.write("total_voter");
            w.write(p.totalVoter);
            w.write("total_delegated_amount");
            w.write(p.totalBondedDelegation);
        w.end();
    }

    public static Proposal readObject(ObjectReader r) {
        r.beginMap();
        var proposal = new Proposal(
                r.readByteArray(),
                r.readAddress(),
                r.readString(),
                r.readString(),
                r.readString(),
                r.readInt(),
                new Value(0, BigInteger.ZERO),
                r.readBigInteger(),
                r.readBigInteger(),
                r.readInt(),
                new Voter(),
                r.readInt(),
                r.readBigInteger()
        );
        r.end();
        return proposal;
    }

    public static Proposal makeWithJson(byte[] data) throws ParseException {
        String jsonStr = new String(data);
        JsonValue json = Json.parse(jsonStr);
        JsonObject jsonObj = json.asObject();

        byte[] id = Convert.hexToBytes(
                jsonObj.getString("id", null)
        );
        Address proposer = Convert.strToAddress(
                jsonObj.getString("proposer", null)
        );

        String proposerName = jsonObj.getString("proposer_name", null);
        String title = jsonObj.getString("title", null);
        String description = jsonObj.getString("description", null);

        int type = Convert.hexToInt(
                jsonObj.getString("type", null)
        );
        Value value = Value.makeWithJson(
                type, jsonObj.get("value").asObject()
        );

        BigInteger  startBlockHeight = Convert.hexToBigInt(
                jsonObj.getString("start_block_height", null)
        );
        BigInteger  expireBlockHeight = Convert.hexToBigInt(
                jsonObj.getString("end_block_height", null)
        );
        int status = Convert.hexToInt(
                jsonObj.getString("status", null)
        );
        Voter vote = Voter.makeVoterWithJson(jsonObj.get("vote"));
        int totalVoter = vote.size();
        BigInteger  totalBondedDelegation = vote.totalAmount();

        return new Proposal(
                id,
                proposer,
                proposerName,
                title,
                description,
                type,
                value,
                startBlockHeight,
                expireBlockHeight,
                status,
                vote,
                totalVoter,
                totalBondedDelegation
        );
    }
}
