/*
 * Copyright 2020 ICONLOOP Inc.
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

package foundation.icon.test.cases;

import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.Bytes;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.test.*;
import foundation.icon.test.score.ChainScore;
import foundation.icon.test.score.Delegation;
import foundation.icon.test.score.GovernanceScore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;

import static foundation.icon.test.Env.LOG;

public class GovernanceTest extends TestBase {
    private static final boolean DEBUG = true;
    private static final Address ZERO_ADDRESS = new Address("hx0000000000000000000000000000000000000000");
    private static TransactionHandler txHandler;
    private static ChainScore chainScore;

    @BeforeAll
    static void setup() throws Exception {
        Env.Chain chain = Env.getDefaultChain();
        IconService iconService = new IconService(new HttpProvider(chain.getEndpointURL(3)));
        txHandler = new TransactionHandler(iconService, chain);
        chainScore = new ChainScore(txHandler);
        var stakeAmount = ICX.multiply(BigInteger.valueOf(100000000));

        // register P-Rep
        var result = chainScore.registerPRep(
                chain.godWallet,
                "god",
                "god@god.com",
                "USA",
                "NY",
                "https://god.com",
                "https://god.com/god.json",
                "god.com:7100",
                chain.godWallet.getAddress());
       var transactionResult = txHandler.getResult(result);
       assertSuccess(transactionResult);

        //set Stake
        result = chainScore.setStake(chain.godWallet, stakeAmount);
        transactionResult = txHandler.getResult(result);
        assertSuccess(transactionResult);

        // set delegation
        Delegation[] delegations = new Delegation[1];
        var delegation = new Delegation(chain.godWallet.getAddress(), stakeAmount.divide(BigInteger.TWO));
        delegations[0] = delegation;
        result = chainScore.setDelegation(chain.godWallet, delegations);
        transactionResult = txHandler.getResult(result);
        assertSuccess(transactionResult);

        // set bond
        result = chainScore.setBond(chain.godWallet, delegations);
        transactionResult = txHandler.getResult(result);
        assertSuccess(transactionResult);
    }

    @Test
    public void testGovernance() throws ResultTimeoutException, TransactionFailureException, IOException {
        GovernanceScore governanceScore = GovernanceScore.update(txHandler);
    }

}
