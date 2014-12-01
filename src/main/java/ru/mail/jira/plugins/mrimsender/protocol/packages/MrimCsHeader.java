package ru.mail.jira.plugins.mrimsender.protocol.packages;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
class MrimCsHeader {
    public static final int SIZE = 44;

    private final int magic;      // Magic( 0xDEADBEEF for _CS_ packets )
    private final int proto;      // pversion(1, 21) for example
    private final int seq;        // Sequence of packet is used to wait for acknolegement in several cases
    private final int msg;        // identifier of a packet
    private final int dlen;       // data length of this packet
    private final int from;       // not user, must be zero
    private final int fromport;   // not user, must be zero
    private final int reserved1;  // not user, must be filled with zeroes
    private final int reserved2;  // not user, must be filled with zeroes
    private final int reserved3;  // not user, must be filled with zeroes
    private final int reserved4;  // not user, must be filled with zeroes

    protected int convertULToInt(byte[] buff, int offset) {
        final int SIZE = Integer.SIZE / Byte.SIZE;
        ByteBuffer bb = ByteBuffer.allocate(SIZE);
        bb.put(buff, offset, SIZE);
        bb.order(ByteOrder.LITTLE_ENDIAN); // Java uses Big Endian, but Network program uses Little Endian
        bb.rewind();
        return bb.getInt();
    }

    protected String convertBytesToString(byte[] buff, int offset, int size, String encoding) {
        try {
            return new String(buff, offset, size, encoding);
        } catch (UnsupportedEncodingException ignored) {
            return null;
        }
    }

    public MrimCsHeader(byte[] header) {
        this.magic = convertULToInt(header, 0);
        this.proto = convertULToInt(header, 4);
        this.seq = convertULToInt(header, 8);
        this.msg = convertULToInt(header, 12);
        this.dlen = convertULToInt(header, 16);
        this.from = convertULToInt(header, 20);
        this.fromport = convertULToInt(header, 24);
        this.reserved1 = convertULToInt(header, 28);
        this.reserved2 = convertULToInt(header, 32);
        this.reserved3 = convertULToInt(header, 36);
        this.reserved4 = convertULToInt(header, 40);
    }

    public int getMessageType() {
        return msg;
    }

    public int getDataLength() {
        return dlen;
    }
}
