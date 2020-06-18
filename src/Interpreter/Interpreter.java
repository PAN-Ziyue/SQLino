package Interpreter;

import API.API;
import Data.*;
import Utils.CommonUtils;
import Utils.DefaultSetting;
import Utils.EType;
import Utils.SQLException;

public class Interpreter {
    private static State state_code = State.IDLE;
    public static boolean parse_script = false;

    //* set the interpreter state
    public static void SetState(State set_state_code) {
        state_code = set_state_code;
    }

    //* spectate the interpreter state
    public static State GetState() {
        return state_code;
    }

    //* preprocess input string, make it easier to be split by blank spaces
    public static String ProcessInput(String input_string) throws SQLException {
        StringBuilder rst = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < input_string.length(); i++) {
            if (input_string.charAt(i) == '\'' && !quoted) {
                quoted = true;
            } else if (input_string.charAt(i) == '\'' && quoted) {
                quoted = false;
            } else if (input_string.charAt(i) == '\\' && quoted) {
                switch (input_string.charAt(i + 1)) {
                    case '\\':
                        rst.append("\\5c");
                        break;
                    case 'n':
                        rst.append("\\a");
                        break;
                    case 'r':
                        rst.append("\\d");
                        break;
                    case 't':
                        rst.append("\\9");
                        break;
                    case '\'':
                        rst.append("\\27");
                        break;
                    default: {
                        throw new SQLException(EType.SyntaxError, 4,
                                "invalid escape character: \\" + input_string.charAt(i + 1));
                    }
                }
                i++;
            } else if (input_string.charAt(i) == '\\' && !quoted) {
                throw new SQLException(EType.SyntaxError, 5, "redundant back slash");
            } else if (quoted) {
                String hex = String.format("%x", (int) input_string.charAt(i));
                rst.append('\\').append(hex);
            } else {
                rst.append(input_string.charAt(i));
            }
        }
        if (quoted) {
            throw new SQLException(EType.SyntaxError, 6, "unquoted string");
        }

