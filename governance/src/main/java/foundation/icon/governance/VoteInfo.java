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

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import score.Address;
import score.Context;
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

public class VoteInfo {
    static final int AGREE_VOTE = 1;
    static final int DISAGREE_VOTE = 0;
    private final Vote agree;
    private final Vote disagree;
    private final NoVote noVote;

    public static class VoterInfo {
        private final byte[] id;
        private final BigInteger timestamp;
        private final Address address;
        private final String name;
        private final BigInteger amount;

        public VoterInfo(byte[] id, BigInteger timestamp, Address address, String name, BigInteger amount) {
            this.id = id;
            this.timestamp = timestamp;
            this.address = address;
            this.name = name;
            this.amount = amount;
        }

        public static void writeObject(ObjectWriter w, VoterInfo v) {
            w.beginList(5);
            w.write(v.id);
            w.write(v.timestamp);
            w.write(v.address);
            w.write(v.name);
            w.write(v.amount);
            w.end();
        }

        public static VoterInfo readObject(ObjectReader r) {
            r.beginList();
            var v = new VoterInfo(
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

        public Map<String, Object> getSummary() {
            return Map.of(
                    "id", id,
                    "timestamp", timestamp,
                    "address", address,
                    "name", name,
                    "amount", amount
            );
        }
    }

    public static class Vote {
        VoterInfo[] voterInfoList;
        BigInteger amount;

        public Vote() {
            voterInfoList = new VoterInfo[0];
            amount = BigInteger.ZERO;
        }

        public Vote(VoterInfo[] list, BigInteger amount) {
            this.voterInfoList = list;
            this.amount = amount;
        }

        public VoterInfo[] getVoterInfoList() {
            return this.voterInfoList;
        }

        public void setVoterInfoList(VoterInfo[] list) {
            this.voterInfoList = list;
        }

        public BigInteger getAmount() {
            return this.amount;
        }

        public void setAmount(BigInteger amount) {
            this.amount = amount;
        }

        public Integer size() {
            return this.voterInfoList.length;
        }

        public Map<String, Object> toMap() {
            var entries = new Map[voterInfoList.length];
            for (int i = 0; i < voterInfoList.length; i++) {
                entries[i] = voterInfoList[i].toMap();
            }
            return Map.of(
                    "list", entries,
                    "amount", amount
            );
        }

        public Map<String, Object> getSummary() {
            return Map.of(
                    "count", voterInfoList.length,
                    "amount", amount
            );
        }

        public void readJson(JsonValue jsonValue) {
            JsonObject obj = jsonValue.asObject();

            JsonArray array = obj.get("list").asArray();
            BigInteger total = BigDecimal.valueOf(
                    obj.getDouble("amount", 0)
            ).toBigInteger();
            setAmount(total);

            VoterInfo[] voterInfoList = new VoterInfo[array.size()];
            int i = 0;
            for (JsonValue item : array) {
                JsonObject voteJson = item.asObject();
                if (voteJson.size() != 5) {
                    throw new IllegalArgumentException("Invalid agree size");
                }

                byte[] id = Converter.hexToBytes(
                        voteJson.getString("id", null)
                );
                BigInteger timestamp = BigDecimal.valueOf(
                        voteJson.getDouble("timestamp", 0)
                ).toBigInteger();
                Address address = Converter.toAddress(
                        voteJson.getString("address", null)
                );
                String name = voteJson.getString("name", null);
                BigInteger amount = BigDecimal.valueOf(
                        voteJson.getDouble("amount", 0)
                ).toBigInteger();

                voterInfoList[i++] = new VoterInfo(id, timestamp, address, name, amount);
            }
            setVoterInfoList(voterInfoList);
        }

        public static void writeObject(ObjectWriter w, Vote v) {
            w.beginList(2);
            w.write(v.size());
            for (VoterInfo voterInfo : v.getVoterInfoList()) {
                w.write(voterInfo);
            }
            w.write(v.getAmount());
            w.end();
        }

        public static Vote readObject(ObjectReader r) {
            r.beginList();
            int size = r.readInt();
            VoterInfo[] voterInfoList = new VoterInfo[size];

            for (int i = 0; i < size; i++) {
                VoterInfo v = r.read(VoterInfo.class);
                voterInfoList[i] = v;
            }
            var a = new Vote(voterInfoList, r.readBigInteger());
            r.end();
            return a;
        }
    }

    public static class NoVote {
        private Address[] list;
        private BigInteger amount;

        public NoVote() {
            this.list = new Address[0];
            this.amount = BigInteger.ZERO;
        }

        public static void writeObject(ObjectWriter w, NoVote n) {
            w.beginList(2);
            w.write(n.size());
            for (Address v : n.list) {
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

        public Map<String, Object> getSummary() {
            return Map.of(
                    "count", list.length,
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
                Address address = Converter.toAddress(_address);
                addrList[i++] = address;
            }
            this.setAddressList(addrList);
        }
    }

    public VoteInfo() {
        this.agree = new Vote();
        this.disagree = new Vote();
        this.noVote = new NoVote();
    }

    public VoteInfo(
            Vote a,
            Vote d,
            NoVote n
    ) {
        this.agree = a;
        this.disagree = d;
        this.noVote = n;
    }

    public static void writeObject(ObjectWriter w, VoteInfo v) {
        w.beginList(3);
        w.write(v.agree);
        w.write(v.disagree);
        w.write(v.noVote);
        w.end();
    }

    public static VoteInfo readObject(ObjectReader r) {
        r.beginList();
        var v = new VoteInfo(
                r.read(Vote.class),
                r.read(Vote.class),
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

    public Address[] getNoVoteList() {
        return noVote.list;
    }

    public Map<String, Map<String, Object>> toMap() {
        return Map.of(
                "agree", agree.toMap(),
                "disagree", disagree.toMap(),
                "noVote", noVote.toMap()
        );
    }

    public Map<String, Map<String, Object>> getSummary() {
        return Map.of(
                "agree", agree.getSummary(),
                "disagree", disagree.getSummary(),
                "noVote", noVote.getSummary()
        );
    }

    public int sizeofAgreed() {
        return agree.size();
    }

    public int sizeofDisagreed() {
        return disagree.size();
    }

    public int sizeofNoVote() {
        return noVote.size();
    }

    public BigInteger amountOfAgreed() {
        return agree.getAmount();
    }

    public BigInteger amountOfDisagreed() {
        return disagree.getAmount();
    }

    private void buildVote(Vote vote, JsonValue jsonValue) {
        vote.readJson(jsonValue);
    }

    private void build(JsonObject jso) {
        buildVote(agree, jso.get("agree"));
        buildVote(disagree, jso.get("disagree"));
        noVote.readJson(jso.get("noVote"));
    }

    public static VoteInfo makeVoter(JsonValue jsonValue) {
        JsonObject voteJson = jsonValue.asObject();
        VoteInfo v = new VoteInfo();
        v.build(voteJson);
        return v;
    }

    public boolean agreed(Address voter) {
        for (VoterInfo v : agree.voterInfoList) {
            if (v.address.equals(voter)) return true;
        }
        return false;
    }

    public boolean disagreed(Address voter) {
        for (VoterInfo v : disagree.voterInfoList) {
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

    void vote(PRepInfo voter, int vote) {
        VoteInfo.Vote v;
        if (vote == VoteInfo.AGREE_VOTE) {
            v = agree;
        } else {
            v = disagree;
        }
        VoteInfo.VoterInfo[] votedInfo = new VoteInfo.VoterInfo[v.voterInfoList.length + 1];
        VoteInfo.VoterInfo voterInfo = new VoteInfo.VoterInfo(
                Context.getTransactionHash(),
                BigInteger.valueOf(Context.getTransactionTimestamp()),
                voter.getAddress(),
                voter.getName(),
                voter.power()
        );
        System.arraycopy(v.voterInfoList, 0, votedInfo, 0, v.voterInfoList.length);
        votedInfo[v.voterInfoList.length] = voterInfo;
        v.setVoterInfoList(votedInfo);

        var votedAmount = v.getAmount();
        v.setAmount(votedAmount.add(voter.power()));

        updateNoVote(voter);
    }

    private void updateNoVote(PRepInfo prep) {
        var addresses = noVote.list;
        int size = sizeofNoVote();
        int index = 0;
        var updatedList = new Address[size - 1];
        for (int i = 0; i < size; i++) {
            if (!prep.getAddress().equals(addresses[i])) {
                updatedList[index++] = addresses[i];
            }
        }
        noVote.setAddressList(updatedList);
        var amount = noVote.getAmount();
        noVote.setAmount(amount.subtract(prep.power()));
    }
}
