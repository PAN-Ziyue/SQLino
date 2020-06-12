package Interpreter;

import API.API;
import Data.CMP;
import Data.DataType;
import Utils.CommonUtils;
import Utils.EType;
import Utils.SQLException;

import java.util.HashSet;
import java.util.regex.Pattern;

public class Interpreter {
    private static State state_code;

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
    }};

    public Interpreter() {
        state_code = State.IDLE;
    }

    public static void SetState(State set_state_code) {
        state_code = set_state_code;
    }

    public static State GetState() {
        return state_code;
    }

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
                        throw new SQLException(EType.SyntaxError, 1, "invalid escape character: \\" + input_string.charAt(i + 1));
                    }
                }
                i++;
            } else if (input_string.charAt(i) == '\\' && !quoted) {
                throw new SQLException(EType.SyntaxError, 2, "redundant back slash");
            } else if (quoted) {
                String hex = String.format("%x", (int) input_string.charAt(i));
                rst.append('\\').append(hex);
            } else {
                rst.append(input_string.charAt(i));
            }
        }
        if (quoted) {
            throw new SQLException(EType.SyntaxError, 3, "unquoted string");
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
//        System.out.println(rst);
        return rst.toString();
    }

    public static void ReadInput(String input_string) throws SQLException {
        String[] tokens = input_string.split("\\s+");
        for (String token : tokens) {
            token = token.toUpperCase();
            if (token.equals(""))
                continue;
            switch (state_code) {
                case State.IDLE: {
                    if (token.equals("SELECT"))
                        state_code = State.SELECT;
                    else if (token.equals("QUIT")) {
                        state_code = State.QUIT;
                        return;
                    } else if (token.equals("CREATE")) {
                        state_code = State.CREATE;
                    } else if (token.equals("INSERT")) {
                        state_code = State.INSERT;
                    } else if (token.equals("EXECFILE")) {
                        state_code = State.EXECFILE;
                    } else if (token.equals("DROP")) {
                        state_code = State.DROP;
                    } else if (token.equals("DELETE")) {
                        state_code = State.DELETE;
                    } else if (!token.equals(";")) {
                        throw new SQLException(EType.SyntaxError, 4, "invalid operation: " + token);
                    }
                }
                break;
                case State.DELETE: {
                    if (token.equals("FROM")) {
                        state_code = State.DELETE_FROM;
                    } else {
                        throw new SQLException(EType.SyntaxError, 5, "invalid argument, expecting 'FROM'");
                    }
                }
                break;
                case State.DELETE_FROM: {
                    if (CommonUtils.IsLegalName(token)) {
                        state_code = State.DELETE_TABLE_PARSED;
                        API.SetDeleteTable(token);
                    } else {
                        throw new SQLException(EType.SyntaxError, 6, "invalid delete table: " + token);
                    }
                }
                break;
                case State.DELETE_TABLE_PARSED: {
                    if (token.equals(";")) {
                        API.QueryDelete();
                        state_code = State.IDLE;
                    } else if (token.equals("WHERE")) {
                        state_code = State.DELETE_WHERE_PARSED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 7, "invalid delete condition: " + token);
                    }
                }
                break;
                case State.DELETE_WHERE_PARSED: {
                    if (CommonUtils.IsLegalExpr(token)) {
                        API.SetWhereExpr1(token);
                        state_code = State.DELETE_EXPR1_PARSED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 8, "invalid where expression: " + token);
                    }
                }
                break;
                case State.DELETE_EXPR1_PARSED: {
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
                            throw new SQLException(EType.SyntaxError, 9, "invalid comparison expression: " + token);
                    }
                }
                break;
                case State.DELETE_CMP_PARSED: {
                    if (CommonUtils.IsLegalExpr(token)) {
                        API.SetWhereExpr2(token);
                        state_code = State.DELETE_EXPR2_PARSED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 8, "invalid where expression: " + token);
                    }
                }
                break;
                case State.DELETE_EXPR2_PARSED: {
                    if (token.equals("AND")) {
                        state_code = State.DELETE_AND_PARSED;
                    } else if (token.equals(";")) {
                        API.QueryDelete();
                        state_code = State.IDLE;
                    } else {
                        throw new SQLException(EType.SyntaxError, 10, "invalid where argument: " + token);
                    }
                }
                break;
                case State.DELETE_AND_PARSED: {
                    if (CommonUtils.IsLegalExpr(token)) {
                        API.SetWhereExpr1(token);
                        state_code = State.SELECT_EXPR1_PARSED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 8, "invalid where expression: " + token);
                    }
                }
                break;
                case State.EXECFILE: {
                    //if (token[0] == '\\') {
                    //	token.erase(0, 1);
                    //	string final_filename = "";
                    //	vector<string> parse_vec = split(token, '\\');
                    //	for (int i = 0; i < parse_vec.size(); i++) {
                    //		unsigned int each_char_val;
                    //		stringstream ss;
                    //		ss << hex << parse_vec[i];
                    //		ss >> each_char_val;
                    //		final_filename.push_back(each_char_val);
                    //	}
                    //	if (ParseFileInput(final_filename)) {
                    //		ExecFile(final_filename);
                    //	}
                    //	state_code = State.IDLE;
                    //}
                    //else {
                    //	PromptErr("[Syntax Error] illegal file path, expect a string");
                    //	state_code = State.IDLE;
                    //	return;
                    //}
                    state_code = State.IDLE;
                }
                break;
                case State.QUIT:
                    return;
                case State.DROP: {
                    if (token.equals("INDEX"))
                        state_code = State.DROP_INDEX;
                    else if (token.equals("TABLE"))
                        state_code = State.DROP_TABLE;
                    else {
                        throw new SQLException(EType.SyntaxError, 11, "unreferenced DROP object, expect 'INDEX' or 'TABLE'");
                    }
                }
                break;
                case State.DROP_INDEX: {
                    if (CommonUtils.IsLegalName(token)) {
                        state_code = State.DROP_INDEX_PARSED;
                        API.SetDropIndex(token);
                    } else {
                        throw new SQLException(EType.SyntaxError, 12, "invalid drop index: " + token);
                    }
                }
                break;
                case State.DROP_INDEX_PARSED: {
                    if (token.equals(";")) {
                        API.QueryDropIndex();
                        state_code = State.IDLE;
                    } else {
                        throw new SQLException(EType.SyntaxError, 13, "invalid token, expect ';' to finish query");
                    }
                }
                break;
                case State.DROP_TABLE: {
                    if (CommonUtils.IsLegalName(token)) {
                        state_code = State.DROP_TABLE_PARSED;
                        API.SetDropTable(token);
                    } else {
                        throw new SQLException(EType.SyntaxError, 14, "invalid drop table: " + token);
                    }
                }
                break;
                case State.DROP_TABLE_PARSED: {
                    if (token.equals(";")) {
                        API.QueryCreateTable();
                        state_code = State.IDLE;
                    } else {
                        throw new SQLException(EType.SyntaxError, 13, "invalid token, expect ';' to finish query");
                    }
                }
                break;
                case State.INSERT: {
                    if (token.equals("INTO"))
                        state_code = State.INTO;
                    else {
                        throw new SQLException(EType.SyntaxError, 15, "invalid keyword, expect 'INTO'");
                    }
                }
                break;
                case State.INTO: {
                    if (CommonUtils.IsLegalName(token)) {
                        API.SetInsertTable(token);
                        state_code = State.INSERT_PARSED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 16, "invalid insert table: " + token);
                    }
                }
                break;
                case State.INSERT_PARSED: {
                    if (token.equals("VALUES")) {
                        state_code = State.VALUES;
                    } else {
                        throw new SQLException(EType.SyntaxError, 17, "invalid keyword, expect 'VALUES'");
                    }
                }
                break;
                case State.VALUES: {
                    if (token.equals("(")) {
                        state_code = State.INSERT_LEFT_BRACKET;
                    } else {
                        throw new SQLException(EType.SyntaxError, 18, "invalid keyword, expect '('");
                    }
                }
                break;
                case State.INSERT_LEFT_BRACKET: {
                    if (token.equals(")")) {
                        throw new SQLException(EType.SyntaxError, 21, "cannot insert nothing to a table!");
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
                        throw new SQLException(EType.SyntaxError, 19, "invalid insert value: " + token);
                    }
                }
                break;
                case State.INSERT_VALUE_RECEIVED: {
                    if (token.equals(",")) {
                        state_code = State.INSERT_COMMA;
                    } else if (token.equals(")")) {
                        state_code = State.INSERT_RIGHT_BRACKET;
                    } else {
                        throw new SQLException(EType.SyntaxError, 20, "invalid symbol: " + token);
                    }
                }
                break;
                case State.INSERT_COMMA: {
                    if (token.equals(")")) {
                        throw new SQLException(EType.SyntaxError, 22, "expect another insert value!");
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
                        throw new SQLException(EType.SyntaxError, 19, "invalid insert value: " + token);
                    }
                }
                break;
                case State.INSERT_RIGHT_BRACKET: {
                    if (token.equals(";")) {
                        API.QueryInsert();
                        state_code = State.IDLE;
                    } else {
                        throw new SQLException(EType.SyntaxError, 23, "invalid keyword, expect ';' to finish query");
                    }
                }
                break;
                case State.CREATE: {
                    if (token.equals("INDEX")) {
                        state_code = State.CREATE_INDEX;
                    } else if (token.equals("TABLE")) {
                        state_code = State.CREATE_TABLE;
                    } else {
                        throw new SQLException(EType.SyntaxError, 24, "invalid keyword, expect 'TABLE' or 'INDEX'");
                    }
                }
                break;
                case State.CREATE_INDEX: {
                    if (CommonUtils.IsLegalName(token)) {
                        API.SetCreateIndex(token);
                        state_code = State.CREATE_INDEX_PARSED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 25, "invalid create index name: " + token);
                    }
                }
                break;
                case State.CREATE_INDEX_PARSED: {
                    if (token.equals("ON"))
                        state_code = State.ON;
                    else {
                        throw new SQLException(EType.SyntaxError, 25, "invalid argument, expect 'ON'");
                    }
                }
                break;
                case State.ON: {
                    if (CommonUtils.IsLegalName(token)) {
                        API.SetOnTable(token);
                        state_code = State.CREATE_INDEX_TABLE_PARSED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 26, "invalid table name: " + token);
                    }
                }
                break;
                case State.CREATE_INDEX_TABLE_PARSED: {
                    if (token.equals("("))
                        state_code = State.CREATE_INDEX_LEFT_BRACKET;
                    else {
                        throw new SQLException(EType.SyntaxError, 27, "invalid symbol, expect '('");
                    }
                }
                break;
                case State.CREATE_INDEX_LEFT_BRACKET: {
                    if (CommonUtils.IsLegalName(token)) {
                        API.SetOnAttribute(token);
                        state_code = State.CREATE_INDEX_ATTR_PARSED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 28, "invalid attribute name: " + token);
                    }
                }
                break;
                case State.CREATE_INDEX_ATTR_PARSED: {
                    if (token.equals(")"))
                        state_code = State.CREATE_INDEX_RIGHT_BRACKET;
                    else {
                        throw new SQLException(EType.SyntaxError, 29, "invalid symbol, expect ')'");
                    }
                }
                break;
                case State.CREATE_INDEX_RIGHT_BRACKET: {
                    if (token.equals(";")) {
                        API.QueryCreateIndex();
                        state_code = State.IDLE;
                    } else {
                        throw new SQLException(EType.SyntaxError, 30, "invalid symbol, expect ';' to finish query");
                    }
                }
                break;
                case State.CREATE_TABLE: {
                    if (CommonUtils.IsLegalName(token)) {
                        API.SetCreateTable(token);
                        state_code = State.CREATE_TABLE_PARSED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 31, "invalid create table name: " + token);
                    }
                }
                break;
                case State.CREATE_TABLE_PARSED: {
                    if (token.equals("("))
                        state_code = State.CREATE_TABLE_LEFT_BRACKET;
                    else {
                        throw new SQLException(EType.SyntaxError, 32, "invalid symbol, expect '('");
                    }
                }
                break;
                case State.CREATE_TABLE_LEFT_BRACKET: {
                    if (token.equals("PRIMARY")) {
                        state_code = State.CREATE_TABLE_PRIMARY;
                    } else if (CommonUtils.IsLegalName(token)) {
                        API.SetCreateAttr(token);
                        state_code = State.CREATE_ATTR_PARSED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 33, "invalid keyword, expect attribute or 'PRIMARY'");
                    }
                }
                break;
                case State.CREATE_TABLE_PRIMARY: {
                    if (token.equals("KEY"))
                        state_code = State.CREATE_PRIMARY_KEY;
                    else {
                        throw new SQLException(EType.SyntaxError, 34, "invalid keyword, expect 'KEY'");
                    }
                }
                break;
                case State.CREATE_PRIMARY_KEY: {
                    if (token.equals("("))
                        state_code = State.CREATE_PRIMARY_LEFT_BRACKET;
                    else {
                        throw new SQLException(EType.SyntaxError, 35, "invalid keyword, expect '('");
                    }
                }
                break;
                case State.CREATE_PRIMARY_LEFT_BRACKET: {
                    if (CommonUtils.IsLegalName(token)) {
                        API.SetPrimary(token);
                        state_code = State.PRIMARY_ATTR_PARSED;
                    } else {
                        throw new SQLException(EType.SyntaxError, 36, "invalid primary attribute name: " + token);
                    }
                }
                break;
                case State.PRIMARY_ATTR_PARSED: {
                    if (token.equals(")"))
                        state_code = State.CREATE_PRIMARY_RIGHT_BRACKET;
                    else {
                        throw new SQLException(EType.SyntaxError, 37, "invalid symbol, expect ')'");
                    }
                }
                break;
                case State.CREATE_PRIMARY_RIGHT_BRACKET: {
                    if (token == ",")
                        state_code = State.CREATE_COMMA;
                    else if (token == ")")
                        state_code = State.CREATE_TABLE_RIGHT_BRACKET;
                    else if (token != "") {
                        PromptErr("[Syntax Error] expect ) or ,");
                        api.create_table.Clear();
                        state_code = State.IDLE;
                        return;
                    }
                }
                break;
                case State.CREATE_COMMA: {
                    if (token == "PRIMARY" || token == "primary") {
                        state_code = State.CREATE_TABLE_PRIMARY;
                    } else if (regex_match(token, regex("^[a-zA-Z0-9_]*$"))) {
                        api.create_table.InsertAttr(token);
                        state_code = State.CREATE_ATTR_PARSED;
                    } else if (token != "") {
                        PromptErr("[Syntax Error] invalid keyword: " + token);
                        api.create_table.Clear();
                        state_code = State.IDLE;
                        return;
                    }
                }
                break;
                case State.CREATE_ATTR_PARSED: {
                    if (token == "INT" || token == "int") {
                        state_code = State.CREATE_INT_PARSED;
                        api.create_table.InsertType(INT);
                    } else if (token == "FLOAT" || token == "float") {
                        state_code = State.CREATE_FLOAT_PARSED;
                        api.create_table.InsertType(FLOAT);
                    } else if (token == "CHAR" || token == "char") {
                        state_code = State.CREATE_CHAR_PARSED;
                        api.create_table.InsertType(CHAR);
                    } else if (token != "") {
                        PromptErr("[Syntax Error] invalid attribute type: " + token);
                        api.create_table.Clear();
                        state_code = State.IDLE;
                        return;
                    }
                }
                break;
                case State.CREATE_INT_PARSED: {
                    if (token == "UNIQUE" || token == "unique") {
                        api.create_table.InsertUnique();
                        state_code = State.UNIQUE_PARSED;
                    } else if (token == ",")
                        state_code = State.CREATE_COMMA;
                    else if (token == ")")
                        state_code = State.CREATE_TABLE_RIGHT_BRACKET;
                    else if (token != "") {
                        PromptErr("[Syntax Error] invalid key word: " + token);
                        api.create_table.Clear();
                        state_code = State.IDLE;
                        return;
                    }
                }
                break;
                case State.CREATE_CHAR_PARSED: {
                    if (token == "(")
                        state_code = State.CHAR_LEFT_BRACKET;
                    else if (token != "") {
                        PromptErr("[Syntax Error] invalid key word: " + token);
                        api.create_table.Clear();
                        state_code = State.IDLE;
                        return;
                    }
                }
                break;
                case State.CREATE_FLOAT_PARSED: {
                    if (token == "UNIQUE" || token == "unique") {
                        api.create_table.InsertUnique();
                        state_code = State.UNIQUE_PARSED;
                    } else if (token == ",")
                        state_code = State.CREATE_COMMA;
                    else if (token == ")")
                        state_code = State.CREATE_TABLE_RIGHT_BRACKET;
                    else if (token != "") {
                        PromptErr("[Syntax Error] invalid key word: " + token);
                        api.create_table.Clear();
                        state_code = State.IDLE;
                        return;
                    }
                }
                break;

                case State.CHAR_LEFT_BRACKET: {
                    if (regex_match(token, regex("^[0-9]*$"))) {
                        api.create_table.InsertSize(stoi(token));
                        state_code = State.CHAR_BIT_PARSED;
                    } else if (token != "") {
                        PromptErr("[Syntax Error] invalid char size: " + token);
                        api.create_table.Clear();
                        state_code = State.IDLE;
                        return;
                    }
                }
                break;
                case State.CHAR_BIT_PARSED: {
                    if (token == ")")
                        state_code = State.CHAR_RIGHT_BRACKET;
                    else if (token != "") {
                        PromptErr("[Syntax Error] invalid keyword: " + token);
                        api.create_table.Clear();
                        state_code = State.IDLE;
                        return;
                    }
                }
                break;
                case State.CHAR_RIGHT_BRACKET: {
                    if (token == "UNIQUE" || token == "unique") {
                        api.create_table.InsertUnique();
                        state_code = State.UNIQUE_PARSED;
                    } else if (token == ",")
                        state_code = State.CREATE_COMMA;
                    else if (token == ")")
                        state_code = State.CREATE_TABLE_RIGHT_BRACKET;
                    else if (token != "") {
                        PromptErr("[Syntax Error] invalid key word: " + token);
                        api.create_table.Clear();
                        state_code = State.IDLE;
                        return;
                    }
                }
                break;
                case State.UNIQUE_PARSED: {
                    if (token == ",")
                        state_code = State.CREATE_COMMA;
                    else if (token == ")")
                        state_code = State.CREATE_TABLE_RIGHT_BRACKET;
                    else if (token != "") {
                        PromptErr("[Syntax Error] invalid key word: " + token);
                        api.create_table.Clear();
                        state_code = State.IDLE;
                        return;
                    }
                }
                break;
                case State.CREATE_TABLE_RIGHT_BRACKET: {
                    if (token == ";") {
                        api.create_table.Query();
                        state_code = State.IDLE;
                    } else if (token != "") {
                        PromptErr("[Syntax Error] expect ;");
                        api.create_table.Clear();
                        state_code = State.IDLE;
                        return;
                    }
                }
                break;
                case State.SELECT: {
                    if (token == "*") {
                        api.select_query.SetSelectAll();
                        state_code = State.SELECT_ALL;
                    } else if (regex_match(token, regex("^[a-zA-Z0-9_]*$"))) {
                        api.select_query.Insert(token);
                        state_code = State.SELECT_ATTR;
                    } else if (token != "") {
                        PromptErr("[Syntax Error] invalid attribute name: " + token);
                        api.select_query.Clear();
                        state_code = State.IDLE;
                        return;
                    }
                }
                break;
                case State.SELECT_ALL: {
                    if (token == "FROM" || token == "from")
                        state_code = State.SELECT_FROM;
                    else if (token != "") {
                        PromptErr("[Syntax Error] expect FROM");
                        api.select_query.Clear();
                        state_code = State.IDLE;
                        return;
                    }
                }
                break;
                case State.SELECT_ATTR: {
                    if (token == ",")
                        state_code = State.SELECT_ATTR_COMMA;
                    else if (token == "FROM" || token == "from")
                        state_code = State.SELECT_FROM;
                    else if (token != "") {
                        PromptErr("[Syntax Error] invalid keyword: " + token);
                        api.select_query.Clear();
                        state_code = State.IDLE;
                        return;
                    }
                }
                break;
                case State.SELECT_FROM: {
                    if (regex_match(token, regex("^[a-zA-Z0-9_]*$")))
                        state_code = State.SELECT_TABLE_PARSED;
                    else if (token != "") {
                        PromptErr("[Syntax Error] invalid table name: " + token);
                        api.select_query.Clear();
                        state_code = State.IDLE;
                        return;
                    }
                }
                break;
                case State.SELECT_ATTR_COMMA: {
                    if (regex_match(token, regex("^[a-zA-Z0-9_]*$"))) {
                        api.select_query.Insert(token);
                        state_code = State.SELECT_ATTR;
                    } else if (token != "") {
                        PromptErr("[Syntax Error] invalid attribute name: " + token);
                        api.select_query.Clear();
                        state_code = State.IDLE;
                        return;
                    }
                }
                break;
                case State.SELECT_TABLE_PARSED: {
                    if (token == ";") {
                        api.select_query.Query(api.record_manager);
                        state_code = State.IDLE;
                    } else if (token == "WHERE" || token == "where") {
                        state_code = State.SELECT_WHERE_PARSED;
                    } else if (token != "") {
                        PromptErr("[Syntax Error] invalid keyword: " + token);
                        api.select_query.Clear();
                        state_code = State.IDLE;
                        return;
                    }
                }
                break;
                case State.SELECT_WHERE_PARSED: {
                    if (is_expr(token)) {
                        api.select_query.SetExpr1(token);
                        state_code = State.SELECT_EXPR1_PARSED;
                    } else if (token != "") {
                        PromptErr("[Syntax Error] invalid expression: " + token);
                        api.select_query.Clear();
                        state_code = State.IDLE;
                        return;
                    }
                }
                break;
                case State.SELECT_EXPR1_PARSED: {
                    if (token == "=") {
                        api.select_query.SetCmp(EQUAL);
                        state_code = State.SELECT_CMP_PARSED;
                    } else if (token == "!=") {
                        api.select_query.SetCmp(NOT_EQUAL);
                        state_code = State.SELECT_CMP_PARSED;
                    } else if (token == ">") {
                        api.select_query.SetCmp(GREATER);
                        state_code = State.SELECT_CMP_PARSED;
                    } else if (token == "<") {
                        api.select_query.SetCmp(LESS);
                        state_code = State.SELECT_CMP_PARSED;
                    } else if (token == ">=") {
                        api.select_query.SetCmp(GREATER_EQUAL);
                        state_code = State.SELECT_CMP_PARSED;
                    } else if (token == "<=") {
                        api.select_query.SetCmp(LESS_EQUAL);
                        state_code = State.SELECT_CMP_PARSED;
                    } else if (token != "") {
                        PromptErr("[Syntax Error] invalid comparison expression: " + token);
                        api.select_query.Clear();
                        state_code = State.IDLE;
                        return;
                    }
                }
                break;
                case State.SELECT_CMP_PARSED: {
                    if (is_expr(token)) {
                        api.select_query.SetWhereExpr(token);
                        state_code = State.SELECT_EXPR2_PARSED;
                    } else if (token != "") {
                        PromptErr("[Syntax Error] invalid expression: " + token);
                        api.select_query.Clear();
                        state_code = State.IDLE;
                        return;
                    }
                }
                break;
                case State.SELECT_EXPR2_PARSED: {
                    if (token == "AND" || token == "and") {
                        state_code = State.SELECT_AND_PARSED;
                    } else if (token == ";") {
                        api.select_query.Query(api.record_manager);
                        state_code = State.IDLE;
                    } else if (token != "") {
                        PromptErr("[Syntax Error] invalid argument: " + token);
                        api.select_query.Clear();
                        state_code = State.IDLE;
                        return;
                    }
                }
                break;
                case State.SELECT_AND_PARSED: {
                    if (is_expr(token)) {
                        api.select_query.SetExpr1(token);
                        state_code = State.SELECT_EXPR1_PARSED;
                    } else if (token != "") {
                        PromptErr("[Syntax Error] invalid expression: " + token);
                        api.select_query.Clear();
                        state_code = State.IDLE;
                        return;
                    }
                }
                break;
                default:
                    break;
            }
        }
    }


}
