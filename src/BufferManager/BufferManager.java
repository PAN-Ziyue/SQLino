package BufferManager;

import Utils.DefaultSetting;
import Utils.EType;
import Utils.SQLException;

import java.io.*;
import java.util.Arrays;

public class BufferManager {

    public static Block[] buffer = new Block[DefaultSetting.BLOCK_NUM];

    public static void Initialize() {
        for (int i = 0; i < DefaultSetting.BLOCK_NUM; i++)
            buffer[i] = new Block();
    }

    public static void Store() throws SQLException {
        for (int i = 0; i < DefaultSetting.BLOCK_NUM; i++)
            if (buffer[i].is_valid)
                WriteBlock(i);
    }

    public static void SetInvalid(String file_name) {
        for (int i = 0; i < DefaultSetting.BLOCK_NUM; i++)
            if (!buffer[i].file_name.equals("") && buffer[i].file_name.equals(file_name))
                buffer[i].is_valid = false;
    }

    public static int ReadBlockID(String file_name, int offset) throws SQLException {
        for (int i = 0; i < DefaultSetting.BLOCK_NUM; i++)
            if (buffer[i].is_valid && buffer[i].file_name.equals(file_name)
                    && buffer[i].block_offset == offset)
                return i;
        File file = new File(file_name);
        int bid = GetFreeBlockID();
        if (bid == -1)
            return bid;
        try {
            if (!file.exists())
                file.createNewFile();
            if (!ReadBlockSucceed(file_name, offset, bid))
                return -1;
        } catch (Exception e) {
            return -1;
        }
        return bid;
    }

    public static Block ReadBlock(String file_name, int offset) throws SQLException {
        int i;
        for (i = 0; i < DefaultSetting.BLOCK_NUM; i++)
            if (buffer[i].is_valid && buffer[i].file_name.equals(file_name)
                    && buffer[i].block_offset == offset)
                break;

        if (i < DefaultSetting.BLOCK_NUM)
            return buffer[i];
        else {
            File file = new File(file_name);
            int bid = GetFreeBlockID();
            if (bid == -1 || !file.exists())
                return null;
            if (!ReadBlockSucceed(file_name, offset, bid))
                return null;
            return buffer[bid];
        }
    }

    public static boolean ReadBlockSucceed(String file_name, int offset, int block_id) throws SQLException {
        boolean flag = false;
        byte[] data = new byte[DefaultSetting.BLOCK_NUM];
        RandomAccessFile raf = null;
        try {
            File in = new File(file_name);
            raf = new RandomAccessFile(in, "rw");
            if((offset + 1) * DefaultSetting.BLOCK_SIZE <= raf.length()) {
                raf.seek(offset * DefaultSetting.BLOCK_SIZE);
                raf.read(data, 0, DefaultSetting.BLOCK_SIZE);
            } else {
                Arrays.fill(data, (byte)0);
            }
            flag = true;
        } catch (Exception e) {
            throw new SQLException(EType.RuntimeError, 3,
                    "failed to create table");
        } finally {
            try {
                if (raf != null) raf.close();
            } catch (Exception e) {
                throw new SQLException(EType.RuntimeError, 3,
                        "failed to create table");
            }
        }
        if(flag) {
            buffer[block_id].Reset();
            buffer[block_id].SetBlockData(data);
            buffer[block_id].file_name = file_name;
            buffer[block_id].block_offset = offset;
            buffer[block_id].is_valid = true;
        }
        return flag;
    }


    public static void WriteBlock(int block_id) throws SQLException {
        if (buffer[block_id].is_dirty) {
            RandomAccessFile raf = null;
            try {
                File out = new File(buffer[block_id].file_name);
                raf = new RandomAccessFile(out, "rw");
                if (!out.exists())
                    out.createNewFile();
                raf.seek(buffer[block_id].block_offset * DefaultSetting.BLOCK_SIZE);
                raf.write(buffer[block_id].GetBlockData());
            } catch (Exception e) {
                throw new SQLException(EType.RuntimeError, 3,
                        "failed to create table");
            } finally {
                try {
                    if (raf != null) raf.close();
                } catch (Exception e) {
                    throw new SQLException(EType.RuntimeError, 3,
                            "failed to create table");
                }
            }
        }
        buffer[block_id].is_valid = false;
    }

    public static int GetFreeBlockID() throws SQLException {
        int index = -1, min = Integer.MAX_VALUE;
        for (int i = 0; i < DefaultSetting.BLOCK_NUM; i++) {
            if (!buffer[i].is_pinned && buffer[i].GetLRU() < min) {
                index = i;
                min = buffer[i].GetLRU();
            }
        }
        if (index != -1 && buffer[index].is_dirty)
            WriteBlock(index);
        return index;
    }
}
