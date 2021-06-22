package com.icon.governance;

import com.eclipsesource.json.Json;

import score.Address;
import score.ObjectWriter;
import score.ObjectReader;

import java.math.BigInteger;
import java.util.Map;
import java.util.List;

/*
    VOTER STATUS
    2 - Pending (noVote) --> Slash **
    1 - Agree
    0 - DisAgree

    Condition Rate
    Approve - 0.66
    DisApprove - 0.33
*/
class Voter {
    private Agree agree;
    private DisAgree disAgree;
    private NoVote noVote;

    public static class Vote {
        private byte[] id;
        private String timestamp;
        private Address address;
        private String name;
        private BigInteger amount;

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

    public static class Agree {
        private List<Vote> voteList;
        private BigInteger amount;

        public Agree() {
            this.voteList = List.of();
            this.amount = BigInteger.ZERO;
        }

        public List<Vote> getVoteList() {
            return voteList;
        }

        public void setVoteList(List<Vote> voteList) {
            this.voteList = voteList;
        }

        public BigInteger getAmount() {
            return amount;
        }

        public void setAmount(BigInteger amount) {
            this.amount = amount;
        }

        public Map<String, ?> toMap() {
            return Map.of(
                    "list", voteList,
                    "amount", amount
            );
        }

        public static void writeObject(ObjectWriter w, Agree a) {
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


    public static class DisAgree {
        private List<Vote> voteList;
        private BigInteger amount;

        public DisAgree() {
            this.voteList = List.of();
            this.amount = BigInteger.ZERO;
        }

        public List<Vote> getVoteList() {
            return voteList;
        }

        public void setVoteList(List<Vote> voteList) {
            this.voteList = voteList;
        }

        public BigInteger getAmount() {
            return amount;
        }

        public void setAmount(BigInteger amount) {
            this.amount = amount;
        }

        public Map<String, ?> toMap() {
            return Map.of(
                    "list", voteList,
                    "amount", amount
            );
        }

        public static void writeObject(ObjectWriter w, DisAgree d) {
            w.beginMap(2);
                w.write("list");
                w.beginList(d.getVoteList().size());
                    for (Vote vote : d.getVoteList()) {
                        vote.writeObject(w, vote);
                    }
                w.end();

                w.write("amount");
                w.write(d.getAmount());
            w.end();
        }
    }

    public static class NoVote {
        private List<Address> list;
        private BigInteger amount;

        public NoVote() {
            this.list = List.of();
            this.amount = BigInteger.ZERO;
        }

        public List<Address> getList() {
            return list;
        }

        public void setList(List<Address> list) {
            this.list = list;
        }

        public BigInteger getAmount() {
            return amount;
        }

        public void setAmount(BigInteger amount) {
            this.amount = amount;
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
                w.beginList(n.getList().size());
                    for (Address addr : n.getList()) {
                        w.write(addr);
                    }
                w.end();

                w.write("amount");
                w.write(n.getAmount());
            w.end();
        }
    }

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

    private void writeWithAgree(ObjectWriter w, Voter v) {
        v.agree.writeObject(w, v.agree);
    }

    private void writeWithDisAgree(ObjectWriter w, Voter v) {
        v.disAgree.writeObject(w, v.disAgree);
    }

    private void writeWithNoVote(ObjectWriter w, Voter v) {
        v.noVote.writeObject(w, v.noVote);
    }

    public static void writeObject(ObjectWriter w, Voter v) {
        w.beginMap(3);
            w.write("agree");
            v.writeWithAgree(w, v);
            w.write("disagree");
            v.writeWithDisAgree(w, v);
            w.write("noVote");
            v.writeWithNoVote(w, v);
        w.end();
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
    byte[]  value;
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
            byte[] value,
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
                r.readByteArray(),
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

}
