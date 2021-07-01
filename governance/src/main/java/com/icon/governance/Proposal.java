package com.icon.governance;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.JsonObject;

import score.Address;
import score.Context;
import score.ObjectWriter;
import score.ObjectReader;

import java.math.BigInteger;
import java.util.Map;

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
    final byte[] id;
    final Address proposer;
    String proposerName;
    String title;
    String description;
    int type;
    Value value;
    BigInteger startBlockHeight;
    BigInteger expireBlockHeight;
    int status;
    Voter vote;
    int totalVoter;
    BigInteger totalBondedDelegation;

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

    public static void writeObject(ObjectWriter w, Proposal p) {
        w.beginList(13);
        w.write(p.id);
        w.write(p.proposer);
        w.write(p.proposerName);
        w.write(p.title);
        w.write(p.description);
        w.write(p.type);
        w.write(p.value);
        w.write(p.startBlockHeight);
        w.write(p.expireBlockHeight);
        w.write(p.status);
        w.write(p.vote);
        w.write(p.totalVoter);
        w.write(p.totalBondedDelegation);
        w.end();
    }

    public static Proposal readObject(ObjectReader r) {
        r.beginMap();
        var p = new Proposal(
                r.readByteArray(),
                r.readAddress(),
                r.readString(),
                r.readString(),
                r.readString(),
                r.readInt(),
                r.read(Value.class),
                r.readBigInteger(),
                r.readBigInteger(),
                r.readInt(),
                r.read(Voter.class),
                r.readInt(),
                r.readBigInteger()
        );
        r.end();
        return p;
    }

//    public Map<String, Object> toMap() {
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
//        return Map.of(
//                "id", id,
//                "proposer", proposer,
//                "proposer_name", proposerName,
//                "title", title,
//                "description", description,
//                "type", type,
//                "value", value.toMap(),
//                "start_block_height", startBlockHeight,
//                "end_block_height", expireBlockHeight,
//                "status", status,
//                "vote", vote.toMap(),
//                "total_voter", totalVoter,
//                "total_delegated_amount", totalBondedDelegation
//        );
//    }

    public static Proposal makeWithJson(byte[] data) {
        // python, java 서로 다른 DB 사용(prefix key),
        // python.get is null -> java score.
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
//        int total_voter = Integer.parseInt(jsonObj.getString("total_voter", null));
//        if (totalVoter != total_voter) {
//            throw new IllegalArgumentException("totalVoter not equal");
//        }

        BigInteger  totalBondedDelegation = vote.totalAmount();
//        BigInteger  total_delegated_amount = new BigInteger(
//                jsonObj.getString("total_delegated_amount", null)
//        );
//        if (!totalBondedDelegation.equals(total_delegated_amount)) {
//            throw new IllegalArgumentException("amount not equal");
//        }

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
