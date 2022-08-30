package foundation.icon.governance;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import score.Address;
import score.Context;
import scorex.util.ArrayList;
import scorex.util.HashMap;

import java.math.BigInteger;
import java.util.Map;

public class Request {
    private Address to;
    private String method;
    private Param[] params;

    public Request(Address to, String method, Param[] params) {
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

        public Object getParam() {
            return convertParam(type, value, fields);
        }

        public static Object convertParam(String type, String value, Map<String, String> fields) {
            switch (type) {
                case "Address":
                    return Converter.toAddress(value);
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
                    throw new IllegalArgumentException("invalid value");
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
                    throw new IllegalArgumentException("invalid value");
                }
                case "struct": {
                    var v = Json.parse(value).asObject();
                    var map = new HashMap<String, Object>();
                    for (String key : fields.keySet()) {
                        var t = fields.get(key);
                        map.put(key, convertParam(t, v.get(key).asString(), fields));
                    }
                    return map;
                }
                case "[]struct": {
                    var v = Json.parse(value).asArray();
                    var list = new ArrayList<Map<String, Object>>();
                    for (int i = 0; i < v.size(); i++) {
                        var map = new HashMap<String, Object>();
                        for (String key : fields.keySet()) {
                            var t = fields.get(key);
                            map.put(key, convertParam(t, v.get(i).asObject().get(key).asString(), fields));
                        }
                        list.add(map);
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
                        } else {
                            throw new IllegalArgumentException("invalid value");
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
    }

    public static Request fromJson(JsonObject object) {
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
            var needFields = type.equals("struct") || type.equals("[]struct");
            Map<String, String> fieldsMap = new HashMap<>();
            if (needFields) {
                Context.require(fields != null, type + " type must have fields");
                for (String key : fields.names()) {
                    fieldsMap.put(key, fields.get(key).asString());
                }
            } else {
                Context.require(fields == null, type + " type must have no fields");
            }
            pArray[i] = new Param(type, stringValue, fieldsMap);
        }
        return new Request(to, method, pArray);
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

    public Object[] getParams() {
        Object[] pArray = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            pArray[i] = params[i].getParam();
        }
        return pArray;
    }

    public void call() {
        var ps = getParams();
        Context.call(to, method, ps);
    }
}
