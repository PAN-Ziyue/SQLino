package Utils;

import Data.DataType;

import java.util.HashSet;
import java.util.regex.Pattern;

public class CommonUtils {

    private static final HashSet<String> reserved_words = new HashSet<String>() {{
        add("SELECT");
        add("INSERT");
        add("INTO");
        add("VALUES");
        add("QUIT");
        add("CREATE");
        add("FROM");
        add("EXECFILE");
        add("DROP");
        add("DELETE");
        add("UNIQUE");
        add("WHERE");
        add("INT");
        add("FLOAT");
        add("AND");
        add("ON");
        add("KEY");
        add("INDEX");
        add("TABLE");
        add("PRIMARY");
    }};

    public static boolean IsReservedWord(String token) {
        return reserved_words.contains(token);
    }


    public static boolean IsLegalExpr(String token) throws SQLException {
        return IsLegalName(token)
                || IsInteger(token)
                || IsFloat(token)
                || IsString(token);
    }

    public static boolean IsLegalName(String token) throws SQLException {
        if (reserved_words.contains(token))
            throw new SQLException(EType.SyntaxError, 0, "invalid keyword, identifier cannot be reserved words");
        return Pattern.matches("^[a-zA-Z_][a-zA-Z0-9_]*$", token);
    }


    public static boolean IsInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean IsString(String s) {
        return s.charAt(0) == '\\';
    }

    public static boolean IsFloat(String s) {
        try {
            Float.parseFloat(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static DataType InferData(String value) {
        if (IsInteger(value))
            return DataType.INT;
        else if(IsFloat(value))
            return DataType.FLOAT;
        else
            return DataType.CHAR;
    }

    public static String ParseString(String token) {
        String[] char_lists = token.split("\\\\");
        StringBuilder rst = new StringBuilder();
        for (String char_num : char_lists) {
            if (char_num.equals(""))
                continue;
            int num = Integer.parseInt(char_num, 16);
            rst.append((char) num);
        }
        return rst.toString();
    }

}

