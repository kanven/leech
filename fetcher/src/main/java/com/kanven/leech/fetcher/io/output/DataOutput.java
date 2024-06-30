package com.kanven.leech.fetcher.io.output;

import com.kanven.leech.fetcher.io.BytesRef;

/**
 * 采用小端机制
 */
public abstract class DataOutput {

    public abstract byte[] toArray();

    public abstract void reset();

    public abstract long size();

    public abstract void writeByte(byte b);

    public abstract void writeBytes(byte[] b, int offset, int len);

    public void writeBytes(byte[] b, int len) {
        writeBytes(b, 0, len);
    }

    public void writeShort(short i) {
        writeByte((byte) i);
        writeByte((byte) (i >> 8));
    }

    public void writeInt(int i) {
        writeByte((byte) i);
        writeByte((byte) (i >> 8));
        writeByte((byte) (i >> 16));
        writeByte((byte) (i >> 24));
    }

    public void writeLong(long i) {
        writeInt((int) i);
        writeInt((int) (i >> 32));
    }

    public void writeString(String s) {
        final BytesRef utf8Result = new BytesRef(s);
        writeVInt(utf8Result.length);
        writeBytes(utf8Result.bytes, utf8Result.offset, utf8Result.length);
    }

    /**
     * 可变长Int<br>
     * 压缩格式如下：
     * 第8位位标志位，表示Int高位是否有值，1表示有，0表示没有
     * 第7~0位存存Int 7位值
     * 存在问题：小负数(譬如-1，二进制表示位10000000 00000000 00000000 00000001)压缩比不高，反而占用了更多空间
     *
     * @param i
     */
    public void writeVInt(int i) {
        while ((i & ~0x7F) != 0) {
            //高位有值,去低7位并低8位标志位设置为1
            int b = i & ~0x7F | 0x80;
            writeByte((byte) b);
            //整数后移7位
            i >>>= 7;
        }
        //写入最后一个byte
        writeByte((byte) i);
    }

    public final void writeVLong(long i) {
        if (i < 0) {
            throw new IllegalArgumentException("cannot write negative vLong (got: " + i + ")");
        }
        writeSignedVLong(i);
    }

    private void writeSignedVLong(long i) {
        while ((i & ~0x7FL) != 0L) {
            writeByte((byte) ((i & 0x7FL) | 0x80L));
            i >>>= 7;
        }
        writeByte((byte) i);
    }


    /**
     * ======== 负数有效压缩算法 ========
     **/

    public final void writeZInt(int i) {
        writeVInt(zigZagEncode(i));
    }

    public final void writeZLong(long l) {
        writeVLong(zigZagEncode(l));
    }

    /**
     * Same as {@link #zigZagEncode(long)} but on integers.
     */
    public static int zigZagEncode(int i) {
        return (i >> 31) ^ (i << 1);
    }

    /**
     * <a href="https://developers.google.com/protocol-buffers/docs/encoding#types">Zig-zag</a> encode
     * the provided long. Assuming the input is a signed long whose absolute value can be stored on
     * <code>n</code> bits, the returned value will be an unsigned long that can be stored on <code>
     * n+1</code> bits.
     */
    public static long zigZagEncode(long l) {
        return (l >> 63) ^ (l << 1);
    }

}
