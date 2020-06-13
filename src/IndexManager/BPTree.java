package IndexManager;

import java.util.ArrayList;
import java.util.Map.Entry;

public class BPTree<K extends Comparable<K>, V> {

    public BPTree() {
        
    }


    public Node<K, V> root;



    public V Search(K key) {

    }

    public Node<K, V> Search(Node<K, V> node, K key) {

    }

    public void Insert(K key, V value) {
    }

    public void Delete(K key) {
    }

    public int HandleLeafNodeUnderflow(
            LeafNode<K, V> left, LeafNode<K, V> right, IndexNode<K, V> parent) {

    }

    public int HandleIndexNodeUnderflow(
            IndexNode<K, V> left, IndexNode<K, V> right, IndexNode<K, V> parent) {

    }

    public Entry<K, Node<K, V>> SplitIndexNode(IndexNode<K, V> index) {

    }

    public Entry<K, Node<K, V>> SplitLeafNode(LeafNode<K, V> leaf) {

    }

    private Entry<K, Node<K, V>> GetChildEntry(
            Node<K, V> node, Entry<K, Node<K, V>> entry, Entry<K, Node<K, V>> new_entry) {
        if(!node.is_leaf_node) {

        } else {

        }
    }

    private Entry<K, Node<K, V>> DeleteChildEntry(
            Node<K, V> parrent_node, Entry<K, Node<K, V>> entry, Entry<K, Node<K, V>> old_entry) {


    }


}
