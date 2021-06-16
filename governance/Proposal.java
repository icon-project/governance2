package governance;

import score.Address;
import score.annotation.Keep;
import score.Context;
import score.ObjectWriter;

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

    private void toWriteWithAgree(ObjectWriter w, Voter v) {
        v.agree.writeObject(w, v.agree);
    }

    private void toWriteWithDisAgree(ObjectWriter w, Voter v) {
        v.disAgree.writeObject(w, v.disAgree);
    }

    private void toWriteWithNoVote(ObjectWriter w, Voter v) {
        v.noVote.writeObject(w, v.noVote);
    }

    public static void writeObject(ObjectWriter w, Voter v) {
        w.beginMap(3);
            w.write("agree");
            v.toWriteWithAgree(w, v);
            w.write("disagree");
            v.toWriteWithDisAgree(w, v);
            w.write("noVote");
            v.toWriteWithNoVote(w, v);
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
    byte[]  id;
    Address proposer;
    String  proposerName;
    String  title;
    String  description;
    int     type;
    byte[]  value;
    int     status;
    Voter   vote;
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
            int status,
            Voter vote,
            BigInteger  startBlockHeight,
            BigInteger  expireBlockHeight,
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
        this.totalBondedDelegation = totalBondedDelegation;
    }

    public static void writeObject(ObjectWriter w, Proposal p) {

    }
}
