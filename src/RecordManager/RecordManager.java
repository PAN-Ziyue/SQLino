package RecordManager;

import BufferManager.BufferManager;
import Utils.*;

import java.io.*;

public class RecordManager {



    public static void CreateTable(String table_name) throws SQLException {
        String file_path = DefaultSetting.TABLE_DIR + "/" + table_name + ".table";
        File table_file = new File(file_path);
        try {
            if (table_file.createNewFile()) {
                Block table_block = BufferManager.ReadBlock(file_path, 0);
                table_block.WriteInt();
            }
        } catch (IOException e) {
            throw new SQLException(EType.RuntimeError, 3,
                    "failed to create table, cannot create table file named " + table_name);
        }
    }



}
