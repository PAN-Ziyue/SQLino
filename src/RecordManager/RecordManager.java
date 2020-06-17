package RecordManager;

import BufferManager.*;
import CatalogManager.CatalogManager;
import Data.*;
import IndexManager.IndexManager;
import Utils.*;

import java.io.*;
import java.util.*;

public class RecordManager {
    //* SQL operation
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

    public static Address Insert(String table_name, ArrayList<InsertVal> insert_value_list)
            throws SQLException {
        int tuple_num = CatalogManager.GetRowNum(table_name);
        Block header = BufferManager.ReadBlock(table_name, 0);
        if (header == null) throw new SQLException(EType.RuntimeError, 3, "xxx");
        header.is_pinned = true;
        int free_offset = header.ReadInt(0);
        int tuple_offset;

        if (free_offset < 0) tuple_offset = tuple_num;
        else tuple_offset = free_offset;

        int block_offset = GetBlockOffset(table_name, tuple_offset);
        int byte_offset = GetByteOffset(table_name, tuple_offset);

        Block insert_block = BufferManager.ReadBlock(table_name, block_offset);
        if (insert_block == null)
            throw new SQLException(EType.RuntimeError, 2, "cannot find block to insert");
        if (free_offset >= 0) {
            free_offset = insert_block.ReadInt(byte_offset + 1);
            header.WriteInt(free_offset, 0);
        }
        header.is_pinned = false;
        WriteTuple(table_name, insert_value_list, insert_block, byte_offset);
        return new Address(table_name, block_offset, byte_offset);
    }

    public static ArrayList<Tuple> Select(String table_name, ArrayList<WhereCond> condition)
            throws SQLException {
        int row_num = CatalogManager.GetRowNum(table_name);
        int store_length = CatalogManager.GetStoreLength(table_name);
        int row_count = 0, block_offset = 0;
        int byte_offset = DefaultSetting.INT_SIZE;
        ArrayList<Tuple> rst = new ArrayList<>();

        Block block = BufferManager.ReadBlock(table_name, block_offset);
        if (block == null)
            throw new SQLException(EType.RuntimeError, 3, "xxx");
        while (row_count < row_num) {
            if (byte_offset + store_length >= DefaultSetting.BLOCK_SIZE) {
                block_offset++;
                byte_offset = 0;
                block = BufferManager.ReadBlock(table_name, block_offset);
                if (block == null) return rst;
            }
            if (block.ReadInt(byte_offset) < 0) {
                int i;
                Tuple data = GetTuple(table_name, block, byte_offset);
                for (i = 0; i < condition.size(); i++) {
                    if (!CheckCondition(table_name, condition.get(i), data))
                        break;
                }
                if (i == condition.size()) {
                    rst.add(data);
                }
                row_count++;
            }
            byte_offset += store_length;
        }
        return rst;
    }

    public static ArrayList<Tuple> Select(
            String table_name, ArrayList<Address> address_list, ArrayList<WhereCond> condition)
            throws SQLException {
        ArrayList<Tuple> res = new ArrayList<>();
        if (address_list.size() == 0) return res;
        Collections.sort(address_list);
        int block_offset = 0, previous_offset = -1;
        int byte_offset = 0;
        Block block = null;
        for (int i = 0; i < address_list.size(); i++) {
            block_offset = address_list.get(i).block_offset;
            byte_offset = address_list.get(i).byte_offset;
            if (i == 0 || block_offset != previous_offset) {
                block = BufferManager.ReadBlock(table_name, block_offset);
                if (block == null && i == 0) {
                    throw new SQLException(EType.RuntimeError, 0, "xxx");
                }
            }
            if (block.ReadInt(byte_offset) < 0) {
                int j;
                Tuple data = GetTuple(table_name, block, byte_offset);
                for (j = 0; j < condition.size(); j++) {
                    if (!CheckCondition(table_name, condition.get(j), data))
                        break;
                }
                if (j == condition.size()) {
                    res.add(data);
                }
            }
            previous_offset = block_offset;
        }
        return res;
    }

