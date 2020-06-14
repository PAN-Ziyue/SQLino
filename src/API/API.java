package API;

import BufferManager.BufferManager;
import CatalogManager.CatalogManager;

import IndexManager.IndexManager;
import RecordManager.RecordManager;
import Utils.*;
import Data.*;

import java.util.*;

public class API {

    //* variables get from the interpreter
    private static boolean primary_defined = false;
    private static String delete_table = "";
    private static String drop_table = "";
    private static String drop_index = "";
    private static String create_index = "";
    private static String create_table = "";
    private static String on_table = "";
    private static String insert_value = "";
    private static String insert_table = "";
    private static String on_attribute = "";
    private static String select_table = "";
    private static String temp_primary = "";
    private static WhereCond temp_where_cond = new WhereCond();
    private static Attribute temp_attr = new Attribute();

    private static ArrayList<WhereCond> where_condition = new ArrayList<WhereCond>();
    private static ArrayList<InsertVal> insert_value_list = new ArrayList<InsertVal>();
    private static ArrayList<Attribute> create_attr_list = new ArrayList<Attribute>();
    private static ArrayList<String> select_attr_list = new ArrayList<String>();


    //* API utility functions
    public static void Initialize() throws SQLException {
        try {
            CatalogManager.Initialize();
            IndexManager.Initialize();
            BufferManager.Initialize();
        } catch (Exception e) {
            throw new SQLException(EType.RuntimeError, 3,
                    "failed to initialize API, quit MiniSQL now!");
        }
    }

    public static void Store() throws SQLException {
        try {
            CatalogManager.Store();
            IndexManager.Store();
        } catch (Exception e) {
            throw new SQLException(EType.RuntimeError, 9,
                    "failed to save ");
        }
    }


    public static void Clear() {
        temp_primary = "";
        temp_attr.Clear();
        primary_defined = false;
        where_condition.clear();
        insert_value_list.clear();
        create_attr_list.clear();
        select_attr_list.clear();
    }

    //* query function
    public static void QueryCreateTable() throws SQLException {
        if (CatalogManager.IsTableExist(create_table)) {
            throw new SQLException(EType.RuntimeError, 3,
                    "failed to create table, " + create_table + " has already existed!");
        } else {
            HashSet<String> check_duplicate_attr = new HashSet<>();
            for (Attribute attr : create_attr_list) {
                if (check_duplicate_attr.contains(attr.name)) {
                    throw new SQLException(EType.RuntimeError, 3,
                            "failed to create table, " + attr.name + " has duplicate attributes!");
                } else {
                    check_duplicate_attr.add(attr.name);
                }
            }

            if (primary_defined) {
                boolean contain_primary = false;
                for (Attribute attr : create_attr_list) {
                    if (attr.name.equals(temp_primary)) {
                        contain_primary = true;
                        attr.unique = true;                 // primary key must be unique
                        break;
                    }
                }
                if (!contain_primary)
                    throw new SQLException(EType.RuntimeError, 3,
                            "failed to create table, " + temp_primary + " is not a defined attribute!");


                Table new_table = new Table(create_table, create_attr_list, temp_primary);
                RecordManager.CreateTable(new_table.name);
                CatalogManager.CreateTable(new_table);
                String index_name = create_table + "_default_index";
                Index index = new Index(index_name, create_table, temp_primary);
                IndexManager.CreateIndex(index);
                CatalogManager.CreateIndex(index);
            } else {
                Table new_table = new Table(create_table, create_attr_list);
                RecordManager.CreateTable(create_table);
                CatalogManager.CreateTable(new_table);
            }
        }
        Store();
        Clear();
    }

    public static void QueryDropTable() throws SQLException {
        if (CatalogManager.IsTableExist(drop_table)) {
            Table tmp = CatalogManager.GetTable(drop_table);
            for (Index drop_table_index : tmp.index_list) {
                IndexManager.DropIndex(drop_table_index);
            }
            RecordManager.DropTable(drop_table);
            CatalogManager.DropTable(drop_table);
        } else {
            throw new SQLException(EType.RuntimeError, 0, "xxx");
        }
        Store();
        Clear();
    }

    public static void QueryDelete() throws SQLException {

        Clear();
    }

    public static void QueryDropIndex() throws SQLException {

    }

    public static void QueryInsert() throws SQLException {

        Clear();
    }

    public static void QueryCreateIndex() throws SQLException {
        if (CatalogManager.IsIndexExist(create_index)) {
            throw new SQLException(EType.RuntimeError, 6,
                    "this index has been created: " + create_index);
        } else {
            if (CatalogManager.IsTableExist(on_table)) {
                if (CatalogManager.IsAttrExist(on_table, on_attribute)) {
                    if (CatalogManager.IsIndexAttr(on_table, on_attribute)) {
                        throw new SQLException(EType.RuntimeError, 8,
                                "this attribute has been created on");
                    } else {
                        Index index = new Index(create_index, on_table, on_attribute);
                        CatalogManager.CreateIndex(index);
                        IndexManager.CreateIndex(index);
                    }
                } else {
                    throw new SQLException(EType.RuntimeError, 7,
                            "this attribute does not exist: " + on_attribute);
                }
            } else {
                throw new SQLException(EType.RuntimeError, 5,
                        "this table does not exist: " + on_table);
            }
        }
        Clear();
    }


    public static void QuerySelect() throws SQLException {

        Clear();
    }

    //* data transfer function
    public static void SetWhereExpr1(String expr1) {
        temp_where_cond.expr1 = expr1;
    }

    public static void SetWhereCmp(CMP cmp) {
        temp_where_cond.cmp = cmp;
    }

    public static void SetWhereExpr2(String expr2) {
        temp_where_cond.expr2 = expr2;
        where_condition.add(temp_where_cond);
    }

    public static void SetDeleteTable(String delete_table) {
        API.delete_table = delete_table;
    }

    public static void SetDropTable(String drop_table) {
        API.drop_table = drop_table;
    }

    public static void SetDropIndex(String drop_index) {
        API.drop_index = drop_index;
    }

    public static void SetInsertTable(String insert_table) {
        API.insert_table = insert_table;
    }

    public static void SetInsertValue(String val, DataType type) {
        insert_value_list.add(new InsertVal(val, type));
    }

    public static void SetCreateIndex(String create_index) {
        API.create_index = create_index;
    }

    public static void SetCreateTable(String create_table) {
        API.create_table = create_table;
    }

    public static void SetOnTable(String on_table) {
        API.on_table = on_table;
    }

    public static void SetOnAttribute(String on_attribute) {
        API.on_attribute = on_attribute;
    }

    public static void SetCreateAttr(String attr_name) {
        temp_attr.name = attr_name;
    }

    public static void SetAttrType(DataType type) {
        temp_attr.type = type;
    }

    public static void SetAttrLength(int length) throws SQLException {
        if (length >= 1 && length <= 255) {
            temp_attr.length = length;
        } else {
            throw new SQLException(EType.RuntimeError, 5,
                    "this table does not exist: " + on_table);
        }
    }

    public static void SetAttrUnique() {
        temp_attr.unique = true;
    }

    public static boolean SetPrimary(String attr_name) {
        if (primary_defined) {
            return false;
        } else {
            primary_defined = true;
            temp_primary = attr_name;
            return true;
        }
    }

    public static void SetSelectAttr(String select_attr) {
        select_attr_list.add(select_attr);
    }

    public static void SetSelectTable(String select_table) {
        API.select_table = select_table;
    }

    public static void SetCreateAttrList() {
        create_attr_list.add(temp_attr);
        temp_attr = new Attribute();
    }
}
