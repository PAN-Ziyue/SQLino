package IndexManager;

import java.util.*;
import java.util.Map.Entry;

public class IndexNode<K extends Comparable<K>, V> extends Node<K, V> {
    protected ArrayList<Node<K, V>> children;

    public IndexNode(K key, Node<K, V> child0, Node<K, V> child1) {
        is_leaf_node = false;
        key_list = new ArrayList<K>();
        key_list.add(key);
        children = new ArrayList<Node<K, V>>();
        children.add(child0);
        children.add(child1);
    }

    public IndexNode(List<K> keys, List<Node<K, V>> children) {
        is_leaf_node = false;
        key_list = new ArrayList<K>(keys);
        this.children = new ArrayList<Node<K, V>>(children);
    }

    public void Insert(Entry<K, Node<K, V>> entry, int index) {
        K key = entry.getKey();
        Node<K, V> child = entry.getValue();
        if (index >= key_list.size()) {
            key_list.add(key);
            children.add(child);
        } else {
            key_list.add(index, key);
            children.add(index + 1, child);
        }
    }
}