    public static int Delete(String delete_table, ArrayList<WhereCond> condition)
            throws SQLException {
        int tuple_num = CatalogManager.GetRowNum(delete_table);
        int store_length = CatalogManager.GetStoreLength(delete_table);

        int tuple_count = 0, byte_offset = DefaultSetting.INT_SIZE, block_offset = 0;
        int delete_count = 0, read_count = 0;
        Block head_block = BufferManager.ReadBlock(delete_table, block_offset);
        Block current_block = head_block;
        if (head_block == null)
            throw new SQLException(EType.RuntimeError, 3, "xxx");
        head_block.is_pinned = true;

        while (tuple_count < tuple_num) {
            if (byte_offset + store_length >= DefaultSetting.BLOCK_SIZE) {
                block_offset++;
                byte_offset = 0;
                current_block = BufferManager.ReadBlock(delete_table, block_offset);
                if (current_block == null) {
                    head_block.is_pinned = false;
                    return delete_count;
                }
            }
            if (current_block.ReadInt(byte_offset) < 0) {
                int i;
                Tuple tuple = GetTuple(delete_table, current_block, byte_offset);
                for (i = 0; i < condition.size(); i++) {
                    if (!CheckCondition(delete_table, condition.get(i), tuple))
                        break;
                }
                if (i == condition.size()) {
                    current_block.WriteInt(0, byte_offset);
                    current_block.WriteInt(head_block.ReadInt(0), byte_offset + 1);
                    head_block.WriteInt(read_count, 0);
                    delete_count++;
                    Table tmp = CatalogManager.GetTable(delete_table);
                    for (Index index : tmp.index_list) {
                        int attr_index = CatalogManager.GetAttrIndex(delete_table, index.attr_name);
                        IndexManager.Delete(index, tuple.value_list.get(attr_index));
                    }
                }
                tuple_count++;
            }
            byte_offset += store_length;
            read_count++;
        }
        head_block.is_pinned = false;
        return delete_count;
    }

    public static int Delete(String delete_table, ArrayList<Address> address_list,
                             ArrayList<WhereCond> condition) throws SQLException {
        if (address_list.size() == 0) return 0;
        Collections.sort(address_list);
        int block_offset = 0, previous_offset = -1;
        int byte_offset = 0, tuple_offset, delete_count = 0;

        Block head_block = BufferManager.ReadBlock(delete_table, block_offset);
        Block current_block = head_block;
        if (head_block == null)
            throw new SQLException(EType.RuntimeError, 3443, "xxx");
        head_block.is_pinned = true;
        for (int i = 0; i < address_list.size(); i++) {
            block_offset = address_list.get(i).block_offset;
            byte_offset = address_list.get(i).byte_offset;
            tuple_offset = GetTupleOffset(delete_table, block_offset, byte_offset);
            if (i == 0 || block_offset != previous_offset) {
                current_block = BufferManager.ReadBlock(delete_table, block_offset);
                if (current_block == null) {
                    head_block.is_pinned = false;
                    return delete_count;
                }
            }
            if (current_block.ReadInt(byte_offset) < 0) {
                int j;
                Tuple tuple = GetTuple(delete_table, current_block, byte_offset);
                for (j = 0; j < condition.size(); j++)
                    if (!CheckCondition(delete_table, condition.get(j), tuple))
                        break;
                if (j == condition.size()) {
                    current_block.WriteInt(0, byte_offset);
                    current_block.WriteInt(head_block.ReadInt(0), byte_offset + 1);
                    head_block.WriteInt(tuple_offset, 0);
                    delete_count++;
                    Table tmp = CatalogManager.GetTable(delete_table);
                    for (Index index : tmp.index_list) {
                        int attr_index = CatalogManager.GetAttrIndex(delete_table, index.attr_name);
                        IndexManager.Delete(index, tuple.value_list.get(attr_index));
                    }
                }
            }
        }
        head_block.is_pinned = false;
        return delete_count;
    }


