package IndexManager;

import BufferManager.BufferManager;
import CatalogManager.CatalogManager;
import Data.*;
import RecordManager.RecordManager;
import Utils.*;
import BufferManager.Block;

import java.io.*;
import java.util.ArrayList;
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


    //* SQL operations
    public static void CreateIndex(Index index) throws SQLException {
        int block_offset = 0, row_count = 0, byte_offset = DefaultSetting.INT_SIZE;
        int row_num = CatalogManager.GetTupleNum(index.table_name);
        int store_length = CatalogManager.GetStoreLength(index.table_name);
        DataType type = CatalogManager.GetAttrType(index.table_name, index.attr_name);
        int tuple_index = CatalogManager.GetAttrIndex(index.table_name, index.attr_name);

        Block tmp_block = BufferManager.ReadBlock(index.table_name, block_offset);

        switch (type) {
            case INT:
                BPTree<Integer, Address> int_tree = new BPTree<Integer, Address>();
                while (row_count < row_num) {
                    if (byte_offset + store_length >= DefaultSetting.BLOCK_SIZE) {
                        block_offset++;
                        byte_offset = 0;
                        tmp_block = BufferManager.ReadBlock(index.table_name, block_offset);
                        if (tmp_block == null) {
                            throw new SQLException(EType.RuntimeError, 30, "read block failed");
                        }
                    }
                    if (tmp_block.ReadInt(byte_offset) < 0) {
                        Address value = new Address(index.table_name, block_offset, byte_offset);
                        Tuple row_data = RecordManager.GetTuple(index.table_name, tmp_block, byte_offset);
                        Integer key = Integer.parseInt(row_data.GetValue(tuple_index));
                        int_tree.Insert(key, value);
                        row_count++;
                    }
                    byte_offset += store_length;
                }
                IntTreeMap.put(index.name, int_tree);
                break;
            case FLOAT:
                BPTree<Double, Address> float_tree = new BPTree<Double, Address>();
                while (row_count < row_num) {
                    if (byte_offset + store_length >= DefaultSetting.BLOCK_SIZE) {
                        block_offset++;
                        byte_offset = 0;
                        tmp_block = BufferManager.ReadBlock(index.table_name, block_offset);
                        if (tmp_block == null) {
                            throw new SQLException(EType.RuntimeError, 30, "read block failed");
                        }
                    }
                    if (tmp_block.ReadInt(byte_offset) < 0) {
                        Address value = new Address(index.table_name, block_offset, byte_offset);
                        Tuple row_data = RecordManager.GetTuple(index.table_name, tmp_block, byte_offset);
                        Double key = Double.parseDouble(row_data.GetValue(tuple_index));
                        float_tree.Insert(key, value);
                        row_count++;
                    }
                    byte_offset += store_length;
                }
                FloatTreeMap.put(index.name, float_tree);
                break;
            case CHAR:
                BPTree<String, Address> char_tree = new BPTree<String, Address>();
                while (row_count < row_num) {
                    if (byte_offset + store_length >= DefaultSetting.BLOCK_SIZE) {
                        block_offset++;
                        byte_offset = 0;
                        tmp_block = BufferManager.ReadBlock(index.table_name, block_offset);
                        if (tmp_block == null) {
                            throw new SQLException(EType.RuntimeError, 30, "read block failed");
                        }
                    }
                    if (tmp_block.ReadInt(byte_offset) < 0) {
                        Address value = new Address(index.table_name, block_offset, byte_offset);
                        Tuple row_data = RecordManager.GetTuple(index.table_name, tmp_block, byte_offset);
                        String key = row_data.GetValue(tuple_index);
                        char_tree.Insert(key, value);
                        row_count++;
                    }
                    byte_offset += store_length;
                }
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

    public static void Insert(Index index, String key, Address insert_addr) {
        DataType type = CatalogManager.GetAttrType(index.table_name, index.attr_name);
        switch (type) {
            case INT:
                BPTree<Integer, Address> int_tree = IntTreeMap.get(index.name);
                int_tree.Insert(Integer.parseInt(key), insert_addr);
                break;
            case FLOAT:
                BPTree<Double, Address> float_tree = FloatTreeMap.get(index.name);
                float_tree.Insert(Double.parseDouble(key), insert_addr);
                break;
            case CHAR:
                BPTree<String, Address> char_tree = CharTreeMap.get(index.name);
                char_tree.Insert(key, insert_addr);
                break;
        }
    }

    public static ArrayList<Address> Select(Index index, WhereCond condition) {
        ArrayList<Address> res = new ArrayList<>();
        if (index == null) return res;
        DataType type = CatalogManager.GetAttrType(index.table_name, index.attr_name);
        switch (type) {
            case INT:
                BPTree<Integer, Address> int_tree = IntTreeMap.get(index.name);
                res = int_tree.Search(Integer.parseInt(condition.expr2), condition.cmp);
                break;
            case FLOAT:
                BPTree<Double, Address> float_tree = FloatTreeMap.get(index.name);
                res = float_tree.Search(Double.parseDouble(condition.expr2), condition.cmp);
                break;
            case CHAR:
                BPTree<String, Address> char_tree = CharTreeMap.get(index.name);
                res = char_tree.Search(condition.expr2, condition.cmp);
                break;
        }
        return res;
    }

    public static void Delete(Index index, String key) {
        DataType type = CatalogManager.GetAttrType(index.table_name, index.attr_name);
        switch (type) {
            case INT:
                BPTree<Integer, Address> int_tree = IntTreeMap.get(index.name);
                int_tree.Delete(Integer.parseInt(key));
                break;
            case FLOAT:
                BPTree<Double, Address> float_tree = FloatTreeMap.get(index.name);
                float_tree.Delete(Double.parseDouble(key));
                break;
            case CHAR:
                BPTree<String, Address> char_tree = CharTreeMap.get(index.name);
                char_tree.Delete(key);
                break;
        }
    }
}
