package API;

import Data.*;

import javax.xml.crypto.Data;
import java.util.ArrayList;

public class API {
    private static String delete_table;
    private static String drop_table;
    private static String drop_index;
    private static String create_index;
    private static String create_table;
    private static String on_table;
    private static String insert_value;
    private static String insert_table;
    private static String on_attribute;
    private static WhereCond temp_where_cond = new WhereCond();
    private static Attribute temp_attr = new Attribute();

    private static ArrayList<String> temp_primary_list = new ArrayList<String>();
    private static ArrayList<WhereCond> where_condition = new ArrayList<WhereCond>();
    private static ArrayList<InsertVal> insert_value_list = new ArrayList<InsertVal>();
    private static ArrayList<Attribute> create_attr_list = new ArrayList<Attribute>();

    public API() {
        Clear();
    }

    public static void Initialize() {

    }


    public static void Clear() {
        temp_primary_list.clear();
        where_condition.clear();
        insert_value_list.clear();
        create_attr_list.clear();
    }

    public static void QueryDelete() {

        Clear();
    }

    public static void QueryDropIndex() {

    }

    public static void QueryDropTable() {

    }

    public static void QueryInsert() {

        Clear();
    }

    public static void QueryCreateIndex() {

    }

    public static void QueryCreateTable() {

    }

    public static void QuerySelect() {

    }


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

    public static void SetAttrLength(int length) {
        temp_attr.length = length;
    }

    public static void SetAttrUnique(boolean unique) {
        temp_attr.unique = unique;
    }

    public static void SetPrimary(String attr_name) {
        temp_primary_list.add(attr_name);
    }


}
