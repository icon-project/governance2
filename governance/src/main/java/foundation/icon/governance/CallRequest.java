package foundation.icon.governance;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;
import score.Address;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;
import scorex.util.HashMap;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class CallRequest {
    final static String SET_REVISION = "setRevision";
    final static String UNBLOCK_SCORE = "unblockScore";
    final static String BLOCK_SCORE = "blockScore";
    final static String DISQUALIFY_PREP = "disqualifyPRep";
    final static String SET_STEP_PRICE = "setStepPrice";
    final static String SET_STEP_COST = "setStepCost";
    final static String SET_REWARD_FUND = "setRewardFund";
    final static String SET_REWARD_FUND_ALLOCATION = "setRewardFundAllocation";
    final static String SET_NETWORK_SCORE = "setNetworkScore";
    final static String UPDATE_NETWORK_SCORE = "updateNetworkScore";
    final static String CONSISTENT_VALIDATION_PENALTY = "setConsistentValidationSlashingRate";
    final static String NON_VOTE_SLASHING_RATE = "setNonVoteSlashingRate";
    private Address to;
    private String method;
    private Param[] params;

    public CallRequest(Address to, String method, Param[] params) {
        this.to = to;
        this.method = method;
        this.params = params;
    }

    public static class Param {
        private String type;
        private String value;
        private Map<String, String> fields;

        public Param(String type, String value, Map<String, String> fields) {
            this.type = type;
            this.value = value;
            this.fields = fields;
        }

        public static Param readObject(ObjectReader r) {
            r.beginList();
            var type = r.readString();
            var value = r.readString();
            List<String> fields = new ArrayList<>();
            r.beginList();
            while (r.hasNext()) {
                fields.add(r.readString());
            }
            r.end();
            Map<String, String> fieldsMap = new HashMap();
            for (int i = 0; i < fields.size() / 2; i++) {
                var key = fields.get(i * 2);
                var v = fields.get(i * 2 + 1);
                fieldsMap.put(key, v);
            }
            r.end();
            return new Param(type, value, fieldsMap);
        }

        public static void writeObject(ObjectWriter w, Param p) {
            w.beginList(3);
            w.write(p.type);
            w.write(p.value);
            w.beginList(p.fields.size() * 2);
            for (String key : p.fields.keySet()) {
                w.write(key);
                w.write(p.fields.get(key));
            }
            w.end();
            w.end();
        }

        public Object getParam() {
            return convertParams(type, value, fields);
        }

        public static Object convertParams(String type, String value, Map<String, String> fields) {
            switch (type) {
                case "Address":
                    return Address.fromString(value);
                case "str":
                    return value;
                case "int": {
                    return Converter.toInteger(value);
                }
                case "bool": {
                    if (value.equals("0x0") || value.equals("false")) {
                        return Boolean.FALSE;
                    } else if (value.equals("0x1") || value.equals("true")) {
                        return Boolean.TRUE;
                    }
                    break;
                }
                case "bytes": {
                    if (value.startsWith("0x") && (value.length() % 2 == 0)) {
                        String hex = value.substring(2);
                        int len = hex.length() / 2;
                        byte[] bytes = new byte[len];
                        for (int i = 0; i < len; i++) {
                            int j = i * 2;
                            bytes[i] = (byte) Integer.parseInt(hex.substring(j, j + 2), 16);
                        }
                        return bytes;
                    }
                    break;
                }
                case "struct": {
                    var v = Json.parse(value).asObject();
                    var map = new HashMap<String, Object>();
                    for (String key : fields.keySet()) {
                        var t = fields.get(key);
                        map.put(key, convertParams(t, v.get(key).asString(), fields));
                    }
                    return map;
                }
                case "[]struct": {
                    var v = Json.parse(value).asArray();
                    var list = new ArrayList<Map<String, Object>>();
                    for (int i = 0; i < v.size(); i++) {
                        for (String key : fields.keySet()) {
                            var t = fields.get(key);
                            list.add(Map.of(key, convertParams(t, v.get(i).asObject().get(key).asString(), fields)));
                        }
                    }
                    return list;
                }
                case "[]Address": {
                    var v = Json.parse(value).asArray();
                    var list = new ArrayList<Address>();
                    for (JsonValue jsonValue : v) {
                        list.add(Address.fromString(jsonValue.asString()));
                    }
                    return list;
                }
                case "[]int": {
                    var v = Json.parse(value).asArray();
                    var list = new ArrayList<BigInteger>();
                    for (JsonValue jsonValue : v) {
                        list.add(Converter.toInteger(jsonValue.asString()));
                    }
                    return list;
                }
                case "[]bool": {
                    var v = Json.parse(value).asArray();
                    var list = new ArrayList<Boolean>();
                    for (JsonValue jsonValue : v) {
                        String strVal = jsonValue.asString();
                        if (strVal.equals("0x0") || strVal.equals("false")) {
                            list.add(Boolean.FALSE);
                        } else if (strVal.equals("0x1") || strVal.equals("true")) {
                            list.add(Boolean.TRUE);
                        }
                    }
                    return list;
                }
                case "[]str": {
                    var v = Json.parse(value).asArray();
                    var list = new ArrayList<String>();
                    for (JsonValue jsonValue : v) {
                        list.add(jsonValue.asString());
                    }
                    return list;
                }
            }
            throw new IllegalArgumentException("Unknown type");
        }

        public Map<String, Object> toMap() {
            return Map.of(
                    "type", type,
                    "value", value,
                    "fields", fields
            );
        }
    }

    public static CallRequest fromJson(JsonValue json) {
        var object = json.asObject();
        Context.require(object.size() == 3, "key size must be 3");
        var method = object.getString("method", "");
        var to = Address.fromString(object.getString("to", ""));
        var params = object.get("params").asArray();
        var paramsLength = params.size();
        Param[] pArray = new Param[paramsLength];
        for (int i = 0; i < paramsLength; i++) {
            var param = params.get(i).asObject();
            var type = param.getString("type", null);
            var value = param.get("value");
            var stringValue = getStringValue(value);
            var fields = param.get("fields") == null ? null : param.get("fields").asObject();
            Map<String, String> fieldsMap = new HashMap<>();
            if (fields != null) {
                for (String key : fields.names()) {
                    fieldsMap.put(key, fields.get(key).asString());
                }
            }
            pArray[i] = new Param(type, stringValue, fieldsMap);
        }
        return new CallRequest(to, method, pArray);
    }

    private static String getStringValue(JsonValue jsonValue) {
        if (jsonValue.isString()) {
            return jsonValue.asString();
        } else if (jsonValue.isArray()) {
            return jsonValue.asArray().toString();
        } else if (jsonValue.isObject()) {
            return jsonValue.asObject().toString();
        }
        throw new IllegalArgumentException("invalid value type. value type must be string, struct, []struct");
    }

    public static void writeObject(ObjectWriter w, CallRequest m) {
        w.beginList(3);
        w.write(m.to);
        w.write(m.method);
        w.beginList(m.params.length);
        for (int i = 0; i < m.params.length; i++) {
            w.write(m.params[i]);
        }
        w.end();
        w.end();
    }

    public static CallRequest readObject(ObjectReader r) {
        r.beginList();
        var to = r.readAddress();
        var method = r.readString();
        r.beginList();
        ArrayList<Param> paramList = new ArrayList<>();
        while (r.hasNext()) {
            paramList.add(r.read(Param.class));
        }
        Param[] params = new Param[paramList.size()];
        for (int i = 0; i < params.length; i++) {
            params[i] = paramList.get(i);
        }
        r.end();
        r.end();
        return new CallRequest(to, method, params);
    }

    public Object[] getParams() {
        Object[] pArray = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            pArray[i] = params[i].getParam();
        }
        return pArray;
    }

    public Map<String, Object> toMap() {
        ArrayList paramList = new ArrayList();
        for (Param p : params) {
            paramList.add(p.toMap());
        }
        return Map.of(
                "to", to,
                "method", method,
                "params", List.of(paramList.toArray())
        );
    }

    public void validateRequest() {
        if (!to.equals(ChainScore.ADDRESS)) return;
        var ps = getParams();
        switch (method) {
            case SET_REVISION:
                validateRevision(ps);
                break;
            case UNBLOCK_SCORE:
                Context.require(ps[0] instanceof Address);
                break;
            case BLOCK_SCORE:
                validateBlockScore(ps);
                break;
            case DISQUALIFY_PREP:
                validateDisqualifyPRep(ps);
                break;
            case SET_STEP_PRICE:
                validateStepPrice(ps);
                break;
            case SET_REWARD_FUND:
                ChainScore.validateRewardFund((BigInteger) ps[0]);
                break;
            case SET_REWARD_FUND_ALLOCATION:
                validateRewardFundsRate(ps);
                break;
            case SET_NETWORK_SCORE:
                validateSetNetworkScore(ps);
                break;
            case UPDATE_NETWORK_SCORE:
                Context.require(2 == ps.length || ps.length ==3);
                Context.require(ps[0] instanceof Address && ps[1] instanceof byte[]);
                var owner = ChainScore.getScoreOwner((Address) ps[0]);
                Context.require(owner.equals(Governance.address));
                if (ps.length == 3) Context.require(ps[2] instanceof String[]);
                break;
            case SET_STEP_COST:
                Context.require(ps.length == 2);
                Context.require(ps[0] instanceof String && ps[1] instanceof BigInteger);
                break;
            case CONSISTENT_VALIDATION_PENALTY:
            case NON_VOTE_SLASHING_RATE:
                Context.require(ps[0] instanceof BigInteger);
                break;
        }
    }

    public void handleRequest(Governance governance) {
        var ps = getParams();
        if (!to.equals(Governance.address)) {
            Context.call(to, method, ps);
            return;
        }
        if (method.equals(UPDATE_NETWORK_SCORE)) {
            String[] scoreParams = null;
            if (ps.length == 3) {
                var c = (ArrayList<String>)ps[2];
                scoreParams = new String[c.size()];
                for (int i = 0; i < scoreParams.length; i++) scoreParams[i] = c.get(i);
            }
            governance.deployScore((Address)ps[0], (byte[]) ps[1], scoreParams);
        }
    }

    private void validateRevision(Object... values) {
        Context.require(values.length == 1);
        var prev = ChainScore.getRevision();
        var revision = (BigInteger)values[0];
        Context.require(revision.compareTo(prev) > 0, "can not decrease revision");
    }

    private void validateBlockScore(Object... values) {
        Context.require(values.length == 1);
        var address = (Address) values[0];
        if (address.equals(Governance.address)) Context.revert("Can not freeze governance SCORE");
    }

    private void validateDisqualifyPRep(Object... values) {
        Context.require(values.length == 1);
        var address = (Address) values[0];
        PRepInfo[] preps = ChainScore.getPRepsInfo();
        Context.require(Governance.getPRepInfoFromList(address, preps) != null, address.toString() + " is not p-rep");
    }

    private void validateStepPrice(Object... values) {
        Context.require(values.length == 1);
        var price = (BigInteger) values[0];
        var prevPrice = ChainScore.getStepPrice();
        var hundred = BigInteger.valueOf(100);
        var max = prevPrice.multiply(BigInteger.valueOf(125)).divide(hundred);
        var min = prevPrice.multiply(BigInteger.valueOf(75)).divide(hundred);
        Context.require(price.compareTo(min) >= 0 && price.compareTo(max) <= 0, "Invalid step price: " + price);
    }

    private void validateRewardFundsRate(Object... values) {
        Context.require(values.length == 4, "InvalidFundType");
        var sum = BigInteger.ZERO;
        for (Object value : values) {
            var rate = (BigInteger) value;
            if (BigInteger.ZERO.compareTo(rate) > 0) {
                Context.revert("reward fund < 0");
            }
            sum = sum.add(rate);
        }
        Context.require(sum.compareTo(BigInteger.valueOf(100)) == 0, "sum of reward funds must be 100");
    }

    private void validateSetNetworkScore(Object... values) {
        Context.require(values.length == 4, "InvalidFundType");
        var address = (Address) values[0];
        var role = (String) values[1];
        Context.require(Value.CPS_SCORE.equals(role) || Value.RELAY_SCORE.equals(role),
                "Invalid network SCORE role: " + role);
        if (address == null) return;
        Address owner = ChainScore.getScoreOwner(address);
        Context.require(owner.equals(Governance.address), "Only owned by governance can be designated");
    }
}
