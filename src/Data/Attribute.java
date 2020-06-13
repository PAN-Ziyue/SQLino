package Data;

public class Attribute {
    public String name;
    public DataType type;
    public boolean unique;
    public boolean primary;
    public int length;


    public Attribute() {
        Clear();
    }

    public void Clear() {
        name = "";
        type = DataType.INT;
        unique = primary = false;
        length = 1;
    }
}
