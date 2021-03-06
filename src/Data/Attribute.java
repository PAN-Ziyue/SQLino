package Data;

import Utils.DefaultSetting;

import java.io.Serializable;

public class Attribute implements Serializable {
    public String name;
    public DataType type;
    public boolean unique;
    public boolean primary;
    public int char_length;

    public int GetLength() {
        switch (type) {
            case CHAR:
                return char_length * DefaultSetting.CHAR_SIZE;
            case INT:
                return DefaultSetting.INT_SIZE;
            case FLOAT:
                return DefaultSetting.FLOAT_SIZE;
            default:
                return 0;
        }
    }

    public Attribute() {
        Clear();
    }

    public void Clear() {
        name = "";
        type = DataType.INT;
        unique = primary = false;
        char_length = 1;
    }
}
