package com.icon.governance;

import score.Address;

class Convert {
    public static byte[] hexToBytes(String value) {
        if (value.startsWith("0x") && (value.length() % 2 == 0)) {
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

    public static int hexToInt(String value) {
        if(value.startsWith("0x")) {
            return Integer.parseInt(value.substring(2), 16);
        } else {
            throw new IllegalArgumentException("Invalid hex value");
        }
    }

    public static Address strToAddress(String value) {
        return Address.fromString(value);
    }
}
