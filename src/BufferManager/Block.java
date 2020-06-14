package BufferManager;

import Utils.DefaultSetting;

import java.util.Arrays;

public class Block {
    private int LRU_count;
    public int block_offset;
    public boolean is_dirty;
    public boolean is_valid;
    public boolean is_pinned;
    public String file_name;
    private byte[] block_data = new byte[DefaultSetting.BLOCK_SIZE];

    public Block() {
        file_name = "";
        LRU_count = -1;
        block_offset = -1;
        is_dirty = is_pinned = false;
        is_valid = true;
        Arrays.fill(block_data, (byte) 0);
    }


    //* common utilities
    public void Reset() {
        is_valid = true;
        is_dirty = is_pinned = false;
        LRU_count = block_offset = -1;
    }

    public int GetLRU() {
        return LRU_count;
    }

    public byte[] GetBlockData() {
        return block_data;
    }

    public void SetBlockData(byte[] block_data) {
        this.block_data = block_data;
    }

    public void SetBlockData() {
        Arrays.fill(block_data, (byte) 0);
    }

    //* read & write supported data type
    public int ReadInt(int offset) {
        if (offset + 4 > DefaultSetting.BLOCK_SIZE)
            return 0;
        int byte0 = block_data[offset] & 0xFF;
        int byte1 = block_data[offset + 1] & 0xFF;
        int byte2 = block_data[offset + 2] & 0xFF;
        int byte3 = block_data[offset + 3] & 0xFF;
        LRU_count++;
        return (byte0 << 24) | (byte1 << 16) | (byte2 << 8) | byte3;
    }

    public double ReadFloat(int offset) {
        int int0 = ReadInt(offset);
        int int1 = ReadInt(offset + 4);
        long l = (((long) int0) << 32) | (int1 & 0xFFFFFFFFL);
        return Double.longBitsToDouble(l);
    }

    public String ReadChar(int length, int offset) {
        byte[] char_bytes = new byte[length];
        for (int i = 0; i < length && i < DefaultSetting.BLOCK_SIZE - offset; i++)
            char_bytes[i] = block_data[offset + i];
        LRU_count++;
        return new String(char_bytes);
    }

    public void WriteInt(int value, int offset) {
        if (offset + 4 <= DefaultSetting.BLOCK_SIZE) {
            block_data[offset] = (byte) (value >> 24 & 0xFF);
            block_data[offset + 1] = (byte) (value >> 16 & 0xFF);
            block_data[offset + 2] = (byte) (value >> 8 & 0xFF);
            block_data[offset + 3] = (byte) (value & 0xFF);
            LRU_count++;
            is_dirty = true;
        }
    }

    public void WriteFloat(double value, int offset) {
        long l = Double.doubleToLongBits(value);
        int int0 = (int) (l >> 32);
        int int1 = (int) l;
        WriteInt(int0, offset);
        WriteInt(int1, offset + 4);
    }

    public void WriteChar(String value, int offset) {
        byte[] char_bytes = value.getBytes();
        if (offset + char_bytes.length <= DefaultSetting.BLOCK_SIZE) {
            for (int i = 0; i < char_bytes.length; i++)
                block_data[offset + i] = char_bytes[i];
            LRU_count++;
            is_dirty = true;
        }
    }


}
