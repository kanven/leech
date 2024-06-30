package com.kanven.leech.fetcher.io.input;

import com.kanven.leech.config.SystemInformation;

import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class ByteBuffersDataInput extends DataInput {

    private final ByteBuffer[] blocks;

    /**
     * 页大小
     */
    private static final int blockSize;

    /**
     * 总长度
     */
    private long length;

    /**
     * 当前位置(全局视角)
     */
    private long pos;

    /**
     * 字节流起始位置
     */
    private long offset;

    static {
        blockSize = (int) SystemInformation.memoryPageSize();
    }

    public ByteBuffersDataInput(List<ByteBuffer> buffers) {

        this.blocks = buffers.toArray(new ByteBuffer[]{});
        for (int i = 0; i < blocks.length; ++i) {
            blocks[i] = blocks[i].asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        }
        long length = 0;
        for (ByteBuffer block : blocks) {
            length += block.remaining();
        }
        this.length = length;
        // The initial "position" of this stream is shifted by the position of the first block.
        this.offset = blocks[0].position();
        this.pos = offset;
    }

    @Override
    public long length() {
        return this.length;
    }

    @Override
    public long position() {
        return pos - offset;
    }

    @Override
    public void seek(long position) throws EOFException {
        this.pos = position + offset;
        if (position > length()) {
            this.pos = length();
            throw new EOFException();
        }
    }

    public ByteBuffersDataInput slice(long offset, long length) {
        if (offset < 0 || length < 0 || offset + length > this.length) {
            throw new IllegalArgumentException(
                    String.format(
                            Locale.ROOT,
                            "slice(offset=%s, length=%s) is out of bounds: %s",
                            offset,
                            length,
                            this));
        }

        return new ByteBuffersDataInput(sliceBufferList(Arrays.asList(this.blocks), offset, length));
    }

    private static List<ByteBuffer> sliceBufferList(
            List<ByteBuffer> buffers, long offset, long length) {
        ensureAssumptions(buffers);

        if (buffers.size() == 1) {
            ByteBuffer cloned = buffers.get(0).asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
            cloned.position(Math.toIntExact(cloned.position() + offset));
            cloned.limit(Math.toIntExact(cloned.position() + length));
            return Arrays.asList(cloned);
        } else {
            long absStart = buffers.get(0).position() + offset;
            long absEnd = absStart + length;

            int blockBits = Integer.numberOfTrailingZeros(blockSize);
            long blockMask = (1L << blockBits) - 1;

            int endOffset = Math.toIntExact(absEnd & blockMask);

            ArrayList<ByteBuffer> cloned =
                    buffers
                            .subList(
                                    Math.toIntExact(absStart / blockSize),
                                    Math.toIntExact(absEnd / blockSize + (endOffset == 0 ? 0 : 1)))
                            .stream()
                            .map(buf -> buf.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN))
                            .collect(Collectors.toCollection(ArrayList::new));

            if (endOffset == 0) {
                cloned.add(ByteBuffer.allocate(0).order(ByteOrder.LITTLE_ENDIAN));
            }

            cloned.get(0).position(Math.toIntExact(absStart & blockMask));
            cloned.get(cloned.size() - 1).limit(endOffset);
            return cloned;
        }
    }

    private static void ensureAssumptions(List<ByteBuffer> buffers) {
        if (buffers.isEmpty()) {
            throw new IllegalArgumentException("Buffer list must not be empty.");
        }

        if (buffers.size() == 1) {
            // Special case of just a single buffer, conditions don't apply.
        } else {
            //final int blockPage = determineBlockPage(buffers);

            // First buffer decides on block page length.
            if (!isPowerOfTwo(blockSize)) {
                throw new IllegalArgumentException(
                        "The first buffer must have power-of-two position() + remaining(): 0x"
                                + Integer.toHexString(blockSize));
            }

            // Any block from 2..last-1 should have the same page size.
            for (int i = 1, last = buffers.size() - 1; i < last; i++) {
                ByteBuffer buffer = buffers.get(i);
                if (buffer.position() != 0) {
                    throw new IllegalArgumentException(
                            "All buffers except for the first one must have position() == 0: " + buffer);
                }
                if (i != last && buffer.remaining() != blockSize) {
                    throw new IllegalArgumentException(
                            "Intermediate buffers must share an identical remaining() power-of-two block size: 0x"
                                    + Integer.toHexString(blockSize));
                }
            }
        }
    }

    private static final boolean isPowerOfTwo(int v) {
        return (v & (v - 1)) == 0;
    }

    @Override
    public short readShort() {
        int blockOffset = indexPosition(pos);
        if (blockOffset + Byte.BYTES < blockSize) {
            ByteBuffer block = blocks[indexBlock(pos)];
            short v = block.getShort();
            pos += Short.BYTES;
            return v;
        } else {
            return super.readShort();
        }
    }

    @Override
    public int readInt() {
        int blockOffset = indexPosition(pos);
        if (blockOffset + Integer.BYTES < blockSize) {
            ByteBuffer block = blocks[indexBlock(pos)];
            int v = block.getInt();
            pos += Integer.BYTES;
            return v;
        } else {
            return super.readInt();
        }
    }

    public byte[] toArray() {
        byte[] bytes = new byte[(int) (length - pos)];
        List<ByteBuffer> buffers = Arrays.asList(this.blocks);
        ArrayList<ByteBuffer> cloned =
                buffers.stream()
                        .map(buf -> buf.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN))
                        .collect(Collectors.toCollection(ArrayList::new));
        int offset = 0;
        for (ByteBuffer buffer : cloned) {
            int delta = buffer.limit() - buffer.position();
            buffer.get(bytes, offset, delta);
            offset += delta;
        }
        return bytes;
    }

    @Override
    public long readLong() {
        int blockOffset = indexPosition(pos);
        if (blockOffset + Long.BYTES < blockSize) {
            ByteBuffer block = blocks[indexBlock(pos)];
            long v = block.getLong();
            pos += Long.BYTES;
            return v;
        } else {
            return super.readLong();
        }
    }

    @Override
    public byte readByte() {
        //定位block
        ByteBuffer block = blocks[indexBlock(pos)];
        //定位所在block position
        byte b = block.get(indexPosition(pos));
        pos++;
        return b;
    }

    @Override
    public void readBytes(byte[] b, int offset, int len) throws EOFException {
        while (len > 0) {
            ByteBuffer block = blocks[indexBlock(pos)].duplicate();
            block.position(indexPosition(pos));
            int chunk = Math.min(len, block.remaining());
            if (chunk == 0) {
                throw new EOFException();
            }
            // Update pos early on for EOF detection, then try to get buffer content.
            pos += chunk;
            block.get(b, offset, chunk);
            len -= chunk;
            offset += chunk;
        }
    }

    private int indexBlock(long pos) {
        return (int) (pos / this.blockSize);
    }

    private int indexPosition(long pos) {
        return (int) (pos % this.blockSize);
    }

}
