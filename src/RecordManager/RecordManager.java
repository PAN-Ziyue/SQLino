package RecordManager;

import BufferManager.BufferManager;
import Utils.*;

import java.io.*;

import BufferManager.Block;


public class RecordManager {

    public static void CreateTable(String table_name) throws SQLException {
        String file_path = DefaultSetting.TABLE_DIR + "/" + table_name + ".table";
        File table_file = new File(file_path);
        try {
            if (table_file.createNewFile()) {
                Block table_block = BufferManager.ReadBlock(table_name, 0);
                table_block.WriteInt(-1, 0);
            }
        } catch (Exception e) {
            throw new SQLException(EType.RuntimeError, 3,
                    "failed to create table, cannot create table file named " + table_name);
        }
    }

    public static void DropTable(String table_name) {
        String file_path = DefaultSetting.TABLE_DIR + "/" + table_name + ".table";
        File file = new File(file_path);
        if (file.delete()) {
            BufferManager.SetInvalid(table_name);
        }
    }
}
