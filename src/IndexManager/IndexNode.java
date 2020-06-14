package IndexManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class IndexNode<K extends Comparable<K>, V> extends Node<K, V> implements Serializable {
	protected ArrayList<Node<K, V>> children;

	public IndexNode(K key, Node<K, V> child0, Node<K, V> child1) {
		is_leaf = false;
		key_list = new ArrayList<K>();
		key_list.add(key);
		children = new ArrayList<Node<K, V>>();
		children.add(child0);
		children.add(child1);
	}

	public IndexNode(List<K> new_key_list, List<Node<K, V>> new_children) {
		is_leaf = false;
		key_list = new ArrayList<K>(new_key_list);
		children = new ArrayList<Node<K, V>>(new_children);
	}

	public void Insert(Entry<K, Node<K, V>> e, int index) {
		K key = e.getKey();
		Node<K, V> child = e.getValue();
		if (index >= key_list.size()) {
			key_list.add(key);
			children.add(child);
		} else {
			key_list.add(index, key);
			children.add(index+1, child);
		}
	}

}
