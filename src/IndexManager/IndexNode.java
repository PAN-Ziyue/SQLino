package IndexManager;

import java.util.ArrayList;

public class IndexNode<K extends Comparable<K>, V> extends Node<K, V> {
    protected ArrayList<Node<K, V>> children;
    
}
