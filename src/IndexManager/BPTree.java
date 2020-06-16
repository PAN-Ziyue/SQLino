package IndexManager;

import Data.CMP;
import Utils.DefaultSetting;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map.Entry;

public class BPTree<K extends Comparable<K>, V> implements Serializable {

    public Node<K, V> root;
    public static final int D = DefaultSetting.BP_ORDER;

    public ArrayList<V> Search(K key, CMP cmp) {
        switch (cmp) {
            case EQUAL:
                return Search_EQUAL(key);
            case NOT_EQUAL:
                return Search_NOT_EQUAL(key);
            case GREATER:
                return Search_GREATER(key);
            case LESS:
                return Search_LESS(key);
            case GREATER_EQUAL:
                return Search_GREATER_EQUAL(key);
            case LESS_EQUAL:
                return Search_LESS_EQUAL(key);
        }
        return null;
    }

    private ArrayList<V> Search_EQUAL(K key) {
        ArrayList<V> res = new ArrayList<>();
        if (key == null || root == null) return res;

        LeafNode<K, V> leaf = (LeafNode<K, V>) Search(root, key);
        if (leaf != null) {
            for (int i = 0; i < leaf.key_list.size(); i++) {
                if (key.compareTo(leaf.key_list.get(i)) == 0) {
                    V tmp_value = leaf.value_list.get(i);
                    res.add(tmp_value);
                    break;
                }
            }
        }
        return res;
    }

    private ArrayList<V> Search_NOT_EQUAL(K key) {
        ArrayList<V> less = Search_LESS(key);
        ArrayList<V> greater = Search_GREATER(key);
        less.addAll(greater);
        return less;
    }

    private ArrayList<V> Search_GREATER(K key) {
        ArrayList<V> res = new ArrayList<>();
        if (key == null || root == null) return res;
        LeafNode<K, V> leaf = (LeafNode<K, V>) Search(root, key);
        if (leaf != null) {
            while (leaf != null) {
                for (int i = 0; i < leaf.key_list.size(); i++) {
                    if (key.compareTo(leaf.key_list.get(i)) < 0) {
                        V tmp_value = leaf.value_list.get(i);
                        res.add(tmp_value);
                    }
                }
                leaf = leaf.next_leaf;
            }
        }
        return res;
    }

    private ArrayList<V> Search_LESS(K key) {
        ArrayList<V> res = new ArrayList<>();
        if (key == null || root == null) return res;
        LeafNode<K, V> leaf = (LeafNode<K, V>) Search(root, key);
        if (leaf != null) {
            while (leaf != null) {
                for (int i = 0; i < leaf.key_list.size(); i++) {
                    if (key.compareTo(leaf.key_list.get(i)) > 0) {
                        V tmp_value = leaf.value_list.get(i);
                        res.add(tmp_value);
                    }
                }
                leaf = leaf.previous_leaf;
            }
        }
        return res;
    }

    private ArrayList<V> Search_GREATER_EQUAL(K key) {
        ArrayList<V> res = Search_EQUAL(key);
        if (res.size() == 0)
            return Search_GREATER(key);
        ArrayList<V> greater = Search_GREATER(key);
        greater.add(0, res.get(0));
        return greater;
    }

    private ArrayList<V> Search_LESS_EQUAL(K key) {
        ArrayList<V> res = Search_EQUAL(key);
        if (res.size() == 0)
            return Search_LESS(key);
        ArrayList<V> less = Search_LESS(key);
        less.add(res.get(0));
        return less;
    }

    private Node<K, V> Search(Node<K, V> node, K key) {
        if (node.is_leaf) return node;

        IndexNode<K, V> index = (IndexNode<K, V>) node;
        if (key.compareTo(index.key_list.get(0)) < 0) {
            return Search(index.children.get(0), key);
        } else if (key.compareTo(index.key_list.get(node.key_list.size() - 1)) >= 0) {
            return Search(index.children.get(index.children.size() - 1), key);
        } else {
            for (int i = 0; i < index.key_list.size() - 1; i++) {
                if (key.compareTo(index.key_list.get(i)) >= 0
                        && key.compareTo(index.key_list.get(i + 1)) < 0) {
                    return Search(index.children.get(i + 1), key);
                }
            }
        }
        return null;
    }

