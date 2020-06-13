package IndexManager;

import Utils.DefaultSetting;

import java.util.ArrayList;

public class Node<K extends Comparable<K>, V> {
    protected boolean is_leaf_node;
    protected ArrayList<K> key_list;

    public boolean IsOverFlow() {
        return key_list.size() > 2 * DefaultSetting.BP_ORDER;
    }

    public boolean IsUnderFlow() {
        return key_list.size() < DefaultSetting.BP_ORDER;
    }

}
