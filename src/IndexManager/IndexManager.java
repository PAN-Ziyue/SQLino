package IndexManager;

import BufferManager.BufferManager;
import CatalogManager.CatalogManager;
import Data.*;
import Utils.*;
import BufferManager.Block;

import java.io.*;
import java.util.LinkedHashMap;

public class IndexManager {

    private static LinkedHashMap<String, BPTree<Integer, Address>> IntTreeMap
            = new LinkedHashMap<String, BPTree<Integer, Address>>();
    private static LinkedHashMap<String, BPTree<Double, Address>> FloatTreeMap
            = new LinkedHashMap<String, BPTree<Double, Address>>();
    private static LinkedHashMap<String, BPTree<String, Address>> CharTreeMap
            = new LinkedHashMap<String, BPTree<String, Address>>();

    public static void Initialize() throws IOException {
        try {
            FileInputStream input_file = new FileInputStream(
                    DefaultSetting.INDEX_DIR + "/IntTreeMap.index");
            ObjectInputStream in = new ObjectInputStream(input_file);
            IntTreeMap = (LinkedHashMap<String, BPTree<Integer, Address>>) in.readObject();
            in.close();
            input_file.close();

            input_file = new FileInputStream(
                    DefaultSetting.INDEX_DIR + "/FloatTreeMap.index");
            in = new ObjectInputStream(input_file);
            FloatTreeMap = (LinkedHashMap<String, BPTree<Double, Address>>) in.readObject();
            in.close();
            input_file.close();

            input_file = new FileInputStream(
                    DefaultSetting.INDEX_DIR + "/CharTreeMap.index");
            in = new ObjectInputStream(input_file);
            CharTreeMap = (LinkedHashMap<String, BPTree<String, Address>>) in.readObject();
            in.close();
            input_file.close();
        } catch (Exception e) {
            Store();
        }
    }

    public static void Store() throws IOException {
        FileOutputStream output_file = new FileOutputStream(
                DefaultSetting.INDEX_DIR + "/IntTreeMap.index");
        ObjectOutputStream out = new ObjectOutputStream(output_file);
        out.writeObject(IntTreeMap);
        out.close();
        output_file.close();

        output_file = new FileOutputStream(
                DefaultSetting.INDEX_DIR + "/FloatTreeMap.index");
        out = new ObjectOutputStream(output_file);
        out.writeObject(FloatTreeMap);
        out.close();
        output_file.close();

        output_file = new FileOutputStream(
                DefaultSetting.INDEX_DIR + "/CharTreeMap.index");
        out = new ObjectOutputStream(output_file);
        out.writeObject(CharTreeMap);
        out.close();
        output_file.close();
    }

    public static void BuildIndex(Index index) {

    }


    //* SQL operations
    public static void CreateIndex(Index index) throws SQLException {
        int block_offset = 0, row_count = 0, byte_offset = 0;
        int row_num = CatalogManager.GetRowNum(index.table_name);
        int store_length = IndexManager.GetStoreLength(index.table_name);
        DataType type = CatalogManager.GetAttrType(index.table_name, index.attr_name);

        Block tmp_block = BufferManager.ReadBlock(index.table_name, block_offset);

        switch (type) {
            case INT:
                BPTree<Integer, Address> int_tree = new BPTree<Integer, Address>();
                while (row_count < row_num) {
                    if(byte_offset + store_length >= DefaultSetting.BLOCK_SIZE) {
                        block_offset++;
                        byte_offset = 0;
                        tmp_block = BufferManager.ReadBlock(index.table_name, block_offset);
                        if(tmp_block == null) {
                            throw new SQLException(EType.RuntimeError, 0, "xxx");
                        }
                    }
                    if(tmp_block.ReadInt(byte_offset) < 0) {
                        Address value = new Address(index.table_name, block_offset, byte_offset);
                        Tuple row_data = IndexManager.GetTuple();
                    }
                }
                IntTreeMap.put(index.name, int_tree);
                break;
            case FLOAT:
                BPTree<Double, Address> float_tree = new BPTree<Double, Address>();
                FloatTreeMap.put(index.name, float_tree);
                break;
            case CHAR:
                BPTree<String, Address> char_tree = new BPTree<String, Address>();
                CharTreeMap.put(index.name, char_tree);
                break;
        }
    }


    public static void DropIndex(Index drop_index) {
        String index_name = drop_index.name;
        DataType type = CatalogManager.GetAttrType(drop_index.table_name, drop_index.attr_name);
        switch (type) {
            case INT:
                IntTreeMap.remove(index_name);
                break;
            case FLOAT:
                FloatTreeMap.remove(index_name);
                break;
            case CHAR:
                CharTreeMap.remove(index_name);
                break;
        }
    }


    //* utilities methods

    public static int GetStoreLength(String table_name) {
//        int row_length = CatalogManager.GetRowLength(table_name);
//        if(row_length > )


//        int row_length = CatalogManager.GetRowLength(table_name);
//        if(row_length > DefaultSetting.INT_SIZE) {
//
//        } else {
//
//        }
        return 0;
    }

    public static Tuple GetTuple(String table_name, Block block, int offset) {

    }
}
