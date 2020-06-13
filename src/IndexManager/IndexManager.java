package IndexManager;

import CatalogManager.CatalogManager;
import Data.*;
import Utils.DefaultSetting;

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


    public static void CreateIndex(Index index) {
        int tuple_name = CatalogManager.GetRowNum(index.table_name);
        DataType type = CatalogManager.GetAttrType(index.table_name, index.attr_name);


    }

    public static void DropIndex(String index_name) {

    }
}
