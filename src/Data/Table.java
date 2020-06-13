package Data;

import java.util.ArrayList;

public class Table {
    public String name;
    public String primary_attr;
    public ArrayList<Attribute> attr_list;
    public ArrayList<Index> index_list;


    public int index_num;
    public int attr_num;
    public int row_num;

    public Table(String name, String primary_attr, ArrayList<Attribute>attr_list) {
        this.name = name;
        this.primary_attr = primary_attr;
        this.attr_list = attr_list;
        this.index_list = new ArrayList<Index>();
        this.index_num = 0;
        this.attr_num = attr_list.size();
        this.row_num = 0;
    }

    public Table(String name, String primary_attr, ArrayList<Attribute>attr_list,
                 ArrayList<Index>index_list, int row_num) {
        this.name = name;
        this.primary_attr = primary_attr;
        this.attr_list = attr_list;
        this.index_list = index_list;
        this.index_num = index_list.size();
        this.attr_num = attr_list.size();
        this.row_num = row_num;
    }
}