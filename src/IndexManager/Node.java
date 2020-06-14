package IndexManager;

import java.io.Serializable;
import java.util.ArrayList;

public class Node<K extends Comparable<K>, V>implements Serializable {
    protected boolean is_leaf;
    protected ArrayList<K> key_list;

    public boolean IsOverflow() {
        return key_list.size() > 2 * BPTree.D;
    }

    public boolean IsUnderflow() {
        return key_list.size() < BPTree.D;
    }
}
