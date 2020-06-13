package IndexManager;

import java.util.ArrayList;
import java.util.*;

public class LeafNode<K extends Comparable<K>, V> extends Node<K, V> {
    protected ArrayList<V> value_list;
    protected LeafNode<K, V> next_leaf;
    protected LeafNode<K, V> previous_leaf;

    public LeafNode(K key, V value) {
        is_leaf_node = true;
        key_list = new ArrayList<K>();
        value_list = new ArrayList<V>();
        key_list.add(key);
        value_list.add(value);
    }

    public LeafNode(List<K> keys, List<V> values) {
        is_leaf_node = true;
        key_list = new ArrayList<K>(keys);
        value_list = new ArrayList<V>(values);
    }

    public void Insert(K key, V value) {
        if (key.compareTo(key_list.get(0)) < 0) {
            key_list.add(0, key);
            value_list.add(0, value);
        } else if (key.compareTo(key_list.get(key_list.size() - 1)) > 0) {
            key_list.add(key);
            value_list.add(value);
        } else {
            ListIterator<K> iterator = key_list.listIterator();
            while (iterator.hasNext()) {
                if (iterator.next().compareTo(key) > 0) {
                    int position = iterator.previousIndex();
                    key_list.add(position, key);
                    value_list.add(position, value);
                    break;
                }
            }
        }
    }
}
