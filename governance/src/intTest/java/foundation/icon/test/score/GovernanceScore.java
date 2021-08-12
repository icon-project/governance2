/*
 * Copyright 2020 ICON Foundation
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
import foundation.icon.icx.data.Bytes;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import foundation.icon.test.*;

import java.io.IOException;
import java.math.BigInteger;

import static foundation.icon.test.Env.LOG;

public class GovernanceScore extends Score {
    public GovernanceScore(Score other) {
        super(other);
    }

    public static GovernanceScore update(TransactionHandler txHandler, Wallet owner)
            throws ResultTimeoutException, TransactionFailureException, IOException {
        LOG.infoEntering("deploy", "Governance");
        RpcObject params = new RpcObject.Builder()
                .build();
        Score score = txHandler.deploy(
                owner,
                getFilePath("governance"),
                Constants.GOVERNANCE_ADDRESS,
                params,
                null
        );
        LOG.info("scoreAddr = " + Constants.GOVERNANCE_ADDRESS);
        LOG.infoExiting();
        return new GovernanceScore(score);
    }

    public BigInteger getProposals() throws IOException {
        RpcObject params = new RpcObject.Builder().build();
        return call("getProposals", params).asInteger();
    }

    public RpcObject getProposal(byte[] id) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("id", new RpcValue(id))
                .build();
        return call("getProposal", params).asObject();
    }

    public Bytes registerProposal(Wallet wallet, String title, String description, BigInteger type, byte[] value) throws IOException {
        LOG.infoEntering("register Proposal");
        RpcObject params = new RpcObject.Builder()
                .put("title", new RpcValue(title))
                .put("description", new RpcValue(description))
                .put("type", new RpcValue(type))
                .put("value", new RpcValue(value))
                .build();
        LOG.info("register proposal done : " + title);
        LOG.infoExiting();
        return invoke(wallet, "registerProposal", params, TestBase.ICX.multiply(BigInteger.valueOf(100)), Constants.DEFAULT_STEPS.multiply(BigInteger.valueOf(5)));
    }

    public Bytes voteProposal(Wallet wallet, byte[] id, BigInteger vote) throws IOException {
        LOG.infoEntering("vote Proposal");
        RpcObject params = new RpcObject.Builder()
                .put("id", new RpcValue(id))
                .put("vote", new RpcValue(vote))
                .build();
        LOG.info("vote proposal done");
        LOG.infoExiting();
        return invoke(wallet, "voteProposal", params, BigInteger.ZERO, Constants.DEFAULT_STEPS.multiply(BigInteger.valueOf(5)));
    }

    public Bytes cancelProposal(Wallet wallet, byte[] id) throws IOException {
        LOG.infoEntering("cancel Proposal");
        RpcObject params = new RpcObject.Builder()
                .put("id", new RpcValue(id))
                .build();
        LOG.info("cancel proposal done : ");
        LOG.infoExiting();
        return invoke(wallet, "cancelProposal", params);
    }

    public RpcObject getPRepTerm() throws IOException {
        RpcObject params = new RpcObject.Builder().build();
        return call("getPRepTerm", params).asObject();
    }
}