    public void Insert(K key, V value) {
        LeafNode<K, V> newLeaf = new LeafNode<>(key, value);
        Entry<K, Node<K, V>> entry = new AbstractMap.SimpleEntry<>(key, newLeaf);
        if (root == null || root.key_list.size() == 0) {
            root = entry.getValue();
        }
        Entry<K, Node<K, V>> new_entry = GetChildEntry(root, entry, null);
        if (new_entry != null) {
            root = new IndexNode<K, V>(new_entry.getKey(), root, new_entry.getValue());
        }
    }

    private Entry<K, Node<K, V>> GetChildEntry(
            Node<K, V> node, Entry<K, Node<K, V>> entry, Entry<K, Node<K, V>> new_entry) {
        if (!node.is_leaf) {
            IndexNode<K, V> index = (IndexNode<K, V>) node;
            int i;
            for (i = 0; i < index.key_list.size(); i++)
                if (entry.getKey().compareTo(index.key_list.get(i)) < 0)
                    break;
            new_entry = GetChildEntry(index.children.get(i), entry, new_entry);

            if (new_entry == null) return null;

            for (i = 0; i < index.key_list.size(); i++)
                if (new_entry.getKey().compareTo(index.key_list.get(i)) < 0)
                    break;

            index.Insert(new_entry, i);
            if (!index.IsOverflow()) return null;
            new_entry = SplitIndexNode(index);
            if (index == root) {
                root = new IndexNode<K, V>(new_entry.getKey(), root, new_entry.getValue());
                return null;
            }
        } else {
            LeafNode<K, V> leaf = (LeafNode<K, V>) node;
            LeafNode<K, V> new_leaf = (LeafNode<K, V>) entry.getValue();

            leaf.Insert(entry.getKey(), new_leaf.value_list.get(0));
            if (!leaf.IsOverflow()) return null;
            new_entry = SplitLeafNode(leaf);
            if (leaf == root) {
                root = new IndexNode<K, V>(new_entry.getKey(), leaf, new_entry.getValue());
                return null;
            }
        }
        return new_entry;
    }

    public Entry<K, Node<K, V>> SplitLeafNode(LeafNode<K, V> leaf) {
        ArrayList<K> new_key_list = new ArrayList<>();
        ArrayList<V> new_value_list = new ArrayList<>();

        for (int i = D; i <= 2 * D; i++) {
            new_key_list.add(leaf.key_list.get(i));
            new_value_list.add(leaf.value_list.get(i));
        }
        for (int i = D; i <= 2 * D; i++) {
            leaf.key_list.remove(leaf.key_list.size() - 1);
            leaf.value_list.remove(leaf.value_list.size() - 1);
        }

        K split_key = new_key_list.get(0);
        LeafNode<K, V> right_node = new LeafNode<>(new_key_list, new_value_list);

        LeafNode<K, V> tmp = leaf.next_leaf;
        leaf.next_leaf = right_node;
        leaf.next_leaf.previous_leaf = right_node;
        right_node.previous_leaf = leaf;
        right_node.next_leaf = tmp;

        return new AbstractMap.SimpleEntry<>(split_key, right_node);
    }

    public Entry<K, Node<K, V>> SplitIndexNode(IndexNode<K, V> index) {
        ArrayList<K> new_key_list = new ArrayList<>();
        ArrayList<Node<K, V>> new_children = new ArrayList<>();

        K split_key = index.key_list.get(D);
        index.key_list.remove(D);
        new_children.add(index.children.get(D + 1));
        index.children.remove(D + 1);

        while (index.key_list.size() > D) {
            new_key_list.add(index.key_list.get(D));
            index.key_list.remove(D);
            new_children.add(index.children.get(D + 1));
            index.children.remove(D + 1);
        }

        IndexNode<K, V> right_node = new IndexNode<K, V>(new_key_list, new_children);
        return new AbstractMap.SimpleEntry<>(split_key, right_node);
    }

