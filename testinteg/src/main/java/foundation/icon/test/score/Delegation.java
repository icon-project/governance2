package foundation.icon.test.score;

import foundation.icon.icx.data.Address;

import java.math.BigInteger;

public class Delegation {
    public Address address;
    public BigInteger value;
    public Delegation(Address address, BigInteger value) {
        this.address = address;
        this.value = value;
    }
}
