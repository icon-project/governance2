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

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import score.Address;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

public class Proposal {
    public static final int TEXT = 0;
    public static final int REVISION = 1;
    public static final int MALICIOUS_SCORE = 2;
    public static final int PREP_DISQUALIFICATION = 3;
    public static final int STEP_PRICE = 4;
    public static final int IREP = 5;
    public static final int STEP_COSTS = 6;
    public static final int REWARD_FUND = 7;
    public static final int REWARD_FUNDS_ALLOCATION = 8;
    public static final int NETWORK_PROPOSAL = 9;

    public static final int MIN = TEXT;
    public static final int MAX = NETWORK_PROPOSAL;

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
    VoteInfo vote;
    int totalVoter;
    BigInteger totalPower;

    public Proposal(
            byte[] id,
            Address proposer,
            String proposerName,
            String title,
            String description,
            int type,
            Value value,
            BigInteger startBlockHeight,
            BigInteger expireBlockHeight,
            int status,
            VoteInfo vote,
            int totalVoter,
            BigInteger totalPower
    ) {
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
        this.totalPower = totalPower;
    }

    public static void writeObject(ObjectWriter w, Proposal p) {
        w.beginList(13);
        w.write(p.id);
        w.write(p.proposer);
        w.write(p.proposerName);
        w.write(p.title);
        w.write(p.description);
        w.write(p.type);
        w.writeNullable(p.value);
        w.write(p.startBlockHeight);
        w.write(p.expireBlockHeight);
        w.write(p.status);
        w.write(p.vote);
        w.write(p.totalVoter);
        w.write(p.totalPower);
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
                r.readNullable(Value.class),
                r.readBigInteger(),
                r.readBigInteger(),
                r.readInt(),
                r.read(VoteInfo.class),
                r.readInt(),
                r.readBigInteger()
        );
        r.end();
        return p;
    }

    public boolean isExpired(BigInteger blockHeight) {
        return blockHeight.compareTo(expireBlockHeight) > 0;
    }

    public int getStatus(BigInteger blockHeight) {
        boolean expired = isExpired(blockHeight);
        if (expired && status == NetworkProposal.VOTING_STATUS) {
            return NetworkProposal.DISAPPROVED_STATUS;
        } else if (expired && status == NetworkProposal.APPROVED_STATUS) {
            return NetworkProposal.EXPIRED_STATUS;
        }
        return status;
    }

    public Map<String, Object> toMap(BigInteger blockHeight) {
        var contents = Map.of("description", description, "title", title, "type", type, "value", value.toMap());
        return Map.ofEntries(
                Map.entry("id", id),
                Map.entry("proposer", proposer),
                Map.entry("proposerName", proposerName),
                Map.entry("contents", contents),
                Map.entry("startBlockHeight", startBlockHeight),
                Map.entry("endBlockHeight", expireBlockHeight),
                Map.entry("status", getStatus(blockHeight)),
                Map.entry("vote", vote.toMap())
        );
    }

    public Map<String, Object> getSummary(BigInteger blockHeight) {
        var contents = Map.of("description", description, "title", title, "type", type);
        return Map.ofEntries(
                Map.entry("id", id),
                Map.entry("proposer", proposer),
                Map.entry("proposerName", proposerName),
                Map.entry("contents", contents),
                Map.entry("startBlockHeight", startBlockHeight),
                Map.entry("endBlockHeight", expireBlockHeight),
                Map.entry("status", getStatus(blockHeight)),
                Map.entry("vote", vote.getSummary())
        );
    }

    public static Proposal loadJson(byte[] data) {
        String jsonStr = new String(data);
        JsonValue json = Json.parse(jsonStr);
        JsonObject jsonObj = json.asObject();

        byte[] id = Converter.hexToBytes(
                "0x" + jsonObj.getString("id", null)
        );

        Address proposer = Converter.toAddress(
                jsonObj.getString("proposer", null)
        );

        String proposerName = jsonObj.getString("proposer_name", null);
        String title = jsonObj.getString("title", null);
        String description = jsonObj.getString("description", null);

        int type = jsonObj.getInt("type", 0);
        Value value = Value.fromJson(
                type, jsonObj.get("value").asObject()
        );

        BigInteger startBlockHeight = BigDecimal.valueOf(
                jsonObj.getDouble("start_block_height", 0)
        ).toBigInteger();

        BigInteger expireBlockHeight = BigDecimal.valueOf(
                jsonObj.getDouble("end_block_height", 0)
        ).toBigInteger();

        int status = jsonObj.getInt("status", 0);
        VoteInfo vote = VoteInfo.makeVoterWithJson(jsonObj.get("vote"));

        int totalVoter = jsonObj.getInt("total_voter", 0);

        BigInteger totalBondedDelegation = BigDecimal.valueOf(
                jsonObj.getDouble("total_delegated_amount", 0)
        ).toBigInteger();

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

    public void updateVote(PRepInfo p, int vote) {
        if (vote == VoteInfo.AGREE_VOTE) {
            voteAgree(p);
        } else {
            voteDisagree(p);
        }
    }

    private void voteAgree(PRepInfo voter) {
        VoteInfo.Vote[] updatedAgree = new VoteInfo.Vote[vote.sizeofAgreed() + 1];
        VoteInfo.Vote v = new VoteInfo.Vote(
                Context.getTransactionHash(),
                BigInteger.valueOf(Context.getTransactionTimestamp()),
                voter.getAddress(),
                voter.getName(),
                voter.power()
        );
        System.arraycopy(vote.agree.voteList, 0, updatedAgree, 0, vote.sizeofAgreed());
        updatedAgree[vote.sizeofAgreed()] = v;
        vote.agree.setVoteList(updatedAgree);

        var votedAmount = vote.agree.getAmount();
        vote.agree.setAmount(votedAmount.add(voter.power()));

        updateNoVote(voter);
    }

    private void voteDisagree(PRepInfo voter) {
        VoteInfo.Vote[] updatedDisagree = new VoteInfo.Vote[vote.sizeofDisagreed() + 1];
        VoteInfo.Vote v = new VoteInfo.Vote(
                Context.getTransactionHash(),
                BigInteger.valueOf(Context.getTransactionTimestamp()),
                voter.getAddress(),
                voter.getName(),
                voter.power()
        );
        System.arraycopy(vote.disagree.voteList, 0, updatedDisagree, 0, vote.sizeofDisagreed());
        updatedDisagree[vote.sizeofDisagreed()] = v;
        vote.disagree.setVoteList(updatedDisagree);

        var votedAmount = vote.disagree.getAmount();
        vote.disagree.setAmount(votedAmount.add(voter.power()));

        updateNoVote(voter);
    }

    private void updateNoVote(PRepInfo prep) {
        var addresses = vote.noVote.getAddressList();
        int size = vote.sizeofNoVote();
        int index = 0;
        var updatedList = new Address[size - 1];
        for (int i = 0; i < size; i++) {
            if (!prep.getAddress().equals(addresses[i])) {
                updatedList[index++] = addresses[i];
            }
        }
        vote.noVote.setAddressList(updatedList);
        var amount = vote.noVote.getAmount();
        vote.noVote.setAmount(amount.subtract(prep.power()));
    }

    boolean agreed(Address prep) {
        return vote.agreed(prep);
    }

    boolean disagreed(Address prep) {
        return vote.disagreed(prep);
    }

    boolean isInNoVote(Address prep) {
        return vote.isInNoVote(prep);
    }

    int sizeofAgreed() {
        return vote.sizeofAgreed();
    }

    int sizeofDisagreed() {
        return vote.sizeofDisagreed();
    }

    BigInteger amountOfAgreed() {
        return vote.amountOfAgreed();
    }

    BigInteger amountOfDisagreed() {
        return vote.amountOfDisagreed();
    }

    int sizeofNoVote() {
        return vote.sizeofNoVote();
    }

    Address[] getNonVoters() {
        return vote.noVote.getAddressList();
    }
}
