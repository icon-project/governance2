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
import com.eclipsesource.json.JsonValue;
import score.Address;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;
import scorex.util.HashMap;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class Param {
    private String type;
    private String value;
    private Field[] fields;

    public void setType(String type) {
        this.type = type;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setFields(Field[] fields) {
        this.fields = fields;
    }

    public static class Field {
        private String name;
        private String type;

        public void setName(String name) {
            this.name = name;
        }

        public void setType(String type) {
            this.type = type;
        }

        public static Field readObject(ObjectReader r) {
            r.beginList();
            var name = r.readString();
            var type = r.readString();
            r.end();
            var f = new Field();
            f.setName(name);
            f.setType(type);
            return f;
        }

        public static void writeObject(ObjectWriter w, Field f) {
            w.beginList(2);
            w.write(f.name);
            w.write(f.type);
            w.end();
        }

        public Map<String, Object> toMap() {
            return Map.of(name, type);
        }
    }

    public static Param readObject(ObjectReader r) {
        r.beginList();
        var type = r.readString();
        var value = r.readString();
        ArrayList<Field> fields = new ArrayList<>();
        r.beginList();
        while (r.hasNext()) {
            fields.add(r.read(Field.class));
        }
        r.end();
        Field[] fieldArray = new Field[fields.size()];
        for (int i = 0; i < fields.size(); i++) fieldArray[i] = fields.get(i);
        r.end();
        var p = new Param();
        p.setFields(fieldArray);
        p.setType(type);
        p.setValue(value);
        return p;
    }

    public static void writeObject(ObjectWriter w, Param p) {
        w.beginList(3);
        w.write(p.type);
        w.write(p.value);
        w.beginList(p.fields.length);
        for (Field f : p.fields) {
            w.write(f);
        }
        w.end();
        w.end();
    }

    public Object getParam() {
        return convertParams(type, value, fields);
    }

    public static Object convertParams(String type, String value, Field[] fields) {
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
                for (Field f : fields) {
                    map.put(f.name, convertParams(f.type, v.get(f.name).asString(), fields));
                }
                return map;
            }
            case "[]struct": {
                var v = Json.parse(value).asArray();
                var list = new ArrayList<Map<String, Object>>();
                for (int i = 0; i < v.size(); i++) {
                    var element = v.get(i).asObject();
                    for (Field f : fields) {
                        list.add(Map.of(f.name, convertParams(f.type, element.get(f.name).asString(), fields)));
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
        if (fields.length != 0) {
            ArrayList<Map<String, Object>> fieldList = new ArrayList<>();
            for (Field f : fields) fieldList.add(f.toMap());
            return Map.of(
                    "type", type,
                    "value", value,
                    "fields", List.of(fieldList.toArray())
            );
        }
        return Map.of(
                "type", type,
                "value", value
        );
    }
}
