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
}
