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

import java.math.BigInteger;

public class Converter {
    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_ARRAY[v >>> 4];
            hexChars[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexToBytes(String value) {
        if (value != null && value.startsWith("0x") && (value.length() % 2 == 0)) {
            String hex = value.substring(2);
            int len = hex.length() / 2;
            byte[] bytes = new byte[len];
            for (int i = 0; i < len; i++) {
                int j = i * 2;
                bytes[i] = (byte) Integer.parseInt(hex.substring(j, j + 2), 16);
            }
            return bytes;
        } else {
            throw new IllegalArgumentException("Invalid hex value");
        }
    }

    public static BigInteger toInteger(String value) {
        if (value.startsWith("0x")) {
            return new BigInteger(value.substring(2), 16);
        } else if (value.startsWith(("-0x"))){
            return new BigInteger("-" + value.substring(3), 16);
        }
        return new BigInteger(value);
    }

    public static Address toAddress(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return Address.fromString(value);
    }

    public static Boolean toBoolean(String value) {
        if (value.equals("0x0") || value.equals("false")) {
            return Boolean.FALSE;
        } else if (value.equals("0x1") || value.equals("true")) {
            return Boolean.TRUE;
        }
        throw new IllegalArgumentException("invalid bool value");
    }
}
