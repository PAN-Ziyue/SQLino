package BufferManager;

import Data.Tuple;
import Utils.*;

import java.io.File;
import java.io.RandomAccessFile;
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
                WriteBlockToDisk(i);
    }

    public static void Drop(String table_name) throws SQLException {
        String file_path = DefaultSetting.TABLE_DIR + "/" + table_name + ".table";
        try {
            File file = new File(file_path);
            if (file.delete())
                SetInvalid(table_name);
        } catch (Exception e) {
            throw new SQLException(EType.RuntimeError, 38, "cannot delete file");
        }
    }

    public static void SetInvalid(String table_name) {
        for (int i = 0; i < DefaultSetting.BLOCK_NUM; i++)
            if (buffer[i].table_name.equals(table_name))
                buffer[i].is_valid = false;
    }

    public static Block ReadBlock(String table_name, int block_offset) {
        for (int i = 0; i < DefaultSetting.BLOCK_NUM; i++) { // find block in buffer
            if (buffer[i].is_valid && buffer[i].table_name.equals(table_name)
                    && buffer[i].block_offset == block_offset)
                return buffer[i];
        }
        String file_path = DefaultSetting.TABLE_DIR + "/" + table_name + ".table";
        File file = new File(file_path);
        int free_block_id = GetFreeBlockID();
        if (free_block_id == -1 || !file.exists()) return null;
        if (!ReadBlockFromDisk(table_name, block_offset, free_block_id)) return null;
        return buffer[free_block_id];
    }

    public static boolean ReadBlockFromDisk(String table_name, int block_offset, int block_id) {
        byte[] tmp_data = new byte[DefaultSetting.BLOCK_SIZE];
        String file_path = DefaultSetting.TABLE_DIR + "/" + table_name + ".table";
        RandomAccessFile raf;
        try {
            File in = new File(file_path);
            raf = new RandomAccessFile(in, "rw");
            if ((block_offset + 1) * DefaultSetting.BLOCK_SIZE <= raf.length()) {
                raf.seek(block_offset * DefaultSetting.BLOCK_SIZE);
                raf.read(tmp_data, 0, DefaultSetting.BLOCK_SIZE);
            } else {
                Arrays.fill(tmp_data, (byte) 0);
            }
            raf.close();
            buffer[block_id].Reset();
            buffer[block_id].SetBlockData(tmp_data);
            buffer[block_id].is_valid = true;
            buffer[block_id].table_name = table_name;
            buffer[block_id].block_offset = block_offset;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void WriteBlockToDisk(int block_id) throws SQLException {
        if (!buffer[block_id].is_dirty) {
            buffer[block_id].is_valid = false;
            return;
        }
        String file_path = DefaultSetting.TABLE_DIR + "/" + buffer[block_id].table_name + ".table";
        RandomAccessFile raf = null;
        try {
            File out = new File(file_path);
            raf = new RandomAccessFile(out, "rw");
            if (!out.exists()) out.createNewFile();
            raf.seek(buffer[block_id].block_offset * DefaultSetting.BLOCK_SIZE);
            raf.write(buffer[block_id].GetBlockData());
            raf.close();
        } catch (Exception e) {
            throw new SQLException(EType.RuntimeError, 39, "fail to save");
        }
    }

    public static int GetFreeBlockID() {
        int free_block_id = -1, min = Integer.MAX_VALUE;
        for (int i = 0; i < DefaultSetting.BLOCK_NUM; i++)
            if (!buffer[i].is_pinned && buffer[i].LRU_count < min) {
                free_block_id = i;
                min = buffer[i].LRU_count;
            }
        return free_block_id;
    }

}
