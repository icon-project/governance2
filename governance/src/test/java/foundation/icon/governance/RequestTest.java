/*
 * Copyright 2022 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the \"License\");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an \"AS IS\" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package foundation.icon.governance;

import org.junit.jupiter.api.Test;
import score.Address;
import score.UserRevertedException;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class RequestTest {

    @Test
    void initValidCallRequest() {
        // validate ChainScore.blockScore
        var to = ChainScore.ADDRESS;
        var method = "blockScore";
        var params = new Param[1];
        var param0 = new Param();
        param0.setValue("cxaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        param0.setType("Address");
        params[0] = param0;
        Request callRequest = new Request();
        callRequest.setTo(to);
        callRequest.setMethod(method);
        callRequest.setParams(params);
        callRequest.validateRequest();

        // validate request with struct parameter
        to = Address.fromString("cx0000000000000000000000000000000000000000");
        method = "method2";
        param0.setType("struct");
        param0.setValue("{\"field1\": \"Bob\",\"field2\": \"hxb6b5791be0b5ef67063b3c10b840fb81514db2fd\"}");
        Param.Field[] fields = new Param.Field[2];
        Param.Field field0 = new Param.Field();
        Param.Field field1 = new Param.Field();
        field0.setName("field1");
        field0.setType("str");
        field1.setName("field2");
        field1.setType("Address");
        fields[0] = field0;
        fields[1] = field1;
        param0.setFields(fields);
        params[0] = param0;
        callRequest.setParams(params);
        callRequest.setMethod(method);
        callRequest.setTo(to);
        callRequest.validateRequest();

        //validate request with []struct parameter
        to = Address.fromString("cx0000000000000000000000000000000000000000");
        method = "method3";
        param0.setType("[]struct");
        param0.setValue("[{\"name\": \"bob\",\"address\": \"hx1111111111111111111111111111111111111111\"},{\"name\": \"alice\", \"address\": \"hx2111111111111111111111111111111111111111\"}]");
        fields = new Param.Field[2];
        field0 = new Param.Field();
        field1 = new Param.Field();
        field0.setName("name");
        field0.setType("str");
        field1.setName("address");
        field1.setType("Address");
        fields[0] = field0;
        fields[1] = field1;
        param0.setFields(fields);
        params[0] = param0;
        callRequest.setParams(params);
        callRequest.setMethod(method);
        callRequest.setTo(to);
        callRequest.validateRequest();

        //validate request for rewardFundAllocation
        method = "setRewardFundAllocation";
        param0 = new Param();
        Param param1 = new Param();
        Param param2 = new Param();
        Param param3 = new Param();
        param0.setType("int");
        param0.setValue("0x19");
        param1.setType("int");
        param1.setValue("0x19");
        param2.setType("int");
        param2.setValue("0x19");
        param3.setType("int");
        param3.setValue("0x19");
        params = new Param[4];
        params[0] = param0; params[1] = param1; params[2] = param2; params[3] = param3;
        callRequest.setMethod(method);
        callRequest.setParams(params);
        callRequest.validateRequest();
    }

    @Test
    void initInvalidCallRequests() {
        // validate ChainScore.blockScore. try to block governance
        var to = ChainScore.ADDRESS;
        var method = "blockScore";
        var params = new Param[1];
        var param0 = new Param();
        param0.setValue(Governance.address.toString());
        param0.setType("Address");
        params[0] = param0;
        Request callRequest = new Request();
        callRequest.setTo(to);
        callRequest.setMethod(method);
        callRequest.setParams(params);
        assertThrows(UserRevertedException.class, callRequest::validateRequest);

        //validate request for rewardFundAllocation(25, 25, 25, 24)
        method = "setRewardFundAllocation";
        param0 = new Param();
        Param param1 = new Param();
        Param param2 = new Param();
        Param param3 = new Param();
        param0.setType("int");
        param0.setValue("0x19");
        param1.setType("int");
        param1.setValue("0x19");
        param2.setType("int");
        param2.setValue("0x19");
        param3.setType("int");
        param3.setValue("0x18");
        params = new Param[4];
        params[0] = param0; params[1] = param1; params[2] = param2; params[3] = param3;
        callRequest.setMethod(method);
        callRequest.setParams(params);
        assertThrows(UserRevertedException.class, callRequest::validateRequest);

        //validate request for rewardFundAllocation(33, 33, 34)
        param0 = new Param();
        param1 = new Param();
        param2 = new Param();
        param0.setType("int");
        param0.setValue("0x21");
        param1.setType("int");
        param1.setValue("0x21");
        param2.setType("int");
        param2.setValue("0x22");
        params = new Param[3];
        params[0] = param0; params[1] = param1; params[2] = param2;
        callRequest.setMethod(method);
        callRequest.setParams(params);
        assertThrows(UserRevertedException.class, callRequest::validateRequest);
    }
}
