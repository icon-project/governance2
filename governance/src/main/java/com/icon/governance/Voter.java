package com.icon.governance;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import score.Address;
import score.Context;
import score.ObjectReader;
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

public class Voter {
    Agree agree;
    DisAgree disAgree;
    NoVote noVote;

    public static class Vote {
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
            w.beginList(5);
            w.write(v.id);
            w.write(v.timestamp);
            w.write(v.address);
            w.write(v.name);
            w.write(v.amount);
            w.end();
        }

        public static Vote readObject(ObjectReader r) {
            r.beginList();
            var v = new Vote(
                    r.readByteArray(),
                    r.readBigInteger(),
                    r.readAddress(),
                    r.readString(),
                    r.readBigInteger()
            );
            r.end();
            return v;
        }
    }

    public abstract static class Slot {
        Vote[] voteList;
        BigInteger amount;

        public Slot(Vote[] list, BigInteger amount) {
            this.voteList = list;
            this.amount = amount;
        }

        public Vote[] getVoteList() {
            return this.voteList;
        }

        public void setVoteList(Vote[] list) {
            this.voteList = list;
        }

        public BigInteger getAmount() {
            return this.amount;
        }

        public void setAmount(BigInteger amount) {
            this.amount = amount;
        }

        public Integer size() {
            return this.voteList.length;
        }

        public Map<String, Object> toMap() {
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
            this.setVoteList(voteList);
        }
    }

    public static class Agree extends Slot {
        public Agree() {
            super(new Vote[0], BigInteger.ZERO);
        }

        public static void writeObject(ObjectWriter w, Agree a) {
            w.beginList(2);
            w.write(a.size());
            for (Vote v : a.getVoteList()) {
                w.write(v);
            }
            w.write(a.getAmount());
            w.end();
        }

        public static Agree readObject(ObjectReader r) {
            r.beginList();
            var a = new Agree();
            int size = r.readInt();
            Vote[] voteList = new Vote[size];

            for (int i = 0; i < size; i++) {
                Vote v = r.read(Vote.class);
                voteList[i] = v;
            }

            a.setVoteList(voteList);
            a.setAmount(r.readBigInteger());
            r.end();
            return a;
        }
    }


    public static class DisAgree extends Slot {
        public DisAgree() {
            super(new Vote[0], BigInteger.ZERO);
        }

        public static void writeObject(ObjectWriter w, DisAgree d) {
            w.beginList(2);
            w.write(d.size());
            for (Vote v : d.getVoteList()) {
                w.write(v);
            }
            w.write(d.getAmount());
            w.end();
        }

        public static DisAgree readObject(ObjectReader r) {
            r.beginList();
            var d = new DisAgree();
            int size = r.readInt();
            Vote[] voteList = new Vote[size];

            for (int i = 0; i < size; i++) {
                Vote v = r.read(Vote.class);
                voteList[i] = v;
            }

            d.setVoteList(voteList);
            d.setAmount(r.readBigInteger());
            r.end();
            return d;
        }
    }

    public static class NoVote {
        Address[] list;
        BigInteger amount;

        public NoVote() {
            this.list = new Address[0];
            this.amount = BigInteger.ZERO;
        }

        public static void writeObject(ObjectWriter w, NoVote n) {
            w.beginList(2);
            w.write(n.size());
            for (Address v : n.getAddressList()) {
                w.write(v);
            }
            w.write(n.getAmount());
            w.end();
        }

        public static NoVote readObject(ObjectReader r) {
            r.beginList();
            var n = new NoVote();
            int size = r.readInt();
            Address[] addressList = new Address[size];

            for (int i = 0; i < size; i++) {
                Address a = r.readAddress();
                addressList[i] = a;
            }

            n.setAddressList(addressList);
            n.setAmount(r.readBigInteger());
            r.end();
            return n;
        }

        public Address[] getAddressList() {
            return this.list;
        }

        public void setAddressList(Address[] list) {
            this.list = list;
        }

        public BigInteger getAmount() {
            return amount;
        }

        public void setAmount(BigInteger amount) {
            this.amount = amount;
        }

        public Integer size() {
            return list.length;
        }

        public Map<String, Object> toMap() {
            return Map.of(
                    "list", list,
                    "amount", amount
            );
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
            this.setAddressList(addrList);
        }
    }

    public Voter() {
        this.agree = new Agree();
        this.disAgree = new DisAgree();
        this.noVote = new NoVote();
    }

    public Voter(
            Agree a,
            DisAgree d,
            NoVote n
    ) {
        this.agree = a;
        this.disAgree = d;
        this.noVote = n;
    }

    public static void writeObject(ObjectWriter w, Voter v) {
        w.beginList(3);
        w.write(v.agree);
        w.write(v.disAgree);
        w.write(v.noVote);
        w.end();
    }

    public static Voter readObject(ObjectReader r) {
        r.beginList();
        var v = new Voter(
                r.read(Agree.class),
                r.read(DisAgree.class),
                r.read(NoVote.class)
        );
        r.end();
        return v;
    }

    public void setAmountForNoVote(BigInteger amount) {
        this.noVote.setAmount(amount);
    }

    public Map<String, Map<String, Object>> toMap() {
        return Map.of(
                "agree", agree.toMap(),
                "disagree", disAgree.toMap(),
                "noVote", noVote.toMap()
        );
    }

    public String sizeof() {
        return  "agree: " + agree.size().toString() +
                " disagree: " + disAgree.size().toString() +
                " noVote: " + noVote.size().toString();
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
}

