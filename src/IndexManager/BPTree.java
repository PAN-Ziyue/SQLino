package IndexManager;

import java.io.Serializable;
import java.util.ArrayList;

public class BPTree<K extends Comparable<? super K>, V> implements Serializable {
    static abstract class Node<K extends  Comparable<? super K>, V> {
        protected InternalNode<K, V> parent;
        protected int order;
        protected int count;
        protected K[] keys;

        public Node(int order) {
            this.order = order;
            count = 0;
            parent = null;
            keys = (K[]) new Comparable[order];
        }

        public abstract V Find(K key);

        public abstract ArrayList<K>
    }

    static class InternalNode<K extends Comparable<? super K>, V> extends Node<K, V> {
        public InternalNode(int order) {
            super(order);
            this.children = new Node[order + 1];
        }

        protected Node<K, V>[] children;

    }

}