    public void Delete(K key) {
        if (key == null || root == null) return;

        LeafNode<K, V> leaf = (LeafNode<K, V>) Search(root, key);
        if (leaf == null) return;

        Entry<K, Node<K, V>> entry = new AbstractMap.SimpleEntry<>(key, leaf);
        Entry<K, Node<K, V>> old_entry = DeleteChildEntry(root, root, entry, null);
        if (old_entry == null) {
            if (root.key_list.size() == 0) {
                if (!root.is_leaf) {
                    root = ((IndexNode<K, V>) root).children.get(0);
                }
            }
        } else {
            int i = 0;
            K oldKey = old_entry.getKey();
            while (i < root.key_list.size()) {
                if (oldKey.compareTo(root.key_list.get(i)) == 0) {
                    break;
                }
                i++;
            }
            if (i == root.key_list.size()) return;
            root.key_list.remove(i);
            ((IndexNode<K, V>) root).children.remove(i + 1);
        }
    }

    private Entry<K, Node<K, V>> DeleteChildEntry(
            Node<K, V> parent_node, Node<K, V> node,
            Entry<K, Node<K, V>> entry, Entry<K, Node<K, V>> old_entry) {
        if (!node.is_leaf) {
            IndexNode<K, V> index = (IndexNode<K, V>) node;
            K entry_key = entry.getKey();
            int i;
            for (i = 0; i < index.key_list.size(); i++)
                if (entry_key.compareTo(index.key_list.get(i)) < 0)
                    break;

            old_entry = DeleteChildEntry(index, index.children.get(i), entry, old_entry);
            if (old_entry == null) {
                return null;
            } else {
                K old_key = old_entry.getKey();
                for (i = 0; i < index.key_list.size(); i++)
                    if (old_key.compareTo(index.key_list.get(i)) == 0)
                        break;

                index.key_list.remove(i);
                index.children.remove(i + 1);
                if (!index.IsUnderflow() || index.key_list.size() == 0) return null;
                if (index == root) return old_entry;

                K firstKey = index.key_list.get(0);
                for (i = 0; i < parent_node.key_list.size(); i++)
                    if (firstKey.compareTo(parent_node.key_list.get(i)) < 0)
                        break;

                int split_key_pos;
                IndexNode<K, V> parent = (IndexNode<K, V>) parent_node;

                if (i > 0 && parent.children.get(i - 1) != null) {
                    split_key_pos = HandleIndexNode(
                            (IndexNode<K, V>) parent.children.get(i - 1), index, parent);
                } else {
                    split_key_pos = HandleIndexNode(
                            index, (IndexNode<K, V>) parent.children.get(i + 1), parent);
                }
                if (split_key_pos == -1) return null;

                K parentKey = parent_node.key_list.get(split_key_pos);
                old_entry = new AbstractMap.SimpleEntry<>(parentKey, parent_node);
                return old_entry;
            }
        } else {
            LeafNode<K, V> leaf = (LeafNode<K, V>) node;
            for (int i = 0; i < leaf.key_list.size(); i++) {
                if (leaf.key_list.get(i) == entry.getKey()) {
                    leaf.key_list.remove(i);
                    leaf.value_list.remove(i);
                    break;
                }
            }
            if (!leaf.IsUnderflow()) return null;

            if (leaf == root || leaf.key_list.size() == 0) {
                return old_entry;
            }
            int split_key_pos;
            K first_key = leaf.key_list.get(0);
            K parent_key = parent_node.key_list.get(0);

            if (leaf.previous_leaf != null && first_key.compareTo(parent_key) >= 0)
                split_key_pos = HandleLeafNode(leaf.previous_leaf, leaf, (IndexNode<K, V>) parent_node);
            else
                split_key_pos = HandleLeafNode(leaf, leaf.next_leaf, (IndexNode<K, V>) parent_node);

            if (split_key_pos == -1) return null;
            parent_key = parent_node.key_list.get(split_key_pos);
            old_entry = new AbstractMap.SimpleEntry<>(parent_key, parent_node);
            return old_entry;
        }
    }