    //* RecordManager Utilities
    public static boolean CheckCondition(String table_name, WhereCond cond, Tuple data)
            throws SQLException {
        String value1, value2;
        DataType type1, type2;
        if (cond.is_expr1_attr) {
            int i = CatalogManager.GetAttrIndex(table_name, cond.expr1);
            value1 = data.value_list.get(i);
            type1 = CatalogManager.GetAttrType(table_name, i);
        } else {
            value1 = cond.expr1;
            type1 = cond.type1;
        }

        if (cond.is_expr2_attr) {
            int i = CatalogManager.GetAttrIndex(table_name, cond.expr2);
            value2 = data.value_list.get(i);
            type2 = CatalogManager.GetAttrType(table_name, i);
        } else {
            type2 = cond.type2;
            value2 = cond.expr2;
        }

        if (type1 == DataType.CHAR && type2 == DataType.CHAR) {
            switch (cond.cmp) {
                case EQUAL:
                    return value1.compareTo(value2) == 0;
                case NOT_EQUAL:
                    return value1.compareTo(value2) != 0;
                case LESS:
                    return value1.compareTo(value2) < 0;
                case GREATER:
                    return value1.compareTo(value2) > 0;
                case LESS_EQUAL:
                    return value1.compareTo(value2) <= 0;
                case GREATER_EQUAL:
                    return value1.compareTo(value2) >= 0;
            }
        } else if (type1 != DataType.CHAR && type2 != DataType.CHAR) {
            if (type1 == DataType.INT && type2 == DataType.INT) {
                int int1, int2;
                int1 = Integer.parseInt(value1);
                int2 = Integer.parseInt(value2);
                switch (cond.cmp) {
                    case EQUAL:
                        return int1 == int2;
                    case NOT_EQUAL:
                        return int1 != int2;
                    case LESS:
                        return int1 < int2;
                    case GREATER:
                        return int1 > int2;
                    case LESS_EQUAL:
                        return int1 <= int2;
                    case GREATER_EQUAL:
                        return int1 >= int2;
                }
            } else if (type1 == DataType.FLOAT && type2 == DataType.FLOAT) {
                double double1, double2;
                double1 = Double.parseDouble(value1);
                double2 = Double.parseDouble(value2);
                switch (cond.cmp) {
                    case EQUAL:
                        return double1 == double2;
                    case NOT_EQUAL:
                        return double1 != double2;
                    case LESS:
                        return double1 < double2;
                    case GREATER:
                        return double1 > double2;
                    case LESS_EQUAL:
                        return double1 <= double2;
                    case GREATER_EQUAL:
                        return double1 >= double2;
                }
            } else if (type1 == DataType.INT && type2 == DataType.FLOAT) {
                int int1;
                double double2;
                int1 = Integer.parseInt(value1);
                double2 = Double.parseDouble(value2);
                switch (cond.cmp) {
                    case EQUAL:
                        return int1 == double2;
                    case NOT_EQUAL:
                        return int1 != double2;
                    case LESS:
                        return int1 < double2;
                    case GREATER:
                        return int1 > double2;
                    case LESS_EQUAL:
                        return int1 <= double2;
                    case GREATER_EQUAL:
                        return int1 >= double2;
                }
            } else {
                int int2;
                double double1;
                double1 = Double.parseDouble(value1);
                int2 = Integer.parseInt(value2);
                switch (cond.cmp) {
                    case EQUAL:
                        return double1 == int2;
                    case NOT_EQUAL:
                        return double1 != int2;
                    case LESS:
                        return double1 < int2;
                    case GREATER:
                        return double1 > int2;
                    case LESS_EQUAL:
                        return double1 <= int2;
                    case GREATER_EQUAL:
                        return double1 >= int2;
                }
            }
        }
        if (cond.cmp == CMP.NOT_EQUAL)
            return true;
        throw new SQLException(EType.RuntimeError, 0, "cannot compare");
    }


