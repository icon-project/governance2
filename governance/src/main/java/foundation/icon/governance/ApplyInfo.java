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

import score.Address;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;
import java.util.Map;

public class ApplyInfo {
    private final byte[] id;
    private final Address address;
    private final String name;
    private final BigInteger timestamp;

    public ApplyInfo(byte[] id, Address address, String name, BigInteger timestamp) {
        this.id = id;
        this.address = address;
        this.name = name;
        this.timestamp = timestamp;
    }

    public static void writeObject(ObjectWriter w, ApplyInfo a) {
        w.beginList(4);
        w.write(a.id);
        w.write(a.address);
        w.write(a.name);
        w.write(a.timestamp);
        w.end();
    }

    public static ApplyInfo readObject(ObjectReader r) {
        r.beginList();
        var e = new ApplyInfo(
                r.readByteArray(),
                r.readAddress(),
                r.readString(),
                r.readBigInteger()
        );
        r.end();
        return e;
    }

    public Map<String, Object> toMap() {
        if(id == null) return null;
        return Map.of(
          "id", id,
          "address", address,
          "name", name,
          "timestamp", timestamp
        );
    }
}