        for (int i = 0; i < rst.length(); i++) {
            if (rst.charAt(i) == '(' || rst.charAt(i) == ')' || rst.charAt(i) == ';' || rst.charAt(i) == ',') {
                rst.insert(i, " ");
                i += 2;
                rst.insert(i, " ");
            } else if (rst.charAt(i) == '=') {
                rst.insert(i, " ");
                i += 2;
                rst.insert(i, " ");
            } else if (rst.charAt(i) == '!' && rst.charAt(i + 1) == '=') {
                rst.insert(i, " ");
                i += 3;
                rst.insert(i, " ");
            } else if ((rst.charAt(i) == '>' || rst.charAt(i) == '<')) {
                if (rst.charAt(i + 1) == '=') {
                    rst.insert(i, " ");
                    i += 3;
                    rst.insert(i, " ");
                } else {
                    rst.insert(i, " ");
                    i += 2;
                    rst.insert(i, " ");
                }
            }
        }
        return rst.toString();
    }

    //* interpreter main method, get the input string and parse the token stream
    public static void ReadInput(String input_string) throws SQLException {
        String[] tokens = input_string.split("\\s+");
        for (String token : tokens) {
            String upper_token = token.toUpperCase();
            if (CommonUtils.IsReservedWord(upper_token)) {
                token = upper_token;
            }
            if (token.equals(""))
                continue;
            switch (state_code) {
                case IDLE: {
                    if (token.equals("SELECT"))
                        state_code = State.SELECT;
                    else if (token.equals("QUIT")) {
                        if (parse_script) {
                            throw new SQLException(EType.RuntimeError, 1,
                                    "QUIT instruction cannot be executed in script");
                        } else {
                            state_code = State.QUIT;
                            return;
                        }
                    } else if (token.equals("CREATE")) {
                        state_code = State.CREATE;
                    } else if (token.equals("INSERT")) {
                        state_code = State.INSERT;
                    } else if (token.equals("EXECFILE")) {
                        if (parse_script) {
                            throw new SQLException(EType.RuntimeError, 2,
                                    "EXECFILE instruction cannot be executed in script");
                        } else {
                            state_code = State.EXECFILE;
                        }
                    } else if (token.equals("DROP")) {
                        state_code = State.DROP;
                    } else if (token.equals("DELETE")) {
                        state_code = State.DELETE;
                    } else if (!token.equals(";")) {
                        throw new SQLException(EType.SyntaxError, 3, "invalid operation: " + token);
                    }
                }
                break;
                case DELETE: {
                    if (token.equals("FROM")) {
                        state_code = State.DELETE_FROM;
                    } else {
                        throw new SQLException(EType.SyntaxError, 7, "invalid argument, expecting 'FROM'");
                    }
                }
                break;
                case DELETE_FROM: {
                    if (CommonUtils.IsLegalName(token)) {
                        state_code = State.DELETE_TABLE_PARSED;
                        API.SetDeleteTable(token);
                    } else {
                        throw new SQLException(EType.SyntaxError, 8, "invalid delete table: " + token);
                    }
                }
                break;
                case DELETE_TABLE_PARSED: {
                    if (token.equals(";")) {
                        API.QueryDelete();
                        state_code = State.IDLE;
                    } else if (token.equals("WHERE")) {
                        state_code = State.DELETE_WHERE_PARSED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 9, "invalid delete condition: " + token);
                    }
                }
                break;
                case DELETE_WHERE_PARSED:
                case DELETE_AND_PARSED: {
                    if (CommonUtils.IsLegalExpr(token)) {
                        API.SetWhereExpr1(token);
                        state_code = State.DELETE_EXPR1_PARSED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 10, "invalid where expression: " + token);
                    }
                }
                break;
                case DELETE_EXPR1_PARSED: {
                    switch (token) {
                        case "=":
                            API.SetWhereCmp(CMP.EQUAL);
                            state_code = State.DELETE_CMP_PARSED;
                            break;
                        case "!=":
                            API.SetWhereCmp(CMP.NOT_EQUAL);
                            state_code = State.DELETE_CMP_PARSED;
                            break;
                        case ">":
                            API.SetWhereCmp(CMP.GREATER);
                            state_code = State.DELETE_CMP_PARSED;
                            break;
                        case "<":
                            API.SetWhereCmp(CMP.LESS);
                            state_code = State.DELETE_CMP_PARSED;
                            break;
                        case ">=":
                            API.SetWhereCmp(CMP.GREATER_EQUAL);
                            state_code = State.DELETE_CMP_PARSED;
                            break;
                        case "<=":
                            API.SetWhereCmp(CMP.LESS_EQUAL);
                            state_code = State.DELETE_CMP_PARSED;
                            break;
                        default:
                            throw new SQLException(EType.SyntaxError, 11,
                                    "invalid comparison expression: " + token);
                    }
                }
                break;
                case DELETE_CMP_PARSED: {
                    if (CommonUtils.IsLegalExpr(token)) {
                        API.SetWhereExpr2(token);
                        state_code = State.DELETE_EXPR2_PARSED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 12, "invalid where expression: " + token);
                    }
                }
                break;
                case DELETE_EXPR2_PARSED: {
                    if (token.equals("AND")) {
                        state_code = State.DELETE_AND_PARSED;
                    } else if (token.equals(";")) {
                        API.QueryDelete();
                        state_code = State.IDLE;
                    } else {
                        throw new SQLException(EType.SyntaxError, 13, "invalid where argument: " + token);
                    }
                }
                break;
                case EXECFILE: {
                    API.SetExecFile(token);
                    state_code = State.FILE_PARSED;
                }
                break;
                case FILE_PARSED: {
                    if (token.equals(";")) {
                        parse_script = true;
                        API.QueryExecFile();
                        parse_script = false;
                        state_code = State.IDLE;
                    } else {
                        throw new SQLException(EType.SyntaxError, 14, "expect ';' to finish query");
                    }
                }
                break;
                case QUIT:
                    return;
                case DROP: {
                    if (token.equals("INDEX"))
                        state_code = State.DROP_INDEX;
                    else if (token.equals("TABLE"))
                        state_code = State.DROP_TABLE;
                    else {
                        throw new SQLException(EType.SyntaxError, 15,
                                "invalid DROP object, expect 'INDEX' or 'TABLE'");
                    }
                }
                break;
                case DROP_INDEX: {
                    if (CommonUtils.IsLegalName(token)) {
                        state_code = State.DROP_INDEX_PARSED;
                        API.SetDropIndex(token);
                    } else {
                        throw new SQLException(EType.SyntaxError, 16, "invalid drop index: " + token);
                    }
                }
                break;
                case DROP_INDEX_PARSED: {
                    if (token.equals(";")) {
                        API.QueryDropIndex();
                        state_code = State.IDLE;
                    } else {
                        throw new SQLException(EType.SyntaxError, 17,
                                "invalid token, expect ';' to finish query");
                    }
                }
                break;
                case DROP_TABLE: {
                    if (CommonUtils.IsLegalName(token)) {
                        state_code = State.DROP_TABLE_PARSED;
                        API.SetDropTable(token);
                    } else {
                        throw new SQLException(EType.SyntaxError, 18, "invalid drop table: " + token);
                    }
                }
                break;
                case DROP_TABLE_PARSED: {
                    if (token.equals(";")) {
                        API.QueryDropTable();
                        state_code = State.IDLE;
                    } else {
                        throw new SQLException(EType.SyntaxError, 19,
                                "invalid token, expect ';' to finish query");
                    }
                }
                break;
                case INSERT: {
                    if (token.equals("INTO"))
                        state_code = State.INTO;
                    else {
                        throw new SQLException(EType.SyntaxError, 20,
                                "invalid keyword, expect 'INTO'");
                    }
                }
                break;
                case INTO: {
                    if (CommonUtils.IsLegalName(token)) {
                        API.SetInsertTable(token);
                        state_code = State.INSERT_PARSED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 21, "invalid insert table: " + token);
                    }
                }
                break;
                case INSERT_PARSED: {
                    if (token.equals("VALUES")) {
                        state_code = State.VALUES;
                    } else {
                        throw new SQLException(EType.SyntaxError, 22, "invalid keyword, expect 'VALUES'");
                    }
                }
                break;
                case VALUES: {
                    if (token.equals("(")) {
                        state_code = State.INSERT_LEFT_BRACKET;
                    } else {
                        throw new SQLException(EType.SyntaxError, 23, "invalid keyword, expect '('");
                    }
                }
                break;
                case INSERT_LEFT_BRACKET: {
                    if (token.equals(")")) {
                        throw new SQLException(EType.SyntaxError, 24, "cannot insert nothing to a table!");
                    } else if (CommonUtils.IsString(token)) {
                        API.SetInsertValue(CommonUtils.ParseString(token), DataType.CHAR);
                        state_code = State.INSERT_VALUE_RECEIVED;
                    } else if (CommonUtils.IsInteger(token)) {
                        API.SetInsertValue(token, DataType.INT);
                        state_code = State.INSERT_VALUE_RECEIVED;
                    } else if (CommonUtils.IsFloat(token)) {
                        API.SetInsertValue(token, DataType.FLOAT);
                        state_code = State.INSERT_VALUE_RECEIVED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 25, "invalid insert value: " + token);
                    }
                }
                break;
                case INSERT_VALUE_RECEIVED: {
                    if (token.equals(",")) {
                        state_code = State.INSERT_COMMA;
                    } else if (token.equals(")")) {
                        state_code = State.INSERT_RIGHT_BRACKET;
                    } else {
                        throw new SQLException(EType.SyntaxError, 26, "invalid symbol: " + token);
                    }
                }
                break;
                case INSERT_COMMA: {
                    if (token.equals(")")) {
                        throw new SQLException(EType.SyntaxError, 27, "expect another insert value!");
                    } else if (CommonUtils.IsString(token)) {
                        API.SetInsertValue(CommonUtils.ParseString(token), DataType.CHAR);
                        state_code = State.INSERT_VALUE_RECEIVED;
                    } else if (CommonUtils.IsInteger(token)) {
                        API.SetInsertValue(token, DataType.INT);
                        state_code = State.INSERT_VALUE_RECEIVED;
                    } else if (CommonUtils.IsFloat(token)) {
                        API.SetInsertValue(token, DataType.FLOAT);
                        state_code = State.INSERT_VALUE_RECEIVED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 28, "invalid insert value: " + token);
                    }
                }
                break;
                case INSERT_RIGHT_BRACKET: {
                    if (token.equals(";")) {
                        API.QueryInsert();
                        state_code = State.IDLE;
                    } else {
                        throw new SQLException(EType.SyntaxError, 29, "invalid keyword, expect ';' to finish query");
                    }
                }
                break;
                case CREATE: {
                    if (token.equals("INDEX")) {
                        state_code = State.CREATE_INDEX;
                    } else if (token.equals("TABLE")) {
                        state_code = State.CREATE_TABLE;
                    } else {
                        throw new SQLException(EType.SyntaxError, 30, "invalid keyword, expect 'TABLE' or 'INDEX'");
                    }
                }
                break;
                case CREATE_INDEX: {
                    if (CommonUtils.IsLegalName(token)) {
                        API.SetCreateIndex(token);
                        state_code = State.CREATE_INDEX_PARSED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 31, "invalid create index name: " + token);
                    }
                }
                break;
                case CREATE_INDEX_PARSED: {
                    if (token.equals("ON"))
                        state_code = State.ON;
                    else {
                        throw new SQLException(EType.SyntaxError, 32, "invalid argument, expect 'ON'");
                    }
                }
                break;
                case ON: {
                    if (CommonUtils.IsLegalName(token)) {
                        API.SetOnTable(token);
                        state_code = State.CREATE_INDEX_TABLE_PARSED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 33, "invalid table name: " + token);
                    }
                }
                break;
                case CREATE_INDEX_TABLE_PARSED: {
                    if (token.equals("("))
                        state_code = State.CREATE_INDEX_LEFT_BRACKET;
                    else {
                        throw new SQLException(EType.SyntaxError, 34, "invalid symbol, expect '('");
                    }
                }
                break;
                case CREATE_INDEX_LEFT_BRACKET: {
                    if (CommonUtils.IsLegalName(token)) {
                        API.SetOnAttribute(token);
                        state_code = State.CREATE_INDEX_ATTR_PARSED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 35, "invalid attribute name: " + token);
                    }
                }
                break;
                case CREATE_INDEX_ATTR_PARSED: {
                    if (token.equals(")"))
                        state_code = State.CREATE_INDEX_RIGHT_BRACKET;
                    else {
                        throw new SQLException(EType.SyntaxError, 36, "invalid symbol, expect ')'");
                    }
                }
                break;
                case CREATE_INDEX_RIGHT_BRACKET: {
                    if (token.equals(";")) {
                        API.QueryCreateIndex();
                        state_code = State.IDLE;
                    } else {
                        throw new SQLException(EType.SyntaxError, 37, "invalid symbol, expect ';' to finish query");
                    }
                }
                break;
                case CREATE_TABLE: {
                    if (CommonUtils.IsLegalName(token)) {
                        API.SetCreateTable(token);
                        state_code = State.CREATE_TABLE_PARSED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 38, "invalid create table name: " + token);
                    }
                }
                break;
                case CREATE_TABLE_PARSED: {
                    if (token.equals("("))
                        state_code = State.CREATE_TABLE_LEFT_BRACKET;
                    else {
                        throw new SQLException(EType.SyntaxError, 39, "invalid symbol, expect '('");
                    }
                }
                break;
                case CREATE_TABLE_LEFT_BRACKET: {
                    if (token.equals("PRIMARY")) {
                        state_code = State.CREATE_TABLE_PRIMARY;
                    } else if (CommonUtils.IsLegalName(token)) {
                        API.SetCreateAttr(token);
                        state_code = State.CREATE_ATTR_PARSED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 40,
                                "invalid keyword, expect attribute or 'PRIMARY'");
                    }
                }
                break;
                case CREATE_TABLE_PRIMARY: {
                    if (token.equals("KEY"))
                        state_code = State.CREATE_PRIMARY_KEY;
                    else {
                        throw new SQLException(EType.SyntaxError, 41, "invalid keyword, expect 'KEY'");
                    }
                }
                break;
                case CREATE_PRIMARY_KEY: {
                    if (token.equals("("))
                        state_code = State.CREATE_PRIMARY_LEFT_BRACKET;
                    else {
                        throw new SQLException(EType.SyntaxError, 42, "invalid keyword, expect '('");
                    }
                }
                break;
                case CREATE_PRIMARY_LEFT_BRACKET: {
                    if (CommonUtils.IsLegalName(token)) {
                        API.SetPrimary(token);
                        state_code = State.PRIMARY_ATTR_PARSED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 43,
                                "invalid primary attribute name: " + token);
                    }
                }
                break;
                case PRIMARY_ATTR_PARSED: {
                    if (token.equals(")"))
                        state_code = State.CREATE_PRIMARY_RIGHT_BRACKET;
                    else {
                        throw new SQLException(EType.SyntaxError, 44,
                                "invalid symbol, expect ')'");
                    }
                }
                break;
                case CREATE_PRIMARY_RIGHT_BRACKET: {
                    if (token.equals(","))
                        state_code = State.CREATE_COMMA;
                    else if (token.equals(")"))
                        state_code = State.CREATE_TABLE_RIGHT_BRACKET;
                    else {
                        throw new SQLException(EType.SyntaxError, 45,
                                "invalid symbol, expect ')' or ','");
                    }
                }
                break;
                case CREATE_COMMA: {
                    if (token.equals("PRIMARY")) {
                        state_code = State.CREATE_TABLE_PRIMARY;
                    } else if (CommonUtils.IsLegalName(token)) {
                        API.SetCreateAttr(token);
                        state_code = State.CREATE_ATTR_PARSED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 46,
                                "invalid create attribute: " + token);
                    }
                }
                break;
                case CREATE_ATTR_PARSED: {
                    switch (token) {
                        case "INT":
                            API.SetAttrType(DataType.INT);
                            state_code = State.CREATE_INT_PARSED;
                            break;
                        case "FLOAT":
                            API.SetAttrType(DataType.FLOAT);
                            state_code = State.CREATE_FLOAT_PARSED;
                            break;
                        case "CHAR":
                            API.SetAttrType(DataType.CHAR);
                            state_code = State.CREATE_CHAR_PARSED;
                            break;
                        default:
                            throw new SQLException(EType.SyntaxError, 47,
                                    "invalid attribute type: " + token);
                    }
                }
                break;
                case CREATE_INT_PARSED:
                case CREATE_FLOAT_PARSED:
                case CHAR_RIGHT_BRACKET: {
                    switch (token) {
                        case "UNIQUE":
                            API.SetAttrUnique();
                            state_code = State.UNIQUE_PARSED;
                            break;
                        case ",":
                            API.SetCreateAttrList();
                            state_code = State.CREATE_COMMA;
                            break;
                        case ")":
                            API.SetCreateAttrList();
                            state_code = State.CREATE_TABLE_RIGHT_BRACKET;
                            break;
                        default:
                            throw new SQLException(EType.SyntaxError, 48,
                                    "invalid argument: " + token);
                    }
                }
                break;
                case CREATE_CHAR_PARSED: {
                    if (token.equals("("))
                        state_code = State.CHAR_LEFT_BRACKET;
                    else {
                        throw new SQLException(EType.SyntaxError, 49,
                                "invalid symbol, expect '(' after 'CHAR'");
                    }
                }
                break;
                case CHAR_LEFT_BRACKET: {
                    if (CommonUtils.IsInteger(token)) {
                        int length = Integer.parseInt(token);
                        if (length <= 0 || length > DefaultSetting.CHAR_MAX_LENGTH) {
                            throw new SQLException(EType.SyntaxError, 50,
                                    "invalid char length, expect between [1~" +
                                            DefaultSetting.CHAR_MAX_LENGTH + "]!");
                        } else {
                            API.SetAttrLength(length);
                            state_code = State.CHAR_BIT_PARSED;
                        }
                    } else {
                        throw new SQLException(EType.SyntaxError, 51,
                                token + " is not a valid char length");
                    }
                }
                break;
                case CHAR_BIT_PARSED: {
                    if (token.equals(")"))
                        state_code = State.CHAR_RIGHT_BRACKET;
                    else {
                        throw new SQLException(EType.SyntaxError, 52,
                                "invalid symbol, expect ')' to bracket char length");
                    }
                }
                break;
                case UNIQUE_PARSED: {
                    if (token.equals(",")) {
                        API.SetCreateAttrList();
                        state_code = State.CREATE_COMMA;
                    } else if (token.equals(")")) {
                        API.SetCreateAttrList();
                        state_code = State.CREATE_TABLE_RIGHT_BRACKET;
                    } else {
                        throw new SQLException(EType.SyntaxError, 53, "invalid argument: " + token);
                    }
                }
                break;
                case CREATE_TABLE_RIGHT_BRACKET: {
                    if (token.equals(";")) {
                        API.QueryCreateTable();
                        state_code = State.IDLE;
                    } else {
                        throw new SQLException(EType.SyntaxError, 54,
                                "invalid symbol, expect ';' to finish query");
                    }
                }
                break;
                case SELECT: {
                    if (token.equals("*")) {
                        state_code = State.SELECT_ALL;
                    } else if (CommonUtils.IsLegalName(token)) {
                        API.SetSelectAttr(token);
                        state_code = State.SELECT_ATTR;
                    } else {
                        throw new SQLException(EType.SyntaxError, 55,
                                "invalid select attribute: " + token);
                    }
                }
                break;
                case SELECT_ALL: {
                    if (token.equals("FROM"))
                        state_code = State.SELECT_FROM;
                    else {
                        throw new SQLException(EType.SyntaxError, 56,
                                "invalid argument, expect 'FROM'");
                    }
                }
                break;
                case SELECT_ATTR: {
                    if (token.equals(","))
                        state_code = State.SELECT_ATTR_COMMA;
                    else if (token.equals("FROM"))
                        state_code = State.SELECT_FROM;
                    else {
                        throw new SQLException(EType.SyntaxError, 57,
                                "invalid argument, expect 'FROM' or ','");
                    }
                }
                break;
                case SELECT_FROM: {
                    if (CommonUtils.IsLegalName(token)) {
                        state_code = State.SELECT_TABLE_PARSED;
                        API.SetSelectTable(token);
                    } else {
                        throw new SQLException(EType.SyntaxError, 58, "invalid select table: " + token);
                    }
                }
                break;
                case SELECT_ATTR_COMMA: {
                    if (CommonUtils.IsLegalName(token)) {
                        API.SetSelectAttr(token);
                        state_code = State.SELECT_ATTR;
                    } else {
                        throw new SQLException(EType.SyntaxError, 59, "invalid select attribute: " + token);
                    }
                }
                break;
                case SELECT_TABLE_PARSED: {
                    if (token.equals(";")) {
                        API.QuerySelect();
                        state_code = State.IDLE;
                    } else if (token.equals("WHERE")) {
                        state_code = State.SELECT_WHERE_PARSED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 60,
                                "invalid keyword expect 'WHERE' condition or ';' to finish query");
                    }
                }
                break;
                case SELECT_WHERE_PARSED: {
                    if (CommonUtils.IsLegalExpr(token)) {
                        API.SetWhereExpr1(token);
                        state_code = State.SELECT_EXPR1_PARSED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 61,
                                "invalid WHERE expression: " + token);
                    }
                }
                break;
                case SELECT_EXPR1_PARSED: {
                    switch (token) {
                        case "=":
                            API.SetWhereCmp(CMP.EQUAL);
                            state_code = State.SELECT_CMP_PARSED;
                            break;
                        case "!=":
                            API.SetWhereCmp(CMP.NOT_EQUAL);
                            state_code = State.SELECT_CMP_PARSED;
                            break;
                        case ">":
                            API.SetWhereCmp(CMP.GREATER);
                            state_code = State.SELECT_CMP_PARSED;
                            break;
                        case "<":
                            API.SetWhereCmp(CMP.LESS);
                            state_code = State.SELECT_CMP_PARSED;
                            break;
                        case ">=":
                            API.SetWhereCmp(CMP.GREATER_EQUAL);
                            state_code = State.SELECT_CMP_PARSED;
                            break;
                        case "<=":
                            API.SetWhereCmp(CMP.LESS_EQUAL);
                            state_code = State.SELECT_CMP_PARSED;
                            break;
                        default:
                            throw new SQLException(EType.SyntaxError, 62,
                                    "invalid comparison expression: " + token);
                    }
                }
                break;
                case SELECT_CMP_PARSED: {
                    if (CommonUtils.IsLegalExpr(token)) {
                        API.SetWhereExpr2(token);
                        state_code = State.SELECT_EXPR2_PARSED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 63,
                                "invalid WHERE expression: " + token);
                    }
                }
                break;
                case SELECT_EXPR2_PARSED: {
                    if (token.equals("AND")) {
                        state_code = State.SELECT_AND_PARSED;
                    } else if (token.equals(";")) {
                        API.QuerySelect();
                        state_code = State.IDLE;
                    } else {
                        throw new SQLException(EType.SyntaxError, 64,
                                "invalid where argument, expect 'AND' or ';'");
                    }
                }
                break;
                case SELECT_AND_PARSED: {
                    if (CommonUtils.IsLegalExpr(token)) {
                        API.SetWhereExpr1(token);
                        state_code = State.SELECT_EXPR1_PARSED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 65,
                                "invalid WHERE expression: " + token);
                    }
                }
                break;
                default:
                    break;
            }
        }
    }

}