    public static Tuple GetTuple(String table_name, Block block, int offset) {
        int attr_num = CatalogManager.GetAttrNum(table_name);
        String attr_value = null;
        Tuple rst = new Tuple();

        offset++;
        for (int i = 0; i < attr_num; i++) {
            int length = CatalogManager.GetAttrLength(table_name, i);
            DataType type = CatalogManager.GetAttrType(table_name, i);
            switch (type) {
                case INT:
                    attr_value = String.valueOf(block.ReadInt(offset));
                    break;
                case FLOAT:
                    attr_value = String.valueOf(block.ReadFloat(offset));
                    break;
                case CHAR:
                    int zero;
                    attr_value = block.ReadChar(length, offset);
                    zero = attr_value.indexOf(0);
                    zero = zero == -1 ? attr_value.length() : zero;
                    attr_value = attr_value.substring(0, zero);
                    break;
            }
            offset += length;
            rst.value_list.add(attr_value);
        }
        return rst;
    }

    public static int GetBlockOffset(String table_name, int tuple_offset) {
        int store_length = CatalogManager.GetStoreLength(table_name);
        int tuple_num_first = (DefaultSetting.BLOCK_SIZE - DefaultSetting.INT_SIZE) / store_length;
        int tuple_num_next = DefaultSetting.BLOCK_SIZE / store_length;

        if (tuple_offset < tuple_num_first) {
            return 0;
        } else {
            return (tuple_offset - tuple_num_first) / tuple_num_next + 1;
        }
    }

    public static int GetByteOffset(String table_name, int tuple_offset) {
        int store_length = CatalogManager.GetStoreLength(table_name);
        int tuple_num_first = (DefaultSetting.BLOCK_SIZE - DefaultSetting.INT_SIZE) / store_length;
        int tuple_num_next = DefaultSetting.BLOCK_SIZE / store_length;
        int block_offset = GetBlockOffset(table_name, tuple_offset);

        if (block_offset == 0) {
            return tuple_offset * store_length + DefaultSetting.INT_SIZE;
        } else {
            return (tuple_offset - tuple_num_first - (block_offset - 1) * tuple_num_next) * store_length;
        }
    }

    public static int GetTupleOffset(String table_name, int block_offset, int byte_offset) {
        int store_length = CatalogManager.GetStoreLength(table_name);
        int tuple_num_first = (DefaultSetting.BLOCK_SIZE - DefaultSetting.INT_SIZE) / store_length;
        int tuple_num_next = DefaultSetting.BLOCK_SIZE / store_length;
        if (block_offset == 0)
            return (byte_offset - DefaultSetting.INT_SIZE) / store_length;
        else
            return tuple_num_first + (block_offset - 1) * tuple_num_next + byte_offset / store_length;
    }

    public static void WriteTuple(
            String table_name, ArrayList<InsertVal> value, Block block, int offset) {
        int attr_num = CatalogManager.GetAttrNum(table_name);
        block.WriteInt(-1, offset);
        offset++;
        for (int i = 0; i < attr_num; i++) {
            int length = CatalogManager.GetAttrLength(table_name, i);
            DataType type = CatalogManager.GetAttrType(table_name, i);
            switch (type) {
                case INT:
                    block.WriteInt(Integer.parseInt(value.get(i).val), offset);
                    break;
                case FLOAT:
                    block.WriteFloat(Double.parseDouble(value.get(i).val), offset);
                    break;
                case CHAR:
                    byte[] reset = new byte[length];
                    Arrays.fill(reset, (byte) 0);
                    block.WriteData(reset, offset);
                    block.WriteChar(value.get(i).val, offset);
                    break;
            }
            offset += length;
        }
    }

}
