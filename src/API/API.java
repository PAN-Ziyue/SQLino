package API;

import BufferManager.BufferManager;
import CatalogManager.CatalogManager;

import IndexManager.IndexManager;
import Interpreter.*;
import RecordManager.RecordManager;
import Utils.*;
import Data.*;
import com.jakewharton.fliptables.FlipTable;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private static String insert_table = "";
    private static String on_attribute = "";
    private static String select_table = "";
    private static String temp_primary = "";
    private static String exec_file = "";
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
            throw new SQLException(EType.RuntimeError, 1,
                    "failed to initialize API");
        }
    }

    public static void Store() throws SQLException {
        try {
            CatalogManager.Store();
            BufferManager.Store();
            IndexManager.Store();
        } catch (Exception e) {
            throw new SQLException(EType.RuntimeError, 2,
                    "failed to save");
        }
    }

    public static void Clear() {
        temp_primary = "";
        temp_attr.Clear();
        primary_defined = false;
        temp_where_cond = new WhereCond();
        where_condition.clear();
        insert_value_list = new ArrayList<>();
        create_attr_list = new ArrayList<>();
        select_attr_list.clear();
    }

    //* query function
    public static void QueryCreateTable() throws SQLException {
        long start_time = System.currentTimeMillis();
        if (CatalogManager.IsTableExist(create_table)) {
            throw new SQLException(EType.RuntimeError, 3,
                    "failed to create table, " + create_table + " has already existed!");
        }
        if (create_attr_list.size() > 32)
            throw new SQLException(EType.RuntimeError, 4, "too many attributes");

        HashSet<String> check_duplicate_attr = new HashSet<>();
        for (Attribute attr : create_attr_list) {
            if (check_duplicate_attr.contains(attr.name)) {
                throw new SQLException(EType.RuntimeError, 5,
                        "failed to create table, " + attr.name + " has duplicate attributes!");
            } else {
                check_duplicate_attr.add(attr.name);
            }
        }

        if (primary_defined) {
            boolean contain_primary = false;
            for (int i = 0; i < create_attr_list.size(); i++) {
                if (create_attr_list.get(i).name.equals(temp_primary)) {
                    contain_primary = true;
                    create_attr_list.get(i).primary = true;
                    create_attr_list.get(i).unique = true;// primary key must be unique
                    break;
                }
            }
            if (!contain_primary)
                throw new SQLException(EType.RuntimeError, 6,
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
        long end_time = System.currentTimeMillis();

        if (!Interpreter.parse_script) {
            String success = "successfully create a table, time elapsed: "
                    + (end_time - start_time) + "ms";
            System.out.println("\033[1;30m" + success + "\033[0m");
        }
        Store();
        Clear();
    }

    public static void QueryDropTable() throws SQLException {
        long start_time = System.currentTimeMillis();
        if (CatalogManager.IsTableExist(drop_table)) {
            Table tmp = CatalogManager.GetTable(drop_table);
            for (Index drop_table_index : tmp.index_list) {
                IndexManager.DropIndex(drop_table_index);
            }
            CatalogManager.DropTable(drop_table);
            BufferManager.Drop(drop_table);
        } else {
            throw new SQLException(EType.RuntimeError, 7,
                    "cannot drop table, table does not exist!");
        }
        long end_time = System.currentTimeMillis();

        if (!Interpreter.parse_script) {
            String success = "successfully drop a table, time elapsed: "
                    + (end_time - start_time) + "ms";
            System.out.println("\033[1;30m" + success + "\033[0m");
        }
        Store();
        Clear();
    }

    public static void QueryInsert() throws SQLException {
        long start_time = System.currentTimeMillis();
        if (!CatalogManager.IsTableExist(insert_table))
            throw new SQLException(EType.RuntimeError, 8,
                    "cannot insert value, table does not exist!");

        Table tmp = CatalogManager.GetTable(insert_table);
        if (insert_value_list.size() != tmp.attr_list.size())
            throw new SQLException(EType.RuntimeError, 9, "attribute number does not match");
        for (int i = 0; i < tmp.attr_list.size(); i++) {
            if (tmp.attr_list.get(i).type != insert_value_list.get(i).type) {
                if (tmp.attr_list.get(i).type == DataType.FLOAT
                        && insert_value_list.get(i).type == DataType.INT) {
                    insert_value_list.get(i).type = DataType.FLOAT;
                } else
                    throw new SQLException(EType.RuntimeError, 10, "attribute type does not match");
            }

            if (tmp.attr_list.get(i).type == DataType.CHAR &&
                    insert_value_list.get(i).val.length() > tmp.attr_list.get(i).char_length)
                throw new SQLException(EType.RuntimeError, 11, "exceed the char length");
            // check unique
            if (tmp.attr_list.get(i).unique) {
                WhereCond check_unique_cond = new WhereCond();
                check_unique_cond.expr1 = tmp.attr_list.get(i).name;
                check_unique_cond.is_expr1_attr = true;
                check_unique_cond.cmp = CMP.EQUAL;
                check_unique_cond.expr2 = insert_value_list.get(i).val;
                check_unique_cond.is_expr2_attr = false;
                check_unique_cond.type2 = insert_value_list.get(i).type;
                where_condition.add(check_unique_cond);
                if (RecordManager.Select(insert_table, where_condition).size() != 0)
                    throw new SQLException(EType.RuntimeError, 12, "duplicate unique attribute");
            }
        }

        Address insert_addr = RecordManager.Insert(insert_table, insert_value_list);
        CatalogManager.Insert(insert_table);
        for (Index index : tmp.index_list) {
            int attr_index = CatalogManager.GetAttrIndex(index.table_name, index.attr_name);
            IndexManager.Insert(index, insert_value_list.get(attr_index).val, insert_addr);
        }
        long end_time = System.currentTimeMillis();

        if (!Interpreter.parse_script) {
            String success = "successfully insert a tuple, time elapsed: "
                    + (end_time - start_time) + "ms";
            System.out.println("\033[1;30m" + success + "\033[0m");
        }
        Store();
        Clear();
    }

    public static void QuerySelect() throws SQLException {
        long start_time = System.currentTimeMillis();
        if (!CatalogManager.IsTableExist(select_table))
            throw new SQLException(EType.RuntimeError, 13, "table does not exist");

        for (String attr_name : select_attr_list) {
            if (!CatalogManager.IsAttrExist(select_table, attr_name))
                throw new SQLException(EType.RuntimeError, 14, "attribute does not exist");
        }

        for (WhereCond cond : where_condition) {
            if (cond.is_expr1_attr && !CatalogManager.IsAttrExist(select_table, cond.expr1)) {
                throw new SQLException(EType.RuntimeError, 15, "attribute does not exist");
            }
            if (cond.is_expr2_attr && !CatalogManager.IsAttrExist(select_table, cond.expr2)) {
                throw new SQLException(EType.RuntimeError, 15, "attribute does not exist");
            }
            if (!cond.is_expr1_attr && !cond.is_expr2_attr)
                throw new SQLException(EType.RuntimeError, 16, "conditions cannot be both constants");
        }

        ArrayList<Tuple> result = new ArrayList<>();
        WhereCond indexed_condition = null;
        Index index = null;
        for (WhereCond cond : where_condition) {
            if (cond.is_expr1_attr && !cond.is_expr2_attr) {
                if (CatalogManager.IsIndexAttr(select_table, cond.expr1)) {
                    indexed_condition = cond;
                    where_condition.remove(cond);
                    index = CatalogManager.GetIndex(select_table, indexed_condition.expr1);
                    break;
                }
            } else if (!cond.is_expr1_attr && cond.is_expr2_attr) {
                if (CatalogManager.IsIndexAttr(select_table, cond.expr2)) {
                    WhereCond tmp_cond = new WhereCond();
                    tmp_cond.expr1 = cond.expr2;
                    tmp_cond.expr2 = cond.expr1;
                    tmp_cond.is_expr2_attr = cond.is_expr1_attr;
                    tmp_cond.is_expr1_attr = cond.is_expr2_attr;
                    tmp_cond.type1 = cond.type2;
                    tmp_cond.type2 = cond.type1;
                    tmp_cond.cmp = cond.cmp;
                    indexed_condition = tmp_cond;
                    where_condition.remove(cond);
                    index = CatalogManager.GetIndex(select_table, indexed_condition.expr1);
                    break;
                }
            }
        }

        if (indexed_condition != null) {
            ArrayList<Address> address_list = IndexManager.Select(index, indexed_condition);
            result = RecordManager.Select(select_table, address_list, where_condition);
        } else {
            result = RecordManager.Select(select_table, where_condition);
        }
        long end_time = System.currentTimeMillis();

        if (!Interpreter.parse_script) {
            String success = result.size() + " tuples found:" + ", time elapsed: "
                    + (end_time - start_time) + "ms";
            System.out.println("\033[1;30m" + success + "\033[0m");

            if (select_attr_list.size() == 0) {
                int attr_num = CatalogManager.GetAttrNum(select_table);
                Table tmp = CatalogManager.GetTable(select_table);
                String[] headers = new String[attr_num];
                for (int i = 0; i < attr_num; i++) {
                    String header_name;
                    if (tmp.attr_list.get(i).name.equals(tmp.primary_attr)) {
                        header_name = tmp.attr_list.get(i).name;
                        if (header_name.length() < 3) header_name = "  " + header_name;
                        headers[i] = "\033[1;33m" + header_name +
                                "\u001B[0m (" + tmp.attr_list.get(i).type.name() + ")";
                    } else if (tmp.attr_list.get(i).unique) {
                        header_name = tmp.attr_list.get(i).name;
                        if (header_name.length() < 3) header_name = "  " + header_name;
                        headers[i] = "\033[0;33m" + header_name +
                                "\u001B[0m (" + tmp.attr_list.get(i).type.name() + ")";
                    } else {
                        header_name = tmp.attr_list.get(i).name;
                        if (header_name.length() < 3) header_name = "  " + header_name;
                        headers[i] = header_name + " (" + tmp.attr_list.get(i).type.name() + ")";
                    }

                }
                int final_size, remain = 0;
                if (result.size() <= DefaultSetting.DISPLAY_NUM)
                    final_size = result.size();
                else {
                    final_size = DefaultSetting.DISPLAY_NUM;
                    remain = result.size() - DefaultSetting.DISPLAY_NUM;
                }
                String[][] data = new String[final_size][attr_num];
                for (int i = 0; i < final_size; i++) {
                    for (int j = 0; j < attr_num; j++)
                        data[i][j] = result.get(i).value_list.get(j);
                }
                System.out.print(FlipTable.of(headers, data));
                if (remain > 0)
                    System.out.println("And " + remain + " more tuples...");
            } else {
                int attr_num = select_attr_list.size();
                Table tmp = CatalogManager.GetTable(select_table);
                String[] headers = new String[attr_num];
                for (int i = 0; i < attr_num; i++) {
                    int idx = CatalogManager.GetAttrIndex(select_table, select_attr_list.get(i));
                    String header_name;
                    if (tmp.attr_list.get(idx).name.equals(tmp.primary_attr)) {
                        header_name = tmp.attr_list.get(i).name;
                        if (header_name.length() < 3) header_name = "  " + header_name;
                        headers[i] = "\033[1;33m" + header_name +
                                "\u001B[0m (" + tmp.attr_list.get(idx).type.name() + ")";
                    } else if (tmp.attr_list.get(idx).unique) {
                        header_name = tmp.attr_list.get(i).name;
                        if (header_name.length() < 3) header_name = "  " + header_name;
                        headers[i] = "\033[0;33m" + header_name +
                                "\u001B[0m (" + tmp.attr_list.get(idx).type.name() + ")";
                    } else {
                        header_name = tmp.attr_list.get(i).name;
                        if (header_name.length() < 3) header_name = "  " + header_name;
                        headers[i] = header_name + " (" + tmp.attr_list.get(idx).type.name() + ")";
                    }
                }
                int final_size, remain = 0;
                if (result.size() <= DefaultSetting.DISPLAY_NUM)
                    final_size = result.size();
                else {
                    final_size = DefaultSetting.DISPLAY_NUM;
                    remain = result.size() - DefaultSetting.DISPLAY_NUM;
                }
                String[][] data = new String[final_size][attr_num];
                for (int i = 0; i < final_size; i++) {
                    for (int j = 0; j < attr_num; j++) {
                        int idx = CatalogManager.GetAttrIndex(select_table, select_attr_list.get(j));
                        data[i][j] = result.get(i).value_list.get(idx);
                    }
                }
                System.out.print(FlipTable.of(headers, data));
                if (remain > 0)
                    System.out.println("\033[1;30m" + "And " + remain + " more tuples..." + "\033[0m");
            }
        }
        Store();
        Clear();
    }

    public static void QueryDelete() throws SQLException {
        long start_time = System.currentTimeMillis();
        if (!CatalogManager.IsTableExist(select_table))
            throw new SQLException(EType.RuntimeError, 17, "cannot delete, table does not exist");

        for (WhereCond cond : where_condition) {
            if (cond.is_expr1_attr && !CatalogManager.IsAttrExist(select_table, cond.expr1)) {
                throw new SQLException(EType.RuntimeError, 18, "attribute does not exist");
            }
            if (cond.is_expr2_attr && !CatalogManager.IsAttrExist(select_table, cond.expr2)) {
                throw new SQLException(EType.RuntimeError, 18, "attribute does not exist");
            }
            if (!cond.is_expr1_attr && !cond.is_expr2_attr)
                throw new SQLException(EType.RuntimeError, 19, "conditions cannot be both constants");
        }

        WhereCond indexed_condition = null; // get the index
        Index index = null;
        for (WhereCond cond : where_condition) {
            if (cond.is_expr1_attr && !cond.is_expr2_attr) {
                if (CatalogManager.IsIndexAttr(select_table, cond.expr1)) {
                    indexed_condition = cond;
                    where_condition.remove(cond);
                    index = CatalogManager.GetIndex(select_table, indexed_condition.expr1);
                    break;
                }
            } else if (!cond.is_expr1_attr && cond.is_expr2_attr) {
                if (CatalogManager.IsIndexAttr(select_table, cond.expr2)) {
                    WhereCond tmp_cond = new WhereCond();
                    tmp_cond.expr1 = cond.expr2;
                    tmp_cond.expr2 = cond.expr1;
                    tmp_cond.is_expr2_attr = cond.is_expr1_attr;
                    tmp_cond.is_expr1_attr = cond.is_expr2_attr;
                    tmp_cond.type1 = cond.type2;
                    tmp_cond.type2 = cond.type1;
                    tmp_cond.cmp = cond.cmp;
                    indexed_condition = tmp_cond;
                    where_condition.remove(cond);
                    index = CatalogManager.GetIndex(select_table, indexed_condition.expr1);
                    break;
                }
            }
        }

        int delete_count;
        if (indexed_condition != null) {
            ArrayList<Address> address_list = IndexManager.Select(index, indexed_condition);
            delete_count = RecordManager.Delete(delete_table, address_list, where_condition);
        } else {
            delete_count = RecordManager.Delete(delete_table, where_condition);
        }
        CatalogManager.Delete(delete_table, delete_count);

        long end_time = System.currentTimeMillis();

        if (!Interpreter.parse_script) {
            String success = "successfully delete " + delete_count
                    + " tuples, time elapsed: " + (end_time - start_time) + "ms";
            System.out.println("\033[1;30m" + success + "\033[0m");
        }
        Store();
        Clear();
    }

    public static void QueryCreateIndex() throws SQLException {
        long start_time = System.currentTimeMillis();
        if (CatalogManager.IsIndexExist(create_index)) {
            throw new SQLException(EType.RuntimeError, 20,
                    "this index has been created: " + create_index);
        }
        if (!CatalogManager.IsTableExist(on_table)) {
            throw new SQLException(EType.RuntimeError, 21,
                    "this table does not exist: " + on_table);
        }
        if (!CatalogManager.IsAttrExist(on_table, on_attribute)) {
            throw new SQLException(EType.RuntimeError, 22,
                    "this attribute does not exist: " + on_attribute);
        }
        if (CatalogManager.IsIndexAttr(on_table, on_attribute)) {
            throw new SQLException(EType.RuntimeError, 23,
                    "this attribute has been created index on");
        }
        if (!CatalogManager.IsUnique(on_table, on_attribute)) {
            throw new SQLException(EType.RuntimeError, 24,
                    "cannot create index on not-unique attribute");
        }
        Index index = new Index(create_index, on_table, on_attribute);
        CatalogManager.CreateIndex(index);
        IndexManager.CreateIndex(index);
        long end_time = System.currentTimeMillis();

        if (!Interpreter.parse_script) {
            String success = "successfully create an index, time elapsed: "
                    + (end_time - start_time) + "ms";
            System.out.println("\033[1;30m" + success + "\033[0m");
        }
        Store();
        Clear();
    }

    public static void QueryDropIndex() throws SQLException {
        long start_time = System.currentTimeMillis();
        if (!CatalogManager.IsIndexExist(drop_index))
            throw new SQLException(EType.RuntimeError, 25,
                    "this index does not exist: " + drop_index);
        Index tmp_index = CatalogManager.GetIndex(drop_index);
        IndexManager.DropIndex(tmp_index);
        CatalogManager.DropIndex(tmp_index);
        long end_time = System.currentTimeMillis();

        if (!Interpreter.parse_script) {
            String success = "successfully drop an index, time elapsed: "
                    + (end_time - start_time) + "ms";
            System.out.println("\033[1;30m" + success + "\033[0m");
        }
        Store();
        Clear();
    }

    public static void QueryExecFile() throws SQLException {
        int line_count = 0;
        Interpreter.parse_script = true;
        ArrayList<String> error_log = new ArrayList<>();
        long start_time = System.currentTimeMillis();
        try {
            File script = new File(DefaultSetting.SCRIPT_DIR + "/" + exec_file);
            BufferedReader script_br = new BufferedReader(new FileReader(script));
            String input_line;
            Interpreter.SetState(State.IDLE);
            while ((input_line = script_br.readLine()) != null) {
                line_count++;
                if (!input_line.equals("")) {
                    try {
                        input_line = Interpreter.ProcessInput(input_line);
                        Interpreter.ReadInput(input_line);
                    } catch (Exception sql_e) {
                        Interpreter.SetState(State.IDLE);
                        Clear();
                        String msg = "line: " + line_count + "\t" + sql_e.getMessage();
                        error_log.add(msg);
                    }
                }
            }
            script_br.close();
        } catch (Exception e) {
            throw new SQLException(EType.RuntimeError, 26, "cannot open file: " + exec_file);
        }
        if (error_log.size() > 0) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
            LocalDateTime now = LocalDateTime.now();
            String error_path = DefaultSetting.SCRIPT_OUT_DIR + "/" + dtf.format(now) + ".log";
            File log_file = new File(error_path);
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(log_file));
                for (String tmp : error_log) {
                    bw.write(tmp + "\n");
                }
                Interpreter.parse_script = false;
                bw.close();
            } catch (Exception e) {
                throw new SQLException(EType.RuntimeError, 27,
                        "cannot write error log to file");
            }
            throw new SQLException(EType.RuntimeError, 28,
                    "script execution failed, check log file: " + error_path);
        }
        long end_time = System.currentTimeMillis();
        String success = "script executed successfully, time elapsed: "
                + (end_time - start_time) + "ms";
        System.out.println("\033[1;30m" + success + "\033[0m");
    }


    //* data transfer function
    public static void SetWhereExpr1(String expr1) {
        if (CommonUtils.IsString(expr1)) {
            temp_where_cond.type1 = DataType.CHAR;
            temp_where_cond.is_expr1_attr = false;
            temp_where_cond.expr1 = CommonUtils.ParseString(expr1);
        } else if (CommonUtils.IsInteger(expr1)) {
            temp_where_cond.type1 = DataType.INT;
            temp_where_cond.is_expr1_attr = false;
            temp_where_cond.expr1 = expr1;
        } else if (CommonUtils.IsFloat(expr1)) {
            temp_where_cond.type1 = DataType.FLOAT;
            temp_where_cond.is_expr1_attr = false;
            temp_where_cond.expr1 = expr1;
        } else {
            temp_where_cond.is_expr1_attr = true;
            temp_where_cond.expr1 = expr1;
        }
    }

    public static void SetWhereCmp(CMP cmp) {
        temp_where_cond.cmp = cmp;
    }

    public static void SetWhereExpr2(String expr2) {
        if (CommonUtils.IsString(expr2)) {
            temp_where_cond.type2 = DataType.CHAR;
            temp_where_cond.is_expr2_attr = false;
            temp_where_cond.expr2 = CommonUtils.ParseString(expr2);
        } else if (CommonUtils.IsInteger(expr2)) {
            temp_where_cond.type2 = DataType.INT;
            temp_where_cond.is_expr2_attr = false;
            temp_where_cond.expr2 = expr2;
        } else if (CommonUtils.IsFloat(expr2)) {
            temp_where_cond.type2 = DataType.FLOAT;
            temp_where_cond.is_expr2_attr = false;
            temp_where_cond.expr2 = expr2;
        } else {
            temp_where_cond.is_expr2_attr = true;
            temp_where_cond.expr2 = expr2;
        }
        where_condition.add(temp_where_cond);
        temp_where_cond = new WhereCond();
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
        if (length >= 1 && length <= 255) {
            temp_attr.char_length = length;
        }
    }

    public static void SetAttrUnique() {
        temp_attr.unique = true;
    }

    public static void SetPrimary(String attr_name) throws SQLException {
        if (primary_defined) {
            throw new SQLException(EType.RuntimeError, 1,
                    "cannot define primary key on multiple attributes");
        } else {
            primary_defined = true;
            temp_primary = attr_name;
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

    public static void SetExecFile(String exec_file) {
        API.exec_file = exec_file;
    }
}
