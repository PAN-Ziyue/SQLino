package Data;

import java.io.Serializable;

public class Index  implements Serializable {
    public String name;
    public String table_name;
    public String attr_name;

    public int root_num;
    public int block_num = 0;

    public Index(String index_name, String table_name,
                 String attr_name, int block_num, int root_num) {
        name = index_name;
        this.table_name = table_name;
        this.attr_name = attr_name;
        this.block_num = block_num;
        this.root_num = root_num;
    }

    public Index(String index_name,
                 String table_name, String attr_name) {
        this.name = index_name;
        this.table_name = table_name;
        this.attr_name = attr_name;
    }
}
