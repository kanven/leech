package com.kanven.leech.fetcher.io.output;

import com.kanven.leech.config.SystemInformation;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class ByteBuffersDataOutput extends DataOutput {

    private static final ByteBuffer EMPTY = ByteBuffer.allocate(0).order(ByteOrder.LITTLE_ENDIAN);

    /**
     * Maximum number of blocks at the current block size before we increase the
     * block size (and thus decrease the number of blocks).
     */
    private static final int MAX_BLOCKS_BEFORE_BLOCK_EXPANSION = 100;

    /**
     * The current-or-next write block.
     */
    private ByteBuffer currentBlock = EMPTY;

    /**
     * Current block size
     */
    private final int blockSize;

    /**
     * Blocks storing data.
     */
    private final ArrayDeque<ByteBuffer> blocks = new ArrayDeque<>();

    public ByteBuffersDataOutput() {
        blockSize = (int) SystemInformation.memoryPageSize();
    }

    @Override
    public void writeByte(byte b) {
        if (!currentBlock.hasRemaining()) {
            appendBlock();
        }
        currentBlock.put(b);
    }

    @Override
    public void writeBytes(byte[] b, int offset, int length) {
        assert length >= 0;
        while (length > 0) {
            if (!currentBlock.hasRemaining()) {
                appendBlock();
            }
            int chunk = Math.min(currentBlock.remaining(), length);
            currentBlock.put(b, offset, chunk);
            length -= chunk;
            offset += chunk;
        }
    }

    @Override
    public void writeShort(short i) {
        if (currentBlock.remaining() >= Short.BYTES) {
            currentBlock.putShort(i);
        } else {
            super.writeShort(i);
        }
    }

    @Override
    public void writeInt(int i) {
        if (currentBlock.remaining() > Integer.BYTES) {
            currentBlock.putInt(i);
        } else {
            super.writeInt(i);
        }
    }

    @Override
    public void writeLong(long i) {
        if (currentBlock.remaining() > Long.BYTES) {
            currentBlock.putLong(i);
        } else {
            super.writeLong(i);
        }
    }

    public void writeDataOutput(ByteBuffersDataOutput output) {
        if (output.blocks.size() > 0) {
            for (ByteBuffer bb : output.toBufferList()) {
                byte[] bytes = new byte[bb.remaining()];
                bb.get(bytes, 0, bb.remaining());
                writeBytes(bytes, 0, bytes.length);
            }
        }
    }

    @Override
    public long size() {
        long size = 0;
        int count = this.blocks.size();
        if (count >= 1) {
            size = (count - 1) * this.blockSize + this.blocks.getLast().position();
        }
        return size;
    }

    @Override
    public void reset() {
        blocks.clear();
        currentBlock = EMPTY;
    }

    @Override
    public byte[] toArray() {
        if (blocks.size() == 0) {
            return new byte[0];
        }
        byte[] bts = new byte[(int) size()];
        int offset = 0;
        for (ByteBuffer buffer : toBufferList()) {
            int len = buffer.remaining();
            buffer.get(bts, offset, len);
            offset += len;
        }
        return bts;
    }

    public List<ByteBuffer> toBufferList() {
        List<ByteBuffer> buffers = new ArrayList<>(0);
        if (blocks.size() > 0) {
            for (ByteBuffer bb : blocks) {
                bb = (ByteBuffer) bb.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN).flip();
                buffers.add(bb);
            }
        }
        return buffers;
    }

    private void appendBlock() {
        if (blocks.size() >= MAX_BLOCKS_BEFORE_BLOCK_EXPANSION) {
            //已经达到最大容量
            throw new IndexOutOfBoundsException();
        }
        currentBlock = ByteBuffer.allocate(blockSize).order(ByteOrder.LITTLE_ENDIAN);
        blocks.add(currentBlock);
    }

}
