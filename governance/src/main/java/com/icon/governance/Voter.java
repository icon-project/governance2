package com.icon.governance;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import score.Address;
import score.Context;
import score.ObjectWriter;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/*
    VOTER STATUS
    2 - Pending (noVote) --> Slash **
    1 - Agree
    0 - DisAgree

    Condition Rate
    Approve - 0.66
    DisApprove - 0.33
*/

class Vote {
    private final byte[] id;
    private final BigInteger timestamp;
    private final Address address;
    private final String name;
    private final BigInteger amount;

    public Vote(byte[] id, BigInteger timestamp, Address address, String name, BigInteger amount) {
        this.id = id;
        this.timestamp = timestamp;
        this.address = address;
        this.name = name;
        this.amount = amount;
    }

    public static void writeObject(ObjectWriter w, Vote v) {
        w.beginMap(5);
            w.write("id");
            w.write(v.id);

            w.write("timestamp");
            w.write(v.timestamp);

            w.write("address");
            w.write(v.address);

            w.write("name");
            w.write(v.name);

            w.write("amount");
            w.write(v.amount);
        w.end();
    }
}


interface IVoter {
    abstract List<?> getVoteList();
    abstract BigInteger getAmount();
    abstract Map<?, ?> toMap();
    abstract int size();
    abstract void readJson(JsonValue jsonValue);
}

abstract class Slot implements IVoter {
    List<Vote> voteList;
    BigInteger amount;

    public Slot(List<Vote> list, BigInteger amount) {
        this.voteList = list;
        this.amount = amount;
    }

    public List<Vote> getVoteList() {
        return this.voteList;
    }

    public void setVoteList(List<Vote> list) {
        this.voteList = list;
    }

    public BigInteger getAmount() {
        return this.amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }

    public int size() {
        return this.voteList.size();
    }

    public Map<String, ?> toMap() {
        return Map.of(
                "list", voteList,
                "amount", amount
        );
    }

    public void readJson(JsonValue jsonValue) {
        JsonObject obj = jsonValue.asObject();

        JsonArray array = obj.get("list").asArray();
        BigInteger total = Convert.hexToBigInt(obj.getString("amount", null));
        this.setAmount(total);

        Vote[] voteList = new Vote[array.size()];
        int i = 0;
        for (JsonValue item : array) {
            JsonObject voteJson = item.asObject();
            if (voteJson.size() != 5) {
                throw new IllegalArgumentException("Invalid agree size");
            }

            String _id = voteJson.getString("id", null);
            String _timestamp = voteJson.getString("timestamp", null);
            String _address = voteJson.getString("address", null);
            String _amount = voteJson.getString("amount", null);

            byte[] id = Convert.hexToBytes(_id);
            BigInteger timestamp = Convert.hexToBigInt(_timestamp);
            Address address = Convert.strToAddress(_address);
            String name = voteJson.getString("name", null);
            BigInteger amount = Convert.hexToBigInt(_amount);

            voteList[i++] = new Vote(id, timestamp, address, name, amount);
        }
        this.setVoteList(List.of(voteList));
    }

    public static void writeObject(ObjectWriter w, Slot a) {
        w.beginMap(2);
        w.write("list");
        w.beginList(a.getVoteList().size());
        for (Vote vote : a.getVoteList()) {
            vote.writeObject(w, vote);
        }
        w.end();

        w.write("amount");
        w.write(a.getAmount());
        w.end();
    }
}

class Agree extends Slot {
    public Agree() {
        super(List.of(), BigInteger.ZERO);
    }
}


class DisAgree extends Slot {
    public DisAgree() {
        super(List.of(), BigInteger.ZERO);
    }
}

class NoVote implements IVoter {
    List<Address> list;
    BigInteger amount;

    public NoVote() {
        this.list = List.of();
        this.amount = BigInteger.ZERO;
    }

    public List<Address> getVoteList() {
        return list;
    }

    public void setVoteList(List<Address> list) {
        this.list = list;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }

    public int size() {
        return list.size();
    }

    public Map<String, ?> toMap() {
        return Map.of(
                "list", list,
                "amount", amount
        );
    }

    public static void writeObject(ObjectWriter w, NoVote n) {
        w.beginMap(2);
        w.write("list");
        w.beginList(n.getVoteList().size());
        for (Address addr : n.getVoteList()) {
            w.write(addr);
        }
        w.end();

        w.write("amount");
        w.write(n.getAmount());
        w.end();
    }

    public void readJson(JsonValue jsonValue) {
        JsonObject obj = jsonValue.asObject();

        JsonArray array = obj.get("list").asArray();
        BigInteger total = Convert.hexToBigInt(obj.getString("amount", null));
        this.setAmount(total);

        Address[] addrList = new Address[array.size()];
        int i = 0;
        for (JsonValue item : array) {
            String _address = item.asString();
            Address address = Convert.strToAddress(_address);
            addrList[i++] = address;
        }
        this.setVoteList(List.of(addrList));
    }
}

class Voter {
    Agree agree;
    DisAgree disAgree;
    NoVote noVote;


    public Voter() {
        this.agree = new Agree();
        this.disAgree = new DisAgree();
        this.noVote = new NoVote();
    }

    public void setAmountForNoVote(BigInteger amount) {
        this.noVote.setAmount(amount);
    }

    public Map<String, Map<String, ?>> toMap() {
        return Map.of(
                "agree", agree.toMap(),
                "disagree", disAgree.toMap(),
                "noVote", noVote.toMap()
        );
    }

    public String sizeof() {
        return  "agree: " + agree.size() +
                " disagree: " + disAgree.size() +
                " noVote: " + noVote.size();
    }

    public int size() {
        return agree.size() + disAgree.size() + noVote.size();
    }

    public BigInteger totalAmount() {
        BigInteger totalAmount = agree.getAmount().add(disAgree.getAmount());
        totalAmount = totalAmount.add(noVote.getAmount());
        return totalAmount;
    }

    private void buildAgreeWithJson(JsonValue jsonValue) {
        this.agree.readJson(jsonValue);
    }

    private void buildDisAgreeWithJson(JsonValue jsonValue) {
        this.disAgree.readJson(jsonValue);
    }

    private void buildNoVoteWithJson(JsonValue jsonValue) {
        this.noVote.readJson(jsonValue);
    }

    private void build(JsonObject jso) {
        this.buildAgreeWithJson(jso.get("agree"));
        this.buildDisAgreeWithJson(jso.get("disagree"));
        this.buildNoVoteWithJson(jso.get("noVote"));
    }

    public static Voter makeVoterWithJson(JsonValue jsonValue) {
        JsonObject voteJson = jsonValue.asObject();
        Voter v = new Voter();
        v.build(voteJson);
        return v;
    }


//    private void writeWithAgree(ObjectWriter w, Voter v) {
//        v.agree.writeObject(w, v.agree);
//    }
//
//    private void writeWithDisAgree(ObjectWriter w, Voter v) {
//        v.disAgree.writeObject(w, v.disAgree);
//    }
//
//    private void writeWithNoVote(ObjectWriter w, Voter v) {
//        v.noVote.writeObject(w, v.noVote);
//    }

    public static void writeObject(ObjectWriter w, Voter v) {
        w.beginMap(3);
        w.write("agree");
        w.write(v.agree);
        w.write("disagree");
        w.write(v.disAgree);
        w.write("noVote");
        w.write(v.noVote);
        w.end();
    }

}

