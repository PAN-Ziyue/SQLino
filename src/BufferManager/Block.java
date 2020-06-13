package BufferManager;

import Utils.DefaultSetting;

public class Block {
    private int LRU_count = 0;
    private int block_offset = 0;
    private boolean is_dirty = false;
    private boolean is_valid = false;
    private boolean is_pinned = false;
    private String file_name;
    private byte[] block_data = new byte[DefaultSetting.BLOCK_SIZE];


    public void Reset() {
        is_dirty = is_pinned = is_valid = false;
        LRU_count = 0;
    }

    public boolean WriteData(int offset, byte[] data) {
        if ()
    }


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

    public String ReadChar(int offset) {

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
            
        }
    }
}
