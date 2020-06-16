package CatalogManager;

import Data.*;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import Utils.*;

public class CatalogManager {
    public static LinkedHashMap<String, Table> table_list =
            new LinkedHashMap<String, Table>();

    public static LinkedHashMap<String, Index> index_list =
            new LinkedHashMap<String, Index>();

    public static void Initialize() throws IOException, ClassNotFoundException {
        InitTableCatalog();
        InitIndexCatalog();
    }

    public static void Store() throws IOException {
        StoreTableCatalog();
        StoreIndexCatalog();
    }

    private static void InitTableCatalog() throws IOException {
        try {
            FileInputStream input_file = new FileInputStream(
                    DefaultSetting.TABLE_CATALOG_PATH);
            ObjectInputStream in = new ObjectInputStream(input_file);
            table_list = (LinkedHashMap<String, Table>) in.readObject();
            in.close();
            input_file.close();
        } catch (Exception e) {
            StoreTableCatalog();
        }
    }

    private static void InitIndexCatalog() throws IOException {
        try {
            FileInputStream input_file = new FileInputStream(
                    DefaultSetting.INDEX_CATALOG_PATH);
            ObjectInputStream in = new ObjectInputStream(input_file);
            index_list = (LinkedHashMap<String, Index>) in.readObject();
            in.close();
            input_file.close();
        } catch (Exception e) {
            StoreIndexCatalog();
        }
    }

    private static void StoreTableCatalog() throws IOException {
        FileOutputStream output_file = new FileOutputStream(
                DefaultSetting.TABLE_CATALOG_PATH);
        ObjectOutputStream out = new ObjectOutputStream(output_file);
        out.writeObject(table_list);
        out.close();
        output_file.close();
    }

    private static void StoreIndexCatalog() throws IOException {
        FileOutputStream output_file = new FileOutputStream(
                DefaultSetting.INDEX_CATALOG_PATH);
        ObjectOutputStream out = new ObjectOutputStream(output_file);
        out.writeObject(index_list);
        out.close();
        output_file.close();
    }


    //* sql methods
    public static void CreateTable(Table new_table) {
        table_list.put(new_table.name, new_table);
    }

    public static void DropTable(String table_name) {
        Table tmp = GetTable(table_name);
        for (int i = 0; i < tmp.index_list.size(); i++) {
            index_list.remove(tmp.index_list.get(i).name);
        }
        table_list.remove(table_name);
    }

    public static void Insert(String table_name) {
        table_list.get(table_name).row_num++;
    }


    // table info
    public static Table GetTable(String name) {
        return table_list.get(name);
    }

    public static Index GetIndex(String name) throws SQLException {
        if (index_list.containsKey(name)) {
            return index_list.get(name);
        } else {
            throw new SQLException(EType.RuntimeError, 3,
                    name + "(index) does not exist");
        }
    }

    public static Index GetIndex(String table_name, String attr_name) throws SQLException {
        Table tmp = GetTable(table_name);
        for (Index index : tmp.index_list) {
            if (index.attr_name.equals(attr_name))
                return index;
        }
        throw new SQLException(EType.RuntimeError, 0, "cannot find index");
    }

    public static String GetPrimaryKey(String table_name) {
        return GetTable(table_name).primary_attr;
    }

    public static int GetAttrNum(String table_name) {
        return GetTable(table_name).attr_num;
    }

    public static int GetRowNum(String table_name) {
        return GetTable(table_name).row_num;
    }

    public static DataType GetAttrType(String table_name, String attr_name) {
        Table tmp = table_list.get(table_name);
        for (Attribute attr : tmp.attr_list) {
            if (attr_name.equals(attr.name))
                return attr.type;
        }
        return DataType.INT;
    }

    public static DataType GetAttrType(String table_name, int i) {
        return GetTable(table_name).attr_list.get(i).type;
    }

    public static boolean IsIndexExist(String index_name) {
        return index_list.containsKey(index_name);
    }

    public static boolean IsTableExist(String table_name) {
        return table_list.containsKey(table_name);
    }

    public static boolean IsAttrExist(String table_name, String attr_name) {
        if (IsTableExist(table_name)) {
            Table tmp = GetTable(table_name);
            for (Attribute attr : tmp.attr_list) {
                if (attr.name.equals(attr_name))
                    return true;
            }
        }
        return false;
    }

    public static boolean IsIndexAttr(String table_name, String attr_name) {
        if (IsAttrExist(table_name, attr_name)) {
            Table tmp = GetTable(table_name);
            for (Index tmp_index : tmp.index_list) {
                if (tmp_index.attr_name.equals(attr_name))
                    return true;
            }
        }
        return false;
    }


    public static boolean IsPrimaryKey(String table_name, String attr_name)
            throws SQLException {
        if (table_list.containsKey(table_name)) {
            Table tmp = GetTable(table_name);
            return attr_name.equals(tmp.primary_attr);
        } else {
            throw new SQLException(EType.RuntimeError, 1,
                    table_name + "(table) does not exist");
        }
    }


    public static boolean IsUnique(String table_name, String attr_name)
            throws SQLException {
        if (table_list.containsKey(table_name)) {
            Table tmp = GetTable(table_name);
            for (Attribute i : tmp.attr_list) {
                if (i.name.equals(attr_name)) {
                    return i.unique;
                }
            }
            throw new SQLException(EType.RuntimeError, 2,
                    attr_name + "(attribute) does not exist");
        } else {
            throw new SQLException(EType.RuntimeError, 1,
                    table_name + "(table) does not exist");
        }
    }


    public static void CreateIndex(Index index) {
        Table tmp = GetTable(index.table_name);
        tmp.index_list.add(index);
        tmp.index_num += 1;
        index_list.put(index.name, index);
    }

    public static void DropIndex(String index_name)
            throws SQLException {

    }

    public static int GetAttrIndex(String table_name, String attr_name) throws SQLException {
        Table tmp = table_list.get(table_name);
        int index = 0;
        for (Attribute attr : tmp.attr_list) {
            if (attr.name.equals(attr_name))
                return index;
            index++;
        }
        throw new SQLException(EType.RuntimeError, 1,
                table_name + "(table) does not exist");
    }

    public static int GetRowLength(String table_name) {
        return GetTable(table_name).row_length;
    }

    public static int GetStoreLength(String table_name) {
        int row_length = GetRowLength(table_name);

        if (row_length > DefaultSetting.INT_SIZE) {
            return row_length + DefaultSetting.CHAR_SIZE;
        } else {
            return DefaultSetting.INT_SIZE + DefaultSetting.CHAR_SIZE;
        }
    }

    public static int GetAttrLength(String table_name, String attr_name) {
        Table tmp = GetTable(table_name);
        for (Attribute attr : tmp.attr_list) {
            if (attr.name.equals(attr_name))
                return attr.GetLength();
        }
        return -1;
    }

    public static int GetAttrLength(String table_name, int i) {
        return GetTable(table_name).attr_list.get(i).GetLength();
    }

}
