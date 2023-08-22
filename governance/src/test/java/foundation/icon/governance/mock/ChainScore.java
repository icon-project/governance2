/*
 * Copyright 2023 ICON Foundation
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

package foundation.icon.governance.mock;

import foundation.icon.governance.Converter;
import score.Address;
import score.Context;
import score.annotation.External;
import score.annotation.Payable;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class ChainScore {
    public static final Address ADDRESS = Address.fromString("cx0000000000000000000000000000000000000000");
    public static final Address GOV_ADDRESS = Address.fromString("cx0000000000000000000000000000000000000001");

    @Payable
    @External
    public void burn() {
        Context.println(">>> ChainScore.burn, value=" + Context.getValue());
    }

    @External
    public void claimIScore() {
        Context.println(">>> ChainScore.claimIScore");
    }

    @External
    public void addTimer(BigInteger height) {
        Context.println(">>> ChainScore.addTimer, height=" + height);
    }

    @External(readonly=true)
    public BigInteger getRevision() {
        final BigInteger rev = BigInteger.valueOf(20);
        Context.println(">>> ChainScore.getRevision, rev=" + rev);
        return rev;
    }

    @External(readonly=true)
    public Address getScoreOwner(Address address) {
        Context.println(">>> ChainScore.getScoreOwner, address=" + address);
        return GOV_ADDRESS;
    }

    @External(readonly=true)
    public Map<String, Object> getPRepTerm() {
        Context.println(">>> ChainScore.getPRepTerm");
        return Map.ofEntries(
                Map.entry("period", BigInteger.valueOf(1000)),
                Map.entry("endBlockHeight", BigInteger.valueOf(200))
        );
    }

    @External(readonly=true)
    public Map<String, Object> getPRep(Address address) {
        Context.println(">>> ChainScore.getPRep, address=" + address);
        return Map.ofEntries(
                Map.entry("name", "TestPRep1"),
                Map.entry("address", address),
                Map.entry("power", BigInteger.valueOf(1_000_000L)),
                Map.entry("grade", BigInteger.ZERO),
                Map.entry("status", BigInteger.ZERO)
        );
    }

    @External(readonly=true)
    public Map<String, List<Map<String, Object>>> getMainPReps() {
        Context.println(">>> ChainScore.getMainPReps");
        return Map.of("preps", List.of(
                Map.ofEntries(
                        Map.entry("name", "TestPRep1"),
                        Map.entry("address", Address.fromString("hx0000000000000000000000000000000000000100")),
                        Map.entry("power", BigInteger.valueOf(1_000_000L))
                ),
                Map.ofEntries(
                        Map.entry("name", "TestPRep2"),
                        Map.entry("address", Address.fromString("hx0000000000000000000000000000000000000101")),
                        Map.entry("power", BigInteger.valueOf(2_000_000L))
                )
            )
        );
    }

    @External
    public void openBTPNetwork(String netType, String name, Address owner) {
        Context.println(">>> ChainScore.openBTPNetwork, netType=" + netType + ", name=" + name + ", owner=" + owner);
    }

    @External
    public void setRewardFundAllocation(BigInteger iprep, BigInteger icps, BigInteger irelay, BigInteger ivoter) {
        var sum = iprep.add(icps).add(irelay).add(ivoter);
        Context.println(">>> ChainScore.setRewardFundAllocation, sum=" + sum);
        Context.println("  - iprep=" + iprep + ", icps=" + icps + ", irelay=" + irelay + ", ivoter=" + ivoter);
        Context.require(sum.equals(BigInteger.valueOf(100)));
    }

    @External
    public void penalizeNonvoters(Address[] preps) {
        StringBuilder prepAddresses = new StringBuilder("[");
        int count = preps.length;
        for (Address p : preps) {
            prepAddresses.append(p.toString());
            if (--count > 0) {
                prepAddresses.append(", ");
            }
        }
        prepAddresses.append("]");
        Context.println(">>> ChainScore.penalizeNonvoters, preps=" + prepAddresses);
    }

    static public class Delegation {
        private Address address;
        private BigInteger value;

        public Address getAddress() {
            return address;
        }

        public BigInteger getValue() {
            return value;
        }

        public void setAddress(Address address) {
            this.address = address;
        }

        public void setValue(BigInteger value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "Delegation{" +
                    "address=" + address +
                    ", value=" + value +
                    '}';
        }
    }

    @External
    public void setDelegation(Delegation[] delegations) {
        Context.println(">>> ChainScore.setDelegation, len=" + delegations.length);
        for (var d : delegations) {
            Context.println("  - " + d);
        }
    }

    @External
    public void testParamCall(boolean bool, byte[] bytes) {
        Context.println(">>> ChainScore.testParamCall");
        Context.println("  - bool=" + bool);
        Context.println("  - bytes=" + Converter.bytesToHex(bytes));
    }

    @External
    public void testStructCall(Delegation delegation) {
        Context.println(">>> ChainScore.testStructCall");
        Context.println("  - " + delegation);
    }

    @External
    public void testArrayCall(String[] strs, BigInteger[] ints, boolean[] bools) {
        Context.println(">>> ChainScore.testArrayCall");
        var sb = new StringBuilder("String[]{");
        for (String s : strs) {
            sb.append(s).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("}");
        Context.println("  - " + sb);

        var sb2 = new StringBuilder("BigInteger[]{");
        for (BigInteger i : ints) {
            sb2.append(i).append(",");
        }
        sb2.deleteCharAt(sb2.length() - 1);
        sb2.append("}");
        Context.println("  - " + sb2);

        var sb3 = new StringBuilder("bools[]{");
        for (boolean b : bools) {
            sb3.append(b).append(",");
        }
        sb3.deleteCharAt(sb3.length() - 1);
        sb3.append("}");
        Context.println("  - " + sb3);
    }
}