    public int HandleLeafNode(LeafNode<K, V> left, LeafNode<K, V> right,
                              IndexNode<K, V> parent) {
        int i;
        K right_key = right.key_list.get(0);
        for (i = 0; i < parent.key_list.size(); i++)
            if (right_key.compareTo(parent.key_list.get(i)) < 0)
                break;

        if (left.key_list.size() + right.key_list.size() >= 2 * D) {
            if (left.key_list.size() > right.key_list.size()) {
                while (left.key_list.size() > D) {
                    right.key_list.add(0, left.key_list.get(left.key_list.size() - 1));
                    right.value_list.add(0, left.value_list.get(left.key_list.size() - 1));
                    left.key_list.remove(left.key_list.size() - 1);
                    left.value_list.remove(left.value_list.size() - 1);
                }
            } else {
                while (left.key_list.size() < D) {
                    left.key_list.add(right.key_list.get(0));
                    left.value_list.add(right.value_list.get(0));
                    right.key_list.remove(0);
                    right.value_list.remove(0);
                }
            }

            parent.key_list.set(i - 1, right.key_list.get(0));
            return -1;
        } else {
            while (right.key_list.size() > 0) {
                left.key_list.add(right.key_list.get(0));
                left.value_list.add(right.value_list.get(0));
                right.key_list.remove(0);
                right.value_list.remove(0);
            }
            if (right.next_leaf != null) {
                right.next_leaf.previous_leaf = left;
            }
            left.next_leaf = right.next_leaf;
            return i - 1;
        }
    }

    public int HandleIndexNode(IndexNode<K, V> leftIndex,
                               IndexNode<K, V> rightIndex, IndexNode<K, V> parent) {
        int i;
        K right_key = rightIndex.key_list.get(0);
        for (i = 0; i < parent.key_list.size(); i++)
            if (right_key.compareTo(parent.key_list.get(i)) < 0)
                break;

        if (leftIndex.key_list.size() + rightIndex.key_list.size() >= 2 * D) {
            if (leftIndex.key_list.size() > rightIndex.key_list.size()) {
                while (leftIndex.key_list.size() > D) {
                    rightIndex.key_list.add(0, parent.key_list.get(i - 1));
                    rightIndex.children.add(leftIndex.children.get(leftIndex.children.size() - 1));
                    parent.key_list.set(i - 1, leftIndex.key_list.get(leftIndex.key_list.size() - 1));
                    leftIndex.key_list.remove(leftIndex.key_list.size() - 1);
                    leftIndex.children.remove(leftIndex.children.size() - 1);
                }
            } else {
                while (leftIndex.key_list.size() < D) {
                    leftIndex.key_list.add(parent.key_list.get(i - 1));
                    leftIndex.children.add(rightIndex.children.get(0));
                    parent.key_list.set(i - 1, rightIndex.key_list.get(0));
                    rightIndex.key_list.remove(0);
                    rightIndex.children.remove(0);
                }
            }
            return -1;
        } else {
            leftIndex.key_list.add(parent.key_list.get(i - 1));
            while (rightIndex.key_list.size() > 0) {
                leftIndex.key_list.add(rightIndex.key_list.get(0));
                leftIndex.children.add(rightIndex.children.get(0));
                rightIndex.key_list.remove(0);
                rightIndex.children.remove(0);
            }
            leftIndex.children.add(rightIndex.children.get(0));
            rightIndex.children.remove(0);

            return i - 1;
        }
    }
}