package com.kanven.leech.fetcher.io.input;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class DataInput {

    public abstract long length();

    public abstract long position();

    public abstract void seek(long position) throws EOFException;

    public abstract byte readByte();

    public abstract void readBytes(byte[] b, int offset, int len) throws EOFException;

    public short readShort() {
        final byte b1 = readByte();
        final byte b2 = readByte();
        return (short) (((b2 & 0xFF) << 8) | (b1 & 0xFF));
    }

    public int readInt() {
        final byte b1 = readByte();
        final byte b2 = readByte();
        final byte b3 = readByte();
        final byte b4 = readByte();
        return (int) ((b4 & 0xFF) << 24) | ((b3 & 0xFF) << 16) | ((b2 & 0xFF) << 8) | (b1 & 0xFF);
    }

    public long readLong() {
        return (readInt() & 0xFFFFFFFFL) | (((long) readInt()) << 32);
    }

    public int readVInt() throws IOException {
        byte b = readByte();
        if (b > 0) {
            return (int) b;
        }
        int i = b & 0x7F;
        b = readByte();
        i |= (b & 0x7F << 7);
        if (b > 0) {
            return i;
        }
        b = readByte();
        i |= (b & 0x7F << 14);
        if (b > 0) {
            return i;
        }
        b = readByte();
        i |= ((b & 0x7F) << 21);
        if (b > 0) {
            return i;
        }
        //INT最后4位
        b = readByte();
        //低4位
        i |= ((b & 0x0F) << 28);
        //高4位
        if ((b & 0xF0) == 0) {
            return i;
        }
        throw new IOException("Invalid vInt detected (too many bits)");
    }

    public long readVLong() throws IOException {
        return readVLong(false);
    }

    private long readVLong(boolean allowNegative) throws IOException {
        byte b = readByte();
        if (b >= 0) return b;
        long i = b & 0x7FL;
        b = readByte();
        i |= (b & 0x7FL) << 7;
        if (b >= 0) return i;
        b = readByte();
        i |= (b & 0x7FL) << 14;
        if (b >= 0) return i;
        b = readByte();
        i |= (b & 0x7FL) << 21;
        if (b >= 0) return i;
        b = readByte();
        i |= (b & 0x7FL) << 28;
        if (b >= 0) return i;
        b = readByte();
        i |= (b & 0x7FL) << 35;
        if (b >= 0) return i;
        b = readByte();
        i |= (b & 0x7FL) << 42;
        if (b >= 0) return i;
        b = readByte();
        i |= (b & 0x7FL) << 49;
        if (b >= 0) return i;
        b = readByte();
        i |= (b & 0x7FL) << 56;
        if (b >= 0) return i;
        if (allowNegative) {
            b = readByte();
            i |= (b & 0x7FL) << 63;
            if (b == 0 || b == 1) return i;
            throw new IOException("Invalid vLong detected (more than 64 bits)");
        } else {
            throw new IOException("Invalid vLong detected (negative values disallowed)");
        }
    }

    public int readZInt() throws IOException {
        return zigZagDecode(readVInt());
    }

    public long readZLong() throws IOException {
        return zigZagDecode(readVLong(true));
    }

    public static int zigZagDecode(int i) {
        return ((i >>> 1) ^ -(i & 1));
    }

    public static long zigZagDecode(long l) {
        return ((l >>> 1) ^ -(l & 1));
    }


    /**
     * Reads a string.
     */
    public String readString() throws IOException {
        int length = readVInt();
        final byte[] bytes = new byte[length];
        readBytes(bytes, 0, length);
        return new String(bytes, 0, length, StandardCharsets.UTF_8);
    }

}
