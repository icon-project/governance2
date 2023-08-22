/*
 * Copyright 2022 ICON Foundation
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
    private final Address to;
    private final String method;
    private final Param[] params;

    public Request(Address to, String method, Param[] params) {
        this.to = to;
        this.method = method;
        this.params = params;
    }

    public static class Param {
        private final String type;
        private final String value;
        private final Map<String, String> fields;

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
                    return Converter.toBoolean(value);
                }
                case "bytes": {
                    return Converter.hexToBytes(value);
                }
                case "struct": {
                    var v = Json.parse(value).asObject();
                    return convertToHashMap(v, fields);
                }
                case "[]struct": {
                    var array = Json.parse(value).asArray();
                    var list = new ArrayList<Map<String, Object>>();
                    for (var item : array) {
                        var v = item.asObject();
                        list.add(convertToHashMap(v, fields));
                    }
                    return list;
                }
                case "[]Address": {
                    var array = Json.parse(value).asArray();
                    var list = new ArrayList<Address>();
                    for (var v : array) {
                        list.add(Address.fromString(v.asString()));
                    }
                    return list;
                }
                case "[]int": {
                    var array = Json.parse(value).asArray();
                    var list = new ArrayList<BigInteger>();
                    for (var v : array) {
                        list.add(Converter.toInteger(v.asString()));
                    }
                    return list;
                }
                case "[]bool": {
                    var array = Json.parse(value).asArray();
                    var list = new ArrayList<Boolean>();
                    for (var v : array) {
                        list.add(Converter.toBoolean(v.asString()));
                    }
                    return list;
                }
                case "[]str": {
                    var array = Json.parse(value).asArray();
                    var list = new ArrayList<String>();
                    for (var v : array) {
                        list.add(v.asString());
                    }
                    return list;
                }
                case "[]bytes": {
                    var array = Json.parse(value).asArray();
                    var list = new ArrayList<byte[]>();
                    for (var v : array) {
                        list.add(Converter.hexToBytes(v.asString()));
                    }
                    return list;
                }
            }
            throw new IllegalArgumentException("unknown param type");
        }

        private static HashMap<String, Object> convertToHashMap(JsonObject v, Map<String, String> fields) {
            var map = new HashMap<String, Object>();
            for (String key : fields.keySet()) {
                var type = fields.get(key);
                map.put(key, convertParam(type, v.get(key).asString(), fields));
            }
            return map;
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
