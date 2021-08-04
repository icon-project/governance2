package com.icon.governance;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import score.Address;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigDecimal;
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
    final static int AGREE_VOTE = 1;
    final static int DISAGREE_VOTE = 0;
    Agree agree;
    Disagree disagree;
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

        public Map<String, Object> toMap() {
            return Map.of(
                    "id", id,
                    "timestamp", timestamp,
                    "address", address,
                    "name", name,
                    "amount", amount
            );
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
            var entries = new Map[voteList.length];
            for (int i = 0; i < voteList.length; i++) {
                entries[i] = voteList[i].toMap();
            }
            return Map.of(
                    "list", entries,
                    "amount", amount
            );

        }

        public void readJson(JsonValue jsonValue) {
            JsonObject obj = jsonValue.asObject();

            JsonArray array = obj.get("list").asArray();
            BigInteger total = BigDecimal.valueOf(
                    obj.getDouble("amount", 0)
            ).toBigInteger();
            this.setAmount(total);

            Vote[] voteList = new Vote[array.size()];
            int i = 0;
            for (JsonValue item : array) {
                JsonObject voteJson = item.asObject();
                if (voteJson.size() != 5) {
                    throw new IllegalArgumentException("Invalid agree size");
                }

                byte[] id = Convert.hexToBytes(
                        voteJson.getString("id", null)
                );
                BigInteger timestamp = BigDecimal.valueOf(
                        voteJson.getDouble("timestamp", 0)
                ).toBigInteger();
                Address address = Convert.strToAddress(
                        voteJson.getString("address", null)
                );
                String name = voteJson.getString("name", null);
                BigInteger amount = BigDecimal.valueOf(
                        voteJson.getDouble("amount", 0)
                ).toBigInteger();

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


    public static class Disagree extends Slot {
        public Disagree() {
            super(new Vote[0], BigInteger.ZERO);
        }

        public static void writeObject(ObjectWriter w, Disagree d) {
            w.beginList(2);
            w.write(d.size());
            for (Vote v : d.getVoteList()) {
                w.write(v);
            }
            w.write(d.getAmount());
            w.end();
        }

        public static Disagree readObject(ObjectReader r) {
            r.beginList();
            var d = new Disagree();
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
                    "list", List.of(list),
                    "amount", amount
            );
        }

        public void readJson(JsonValue jsonValue) {
            JsonObject obj = jsonValue.asObject();

            JsonArray array = obj.get("list").asArray();
            BigInteger total = BigDecimal.valueOf(
                    obj.getDouble("amount", 0)
            ).toBigInteger();
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
        this.disagree = new Disagree();
        this.noVote = new NoVote();
    }

    public Voter(
            Agree a,
            Disagree d,
            NoVote n
    ) {
        this.agree = a;
        this.disagree = d;
        this.noVote = n;
    }

    public static void writeObject(ObjectWriter w, Voter v) {
        w.beginList(3);
        w.write(v.agree);
        w.write(v.disagree);
        w.write(v.noVote);
        w.end();
    }

    public static Voter readObject(ObjectReader r) {
        r.beginList();
        var v = new Voter(
                r.read(Agree.class),
                r.read(Disagree.class),
                r.read(NoVote.class)
        );
        r.end();
        return v;
    }

    public void setAmountForNoVote(BigInteger amount) {
        noVote.setAmount(amount);
    }

    public void setNoVoteList(Address[] addresses) {
        noVote.setAddressList(addresses);
    }

    public Map<String, Map<String, Object>> toMap() {
        return Map.of(
                "agree", agree.toMap(),
                "disagree", disagree.toMap(),
                "noVote", noVote.toMap()
        );
    }

    public int size() {
        return agree.size() + disagree.size() + noVote.size();
    }

    public int sizeofAgreed() {
        return agree.size();
    }

    public int sizeofDisagreed() {
        return disagree.size();
    }

    public BigInteger amountOfAgreed() {
        return agree.getAmount();
    }

    public BigInteger amountOfDisagreed() {
        return disagree.getAmount();
    }

    private void buildAgreeWithJson(JsonValue jsonValue) {
        this.agree.readJson(jsonValue);
    }

    private void buildDisAgreeWithJson(JsonValue jsonValue) {
        this.disagree.readJson(jsonValue);
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

    public boolean agreed(Address voter) {
        for (Vote v : agree.voteList) {
            if (v.address.equals(voter)) return true;
        }
        return false;
    }

    public boolean disagreed(Address voter) {
        for (Vote v : disagree.voteList) {
            if (v.address.equals(voter)) return true;
        }
        return false;
    }
    public boolean isInNoVote(Address voter) {
        for (Address a : noVote.list) {
            if (a.equals(voter)) return true;
        }
        return false;
    }
}
