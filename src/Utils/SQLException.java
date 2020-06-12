package Utils;
public class SQLException extends Exception {
    public int error_code;  // error code
    public String msg;      // error message
    public EType type;  // error type [syntax error | runtime error]

    public SQLException(EType type, int error_code, String msg) {
        this.type = type;
        this.error_code = error_code;
        this.msg = msg;
    }

    @Override
    public String getMessage() {
        return "[" + type.name() + ": " + error_code + "] " + msg;
    }

    public void PromptMsg() {
        System.out.println("[" + type.name() + ": " + error_code + "] " + msg);
    }
}
