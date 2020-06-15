package Data;

import java.util.ArrayList;

public class Tuple {

    public ArrayList<String> value_list;

    public Tuple() {
        value_list = new ArrayList<>();
    }

    public Tuple(ArrayList<String> value_list) {
        this.value_list = new ArrayList<>(value_list);
    }

    public String GetValue(int index) {
        return value_list.get(index);
    }

}
