package com.kanven.leech.fetcher.io;

import java.util.Arrays;

public final class BytesRef {

    /**
     * An empty byte array for convenience
     */
    public static final byte[] EMPTY_BYTES = new byte[0];

    /**
     * The contents of the BytesRef. Should never be {@code null}.
     */
    public byte[] bytes;

    /**
     * Offset of first valid byte.
     */
    public int offset;

    /**
     * Length of used bytes.
     */
    public int length;

    /**
     * Create a BytesRef with {@link #EMPTY_BYTES}
     */
    public BytesRef() {
        this(EMPTY_BYTES);
    }

    /**
     * This instance will directly reference bytes w/o making a copy. bytes should not be null.
     */
    public BytesRef(byte[] bytes, int offset, int length) {
        this.bytes = bytes;
        this.offset = offset;
        this.length = length;
        assert isValid();
    }

    /**
     * This instance will directly reference bytes w/o making a copy. bytes should not be null
     */
    public BytesRef(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    /**
     * Create a BytesRef pointing to a new array of size <code>capacity</code>. Offset and length will
     * both be zero.
     */
    public BytesRef(int capacity) {
        this.bytes = new byte[capacity];
    }

    /**
     * Initialize the byte[] from the UTF8 bytes for the provided String.
     *
     * @param text This must be well-formed unicode text, with no unpaired surrogates.
     */
    public BytesRef(CharSequence text) {
        this(new byte[UnicodeUtil.maxUTF8Length(text.length())]);
        length = UnicodeUtil.UTF16toUTF8(text, 0, text.length(), bytes);
    }

    /**
     * Expert: compares the bytes against another BytesRef, returning true if the bytes are equal.
     *
     * @param other Another BytesRef, should not be null.
     * @lucene.internal
     */
    public boolean bytesEquals(BytesRef other) {
        return Arrays.equals(this.bytes, other.bytes) && this.offset == other.offset && this.offset + this.length == other.offset + other.length;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof BytesRef) {
            return this.bytesEquals((BytesRef) other);
        }
        return false;
    }

    /**
     * Interprets stored bytes as UTF8 bytes, returning the resulting string
     */
    public String utf8ToString() {
        final char[] ref = new char[length];
        final int len = UnicodeUtil.UTF8toUTF16(bytes, offset, length, ref);
        return new String(ref, 0, len);
    }

    /**
     * Returns hex encoded bytes, eg [0x6c 0x75 0x63 0x65 0x6e 0x65]
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        final int end = offset + length;
        for (int i = offset; i < end; i++) {
            if (i > offset) {
                sb.append(' ');
            }
            sb.append(Integer.toHexString(bytes[i] & 0xff));
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Performs internal consistency checks. Always returns true (or throws IllegalStateException)
     */
    public boolean isValid() {
        if (bytes == null) {
            throw new IllegalStateException("bytes is null");
        }
        if (length < 0) {
            throw new IllegalStateException("length is negative: " + length);
        }
        if (length > bytes.length) {
            throw new IllegalStateException(
                    "length is out of bounds: " + length + ",bytes.length=" + bytes.length);
        }
        if (offset < 0) {
            throw new IllegalStateException("offset is negative: " + offset);
        }
        if (offset > bytes.length) {
            throw new IllegalStateException(
                    "offset out of bounds: " + offset + ",bytes.length=" + bytes.length);
        }
        if (offset + length < 0) {
            throw new IllegalStateException(
                    "offset+length is negative: offset=" + offset + ",length=" + length);
        }
        if (offset + length > bytes.length) {
            throw new IllegalStateException(
                    "offset+length out of bounds: offset="
                            + offset
                            + ",length="
                            + length
                            + ",bytes.length="
                            + bytes.length);
        }
        return true;
    }

}
