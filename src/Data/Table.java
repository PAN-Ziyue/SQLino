package Data;

import java.io.Serializable;
import java.util.ArrayList;

public class Table implements Serializable {
    public String name;
    public String primary_attr;
    public ArrayList<Attribute> attr_list;
    public ArrayList<Index> index_list;


    public boolean has_primary;

    public int index_num;
    public int attr_num;
    public int row_num;
    public int row_length;

    public Table(String name, ArrayList<Attribute> attr_list) {
        this.name = name;
        primary_attr = "";
        has_primary = false;
        this.attr_list = attr_list;
        this.index_list = new ArrayList<Index>();
        this.index_num = 0;
        this.attr_num = attr_list.size();
        this.row_num = 0;
        row_length = 0;
        for (Attribute attr : attr_list) {
            row_length += attr.GetLength();
        }
    }


    public Table(String name, ArrayList<Attribute> attr_list, String primary_attr) {
        this.name = name;
        this.primary_attr = primary_attr;
        this.attr_list = attr_list;
        this.index_list = new ArrayList<Index>();
        this.index_num = 0;
        this.attr_num = attr_list.size();
        this.row_num = 0;
        has_primary = true;
        row_length = 0;
        for (Attribute attr : attr_list) {
            if(attr.name.equals(primary_attr))
                attr.unique = true;
            row_length += attr.GetLength();
        }
    }

    public Table(String name, String primary_attr, ArrayList<Attribute> attr_list,
                 ArrayList<Index> index_list, int row_num) {
        this.name = name;
        this.primary_attr = primary_attr;
        this.attr_list = attr_list;
        this.index_list = index_list;
        this.index_num = index_list.size();
        this.attr_num = attr_list.size();
        this.row_num = row_num;
        has_primary = true;
    }
}
