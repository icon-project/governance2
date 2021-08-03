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

package foundation.icon.test.score;

import foundation.icon.icx.Wallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.Bytes;
import foundation.icon.icx.transport.jsonrpc.RpcArray;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import foundation.icon.test.Constants;
import foundation.icon.test.TransactionHandler;

import java.io.IOException;
import java.math.BigInteger;

import static foundation.icon.test.TestBase.ICX;

public class ChainScore extends Score {

    public ChainScore(TransactionHandler txHandler) {
        super(txHandler, Constants.CHAIN_SCORE_ADDRESS);
    }

    public BigInteger getStepPrice() throws IOException {
        return call("getStepPrice", null).asInteger();
    }

    public Bytes registerPRep(
            Wallet wallet,
            String name,
            String email,
            String country,
            String city,
            String website,
            String details,
            String p2pEndpoint,
            Address node
            ) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("name", new RpcValue(name))
                .put("email", new RpcValue(email))
                .put("country", new RpcValue(country))
                .put("city", new RpcValue(city))
                .put("website", new RpcValue(website))
                .put("details", new RpcValue(details))
                .put("p2pEndpoint", new RpcValue(p2pEndpoint))
                .put("nodeAddress", new RpcValue(node))
                .build();
        return invoke(wallet, "registerPRep", params, ICX.multiply(BigInteger.valueOf(2000)), null);
    }

    public Bytes setStake(Wallet wallet, BigInteger value) throws IOException {

        RpcObject params = new RpcObject.Builder()
                .put("value", new RpcValue(value))
                .build();
        return invoke(wallet, "setStake", params, BigInteger.ZERO, null);
    }

    public Bytes setBond(Wallet wallet, Delegation[] delegations) throws IOException {
        var rpcBonds = getRpcDelegations(delegations);
        RpcObject params = new RpcObject.Builder()
                .put("bonds", rpcBonds)
                .build();
        return invoke(wallet, "setBond", params);
    }

    public Bytes setDelegation(Wallet wallet, Delegation[] delegations) throws IOException {
        var rpcDelegations = getRpcDelegations(delegations);
        RpcObject params = new RpcObject.Builder()
                .put("delegations", rpcDelegations)
                .build();
        return invoke(wallet, "setDelegation", params);
    }

    private RpcArray getRpcDelegations(Delegation[] delegations) {
        var rpcArrayBuilder = new RpcArray.Builder();
        for (Delegation delegation : delegations) {
            rpcArrayBuilder.add(new RpcObject.Builder()
                    .put("address", new RpcValue(delegation.address))
                    .put("value", new RpcValue(delegation.value))
                    .build()
            );
        }
        return rpcArrayBuilder.build();
    }
}
