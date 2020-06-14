package IndexManager;

import CatalogManager.CatalogManager;
import Data.*;
import Utils.DefaultSetting;
import Utils.SQLException;

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
        int tuple_name = CatalogManager.GetRowNum(index.table_name);
        DataType type = CatalogManager.GetAttrType(index.table_name, index.attr_name);

        switch (type) {
            case INT:
                BPTree<Integer, Address> int_tree = new BPTree<Integer, Address>(DefaultSetting.BP_ORDER);
                IntTreeMap.put(index.name, int_tree);
                break;
            case FLOAT:
                BPTree<Double, Address> float_tree = new BPTree<Double, Address>(DefaultSetting.BP_ORDER);
                FloatTreeMap.put(index.name, float_tree);
                break;
            case CHAR:
                BPTree<String, Address> char_tree = new BPTree<String, Address>(DefaultSetting.BP_ORDER);
                CharTreeMap.put(index.name, char_tree);
                break;
        }

//        int length = IndexManager.GetLength(index.table_name);
//        int byte_offset = DefaultSetting.INT_SIZE;
//        int block_offset = 0, count = 0;
//        int attr_index = CatalogManager.GetAttrIndex(index.table_name, index.attr_name);
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

    public static int GetLength(String table_name) {
//        int row_length = CatalogManager.GetRowLength(table_name);
//        if(row_length > DefaultSetting.INT_SIZE) {
//
//        } else {
//
//        }
        return 0;
    }
}
