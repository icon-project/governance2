package com.icon.governance;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.JsonObject;

import score.Address;
import score.Context;
import score.ObjectWriter;
import score.ObjectReader;

import java.math.BigDecimal;
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
    6 - STEP_COSTS

*/
public class Proposal {
    public static final int TEXT = 0;
    public static final int MIN = TEXT;
    public static final int REVISION = 1;
    public static final int MALICIOUS_SCORE = 2;
    public static final int PREP_DISQUALIFICATION = 3;
    public static final int STEP_PRICE = 4;
    public static final int IREP = 5;
    public static final int STEP_COSTS = 6;
    public static final int REWARD_FUND = 7;
    public static final int REWARD_FUNDS_ALLOCATION = 8;
    public static final int NETWORK_SCORE_DESIGNATION = 9;
    public static final int NETWORK_SCORE_UPDATE = 10;
    public static final int MAX = NETWORK_SCORE_UPDATE;
    public static final int FREEZE_SCORE = 0;
    public static final int UNFREEZE_SCORE = 1;
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

    public Map<String, Object> toMap() {
        return Map.ofEntries(
                Map.entry("id", id),
                Map.entry("proposer", proposer),
                Map.entry("proposer_name", proposerName),
                Map.entry("title", title),
                Map.entry("description", description),
                Map.entry("type", type),
                Map.entry("value", value.toMap()),
                Map.entry("start_block_height", startBlockHeight),
                Map.entry("end_block_height", expireBlockHeight),
                Map.entry("status", status),
                Map.entry("vote", vote.toMap()),
                Map.entry("total_voter", totalVoter),
                Map.entry("total_bonded_delegation", totalBondedDelegation)
        );
    }

    public static Proposal loadJson(byte[] data) {
        String jsonStr = new String(data);
        JsonValue json = Json.parse(jsonStr);
        JsonObject jsonObj = json.asObject();

        byte[] id = Converter.hexToBytes(
                "0x" + jsonObj.getString("id", null)
        );

        Address proposer = Converter.strToAddress(
                jsonObj.getString("proposer", null)
        );

        String proposerName = jsonObj.getString("proposer_name", null);
        String title = jsonObj.getString("title", null);
        String description = jsonObj.getString("description", null);

        int type = jsonObj.getInt("type", 0);
        Value value = Value.fromJson(
                type, jsonObj.get("value").asObject()
        );

        BigInteger  startBlockHeight = BigDecimal.valueOf(
                jsonObj.getDouble("start_block_height", 0)
        ).toBigInteger();

        BigInteger  expireBlockHeight = BigDecimal.valueOf(
                jsonObj.getDouble("end_block_height", 0)
        ).toBigInteger();

        int status = jsonObj.getInt("status", 0);
        Voter vote = Voter.makeVoterWithJson(jsonObj.get("vote"));

        int totalVoter = jsonObj.getInt("total_voter", 0);

        BigInteger  totalBondedDelegation = BigDecimal.valueOf(
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
        if (vote == Voter.AGREE_VOTE) {
            voteAgree(p);
        } else {
            voteDisagree(p);
        }
    }

    private void voteAgree(PRepInfo voter) {
        Voter.Vote[] updatedAgree = new Voter.Vote[vote.agree.voteList.length + 1];
        Voter.Vote v = new Voter.Vote(
                Context.getTransactionHash(),
                BigInteger.valueOf(Context.getTransactionTimestamp()),
                voter.getAddress(),
                voter.getName(),
                BigInteger.valueOf(Context.getTransactionTimestamp())
        );
        System.arraycopy(vote.agree.voteList, 0, updatedAgree, 0, vote.agree.voteList.length);
        updatedAgree[vote.agree.voteList.length] = v;
        vote.agree.setVoteList(updatedAgree);

        var votedAmount = vote.agree.getAmount();
        vote.agree.setAmount(votedAmount.add(voter.getBondedDelegation()));

        updateNoVote(voter);
    }

    private void voteDisagree(PRepInfo voter) {
        Voter.Vote[] updatedDisagree = new Voter.Vote[vote.disagree.voteList.length + 1];
        Voter.Vote v = new Voter.Vote(
                Context.getTransactionHash(),
                BigInteger.valueOf(Context.getTransactionTimestamp()),
                voter.getAddress(),
                voter.getName(),
                BigInteger.valueOf(Context.getTransactionTimestamp())
        );
        System.arraycopy(vote.disagree.voteList, 0, updatedDisagree, 0, vote.disagree.voteList.length);
        updatedDisagree[vote.disagree.voteList.length] = v;
        vote.disagree.setVoteList(updatedDisagree);

        var votedAmount = vote.disagree.getAmount();
        vote.disagree.setAmount(votedAmount.add(voter.getBondedDelegation()));

        updateNoVote(voter);
    }

    private  void updateNoVote(PRepInfo prep) {
        var addresses = vote.noVote.getAddressList();
        int size = vote.noVote.size();
        Address[] updatedList = new Address[size - 1];
        for (int i = 0; i < size; i++) {
            if (prep.getAddress().equals(addresses[i])) {
                continue;
            }
            updatedList[i] = addresses[i];
        }
        vote.noVote.setAddressList(updatedList);
        var amount = vote.noVote.getAmount();
        vote.noVote.setAmount(amount.subtract(prep.getBondedDelegation()));
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
}
