package com.web.common;

import com.google.common.hash.Hashing;
import com.web.common.DateTimeUtil.DateTime;
import com.web.common.KeyValueGetter.DbMap;
import org.apache.commons.codec.binary.Base64;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 字符串处理相关工具类
 *
 * @author JingHe
 * @since 2019/8/13
 */
public class StringTools {

    private static final int caseDiff = ('a' - 'A');
    private static final char[] DIGITS_LOWER = {
        '0',
        '1',
        '2',
        '3',
        '4',
        '5',
        '6',
        '7',
        '8',
        '9',
        'a',
        'b',
        'c',
        'd',
        'e',
        'f'
    };
    /**
     * 基本型的转型方法列表
     */
    private static final Map<Type, Method> valueOfMethod = initValueOfMethod();
    private static final IllegalArgumentException classNotBasicTypeException = new IllegalArgumentException("classNotBasicType");
    private static final String[] HTML_ESCAPE_LIST;
    private static final String[] XML_ESCAPE_LIST;
    private static final String[] QUOTED_STRING_ESCAPE_LIST;
    private static final String[] WHITESPACE_ESCAPE_LIST;
    private static final String[] SQL_LIKE_PATTERN;
    private static BitSet dontNeedEncoding;

    static {
        dontNeedEncoding = new BitSet(256);
        int i;
        for (i = 'a'; i <= 'z'; i++) {
            dontNeedEncoding.set(i);
        }
        for (i = 'A'; i <= 'Z'; i++) {
            dontNeedEncoding.set(i);
        }
        for (i = '0'; i <= '9'; i++) {
            dontNeedEncoding.set(i);
        }
        dontNeedEncoding.set('-');
        dontNeedEncoding.set('_');
        dontNeedEncoding.set('.');
        dontNeedEncoding.set('!');
        dontNeedEncoding.set('~');
        dontNeedEncoding.set('*');
        dontNeedEncoding.set('\'');
        dontNeedEncoding.set('(');
        dontNeedEncoding.set(')');
    }

    static {
        HTML_ESCAPE_LIST = _buildEscapeArray();
        HTML_ESCAPE_LIST['<'] = "&lt;";
        HTML_ESCAPE_LIST['>'] = "&gt;";
        HTML_ESCAPE_LIST['\"'] = "&quot;";
        HTML_ESCAPE_LIST['\n'] = "<br/>";
        HTML_ESCAPE_LIST['\r'] = "";
        HTML_ESCAPE_LIST['\t'] = "&nbsp;&nbsp;&nbsp;&nbsp;";
        HTML_ESCAPE_LIST[' '] = "&nbsp;";
        // 空格的ascii码值有两个：从键盘输入的空格ascii值为0x20；从网页上的&nbsp;字符表单提交而来的空格ascii值为0xa0
        HTML_ESCAPE_LIST['\u00a0'] = "&nbsp;";
    }

    static {
        XML_ESCAPE_LIST = _buildEscapeArray();
        XML_ESCAPE_LIST['<'] = "&lt;";
        XML_ESCAPE_LIST['>'] = "&gt;";
        XML_ESCAPE_LIST['\"'] = "&quot;";
        XML_ESCAPE_LIST['&'] = "&amp;";
        XML_ESCAPE_LIST['\''] = "&apos;";
    }

    static {
        QUOTED_STRING_ESCAPE_LIST = _buildEscapeArray();
        QUOTED_STRING_ESCAPE_LIST['\\'] = "\\\\";
        QUOTED_STRING_ESCAPE_LIST['\"'] = "\\\"";
        QUOTED_STRING_ESCAPE_LIST['\''] = "\\\'";
        QUOTED_STRING_ESCAPE_LIST['\r'] = "\\r";
        QUOTED_STRING_ESCAPE_LIST['\n'] = "\\n";
        QUOTED_STRING_ESCAPE_LIST['\f'] = "\\f";
        QUOTED_STRING_ESCAPE_LIST['\t'] = "\\t";
        QUOTED_STRING_ESCAPE_LIST['\b'] = "\\b";
        QUOTED_STRING_ESCAPE_LIST['\u00a0'] = " ";
    }

    static {
        WHITESPACE_ESCAPE_LIST = _buildEscapeArray();
        WHITESPACE_ESCAPE_LIST['\r'] = "\\r";
        WHITESPACE_ESCAPE_LIST['\n'] = "\\n";
        WHITESPACE_ESCAPE_LIST['\f'] = "\\f";
        WHITESPACE_ESCAPE_LIST['\t'] = "\\t";
        WHITESPACE_ESCAPE_LIST['\b'] = "\\b";
        WHITESPACE_ESCAPE_LIST['\u00a0'] = " ";
    }

    static {
        SQL_LIKE_PATTERN = _buildEscapeArray();
        SQL_LIKE_PATTERN['%'] = "\\%";
        SQL_LIKE_PATTERN['_'] = "\\_";
    }

    private StringTools() {
    }

    /**
     * 判断字符串是否为空
     */
    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    /**
     * 判断字符串是否不为空
     */
    public static boolean isNotEmpty(CharSequence str) {
        return !isEmpty(str);
    }

    /**
     * 字符串分割并去除首尾空格
     */
    public static List<String> splitAndTrim(String str, String regex) {
        String[] array = str.split(regex);
        List<String> list = new ArrayList<String>(array.length);
        for (String a : array) {
            String add = a.trim();
            if (!add.isEmpty()) {
                list.add(add);
            }
        }
        return list;
    }

    /**
     * 对URL进行编码，抄自java.net.URLEncoder，其不同之处在于空格不会转为+而是转为%20， 与前端js的encodeURIComponent效果保持一致
     */
    public static String encodeURIComponent(String s, Charset charset) {
        boolean needToChange = false;
        StringBuffer out = new StringBuffer(s.length());
        CharArrayWriter charArrayWriter = new CharArrayWriter();
        for (int i = 0; i < s.length(); ) {
            int c = s.charAt(i);
            if (dontNeedEncoding.get(c)) {
                out.append((char) c);
                i++;
            } else {
                do {
                    charArrayWriter.write(c);
                    if (c >= 0xD800 && c <= 0xDBFF) {
                        if ((i + 1) < s.length()) {
                            int d = s.charAt(i + 1);
                            if (d >= 0xDC00 && d <= 0xDFFF) {
                                charArrayWriter.write(d);
                                i++;
                            }
                        }
                    }
                    i++;
                } while (i < s.length() && !dontNeedEncoding.get((c = s.charAt(i))));
                charArrayWriter.flush();
                String str = new String(charArrayWriter.toCharArray());
                byte[] ba = str.getBytes(charset);
                for (int j = 0; j < ba.length; j++) {
                    out.append('%');
                    char ch = Character.forDigit((ba[j] >> 4) & 0xF, 16);
                    // converting to use uppercase letter as part of
                    // the hex value if ch is a letter.
                    if (Character.isLetter(ch)) {
                        ch -= caseDiff;
                    }
                    out.append(ch);
                    ch = Character.forDigit(ba[j] & 0xF, 16);
                    if (Character.isLetter(ch)) {
                        ch -= caseDiff;
                    }
                    out.append(ch);
                }
                charArrayWriter.reset();
                needToChange = true;
            }
        }
        return (needToChange ? out.toString() : s);
    }

    /**
     * 对URL进行编码，抄自java.net.URLEncoder，其不同之处在于空格不会转为+而是转为%20， 与前端js的encodeURIComponent效果保持一致，使用UTF-8编码
     */
    public static String encodeURIComponent(String s) {
        return encodeURIComponent(s, Charsets.UTF_8);
    }

    /**
     * 对URL进行解码，抄自java.net.URLDecoder，其不同之处在于+不会转为空格而是处理为%2B， 与前端js的decodeURIComponent效果保持一致
     */
    public static String decodeURIComponent(String s, Charset charset) {
        boolean needToChange = false;
        int numChars = s.length();
        StringBuffer sb = new StringBuffer(numChars > 500 ? numChars / 2 : numChars);
        int i = 0;
        char c;
        byte[] bytes = null;
        while (i < numChars) {
            c = s.charAt(i);
            switch (c) {
            case '%':
                /*
                 * Starting with this instance of %, process all consecutive substrings of the form %xy. Each substring %xy will yield a byte. Convert all consecutive bytes obtained this way to
                 * whatever character(s) they represent in the provided encoding.
                 */
                try {
                    // (numChars-i)/3 is an upper bound for the number
                    // of remaining bytes
                    if (bytes == null) {
                        bytes = new byte[(numChars - i) / 3];
                    }
                    int pos = 0;

                    while (((i + 2) < numChars) && (c == '%')) {
                        bytes[pos++] = (byte) Integer.parseInt(s.substring(i + 1, i + 3), 16);
                        i += 3;
                        if (i < numChars) {
                            c = s.charAt(i);
                        }
                    }
                    // A trailing, incomplete byte encoding such as
                    // "%x" will cause an exception to be thrown
                    if ((i < numChars) && (c == '%')) {
                        throw new IllegalArgumentException("decodeURIComponent: Incomplete trailing escape (%) pattern");
                    }
                    sb.append(new String(bytes, 0, pos, charset));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("decodeURIComponent: Illegal hex characters in escape (%) pattern - " + e.getMessage());
                }
                needToChange = true;
                break;
            default:
                sb.append(c);
                i++;
                break;
            }
        }
        return (needToChange ? sb.toString() : s);
    }

    /**
     * 对URL进行解码，抄自java.net.URLDecoder，其不同之处在于+不会转为空格而是处理为%2B， 与前端js的decodeURIComponent效果保持一致，使用UTF-8解码
     */
    public static String decodeURIComponent(String s) {
        return decodeURIComponent(s, Charsets.UTF_8);
    }

    /**
     * 生成格式化后的字符串，中文内容（仅限GB18030字符集中有的）一个字按2个ascii字符的宽度处理（jdk自带的String.format做不到这一点），如果包括异常参数，自动将其内容转化后输出
     */
    public static String format(String format, Object... args) {
        if (null == args) {
            return format;
        }
        String fmt = new String(format.getBytes(Charsets.GB18030), Charsets.ISO_8859_1);
        Object[] objs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Throwable) {
                objs[i] = printThrowable((Throwable) args[i]);
            } else if (args[i] instanceof String) {
                objs[i] = new String(((String) args[i]).getBytes(Charsets.GB18030), Charsets.ISO_8859_1);
            } else {
                objs[i] = args[i];
            }
        }
        return new String(String.format(fmt, objs).getBytes(Charsets.ISO_8859_1), Charsets.GB18030);
    }



    /**
     * 比较两个字符串是否不相等
     * @param in0
     * @param in1
     * @return
     */
    public static boolean isNotEqual(String in0, String in1) {
        return !isEqual(in0, in1);
    }



    /**
     * 比较两个字符串是否不相等
     * 空串与null视为等价
     * @param in0
     * @param in1
     * @return
     */
    public static boolean isNotEqualIncludeEmpty(String in0, String in1) {
        return !isEqualIncludeEmpty(in0, in1);
    }


    /**
     * 比较两个字符串是否相等
     * @param in0
     * @param in1
     * @return
     */
    public static boolean isEqual(String in0, String in1) {

        if (null == in0 && null == in1) {
            return true;
        }

        if (null == in0 || null == in1) {
            return false;
        }
        return in0.equals(in1);
    }



    /**
     * 比较两个字符串是否相等
     * 空串与null视为等价
     * @param in0
     * @param in1
     * @return
     */
    public static boolean isEqualIncludeEmpty(String in0, String in1) {

        if (StringTools.isEmpty(in0) && StringTools.isEmpty(in1)) {
            return true;
        }

        if (StringTools.isEmpty(in0) || StringTools.isEmpty(in1)) {
            return false;
        }
        return in0.equals(in1);
    }


    /**
     * 合并带?的SQL语句，用于调试时显示执行的SQL之用
     */
    public static String mergeSql(String sql, Object... args) {
        if (null == args) {
            return sql;
        }
        String[] sqls = (sql + " ").split("\\?");
        if (args.length == sqls.length - 1) {
            StringBuilder exsql = new StringBuilder();
            int i;
            for (i = 0; i < args.length; i++) {
                exsql.append(sqls[i]);
                if (args[i] instanceof String) {
                    // 参数有限制，防止显示在日志中的SQL出现换行或者太长的情况
                    String src = args[i].toString().replace("'", "\\'").replace("\r", "").replace("\n", "").trim();
                    String append = src;
                    if (src.length() > 120) {
                        append = src.substring(0, 50) + "...(" + args[i].toString().length() + ")..." + src.substring(src.length() - 50, src.length());
                    }
                    exsql.append("'").append(append).append("'");
                } else if (args[i] instanceof Date) { // Date统一转为Datetime处理
                    exsql.append("'").append(new DateTime(((Date) args[i])).toString(DateTime.DF_yyyy_MM_dd_HHmmss_SSSZ)).append("'");
                } else if (args[i] instanceof byte[]) { // 二进制内容不显示，只显示长度信息
                    exsql.append("[Binary Content ").append(((byte[]) args[i]).length).append("]");
                } else {
                    exsql.append(args[i]);
                }
            }
            exsql.append(sqls[i]);
            exsql.deleteCharAt(exsql.length() - 1);
            return exsql.toString();
        }
        return "";
    }

    /**
     * 打印一行 长度为len的重复lineChar字符的分隔符
     */
    public static StringBuilder printLine(int len, char linechar) {
        return printLine(new StringBuilder(), len, linechar);
    }

    /**
     * 打印一行 长度为 len 的重复lineChar字符的分隔符
     *
     * @param corner   转角处所使用字符
     * @param linechar 默认字符
     */
    public static StringBuilder printLine(int len, char corner, char linechar) {
        return printLine(new StringBuilder(), len, corner, linechar);
    }

    /**
     * 获得有数量len个lineChar组成的字符串
     *
     * @param tmp      存放结果的StringBuilder对象的引用
     * @param len      字符的个数
     * @param linechar 字符
     */
    public static StringBuilder printLine(StringBuilder tmp, int len, char linechar) {
        for (int i = 0; i < len; i++) {
            tmp.append(linechar);
        }
        tmp.append('\n');
        return tmp;
    }

    /**
     * 返回以corner结束的由len个lineChar字符组成的字符串
     *
     * @param tmp      存放结果的引用
     * @param len      字符的个数
     * @param corner   结束符号
     * @param linechar 字符
     */
    public static StringBuilder printLine(StringBuilder tmp, int len, char corner, char linechar) {
        tmp.append(corner);
        for (int i = 0; i < len; i++) {
            tmp.append(linechar);
        }
        tmp.append(corner);
        tmp.append('\n');
        return tmp;
    }

    /**
     * 输出堆栈异常
     */
    public static String printThrowable(Throwable ex) {
        if (null != ex) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter pw = new PrintWriter(stringWriter);
            ex.printStackTrace(pw);
            return stringWriter.toString();
        }
        return "";
    }

    /**
     * 输出堆栈异常
     */
    public static StringBuilder printThrowable(StringBuilder sb, Throwable ex) {
        if (null != ex) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter pw = new PrintWriter(stringWriter);
            ex.printStackTrace(pw);
            sb.append(stringWriter);
        }
        return sb;
    }

    /**
     * 将字节转换成16进制显示
     */
    private static String _toHex(byte b) {
        char[] buf = new char[2];
        byte bt = b;
        for (int i = 0; i < 2; i++) {
            buf[1 - i] = DIGITS_LOWER[bt & 0xF];
            bt = (byte) (bt >>> 4);
        }
        return new String(buf);
    }

    /**
     * 将hexStr格式化成length长度16进制数，并在后边加上h
     */
    private static StringBuilder _fixHexString(StringBuilder buf, String hexStr, int length) {
        if (hexStr == null || hexStr.length() == 0) {
            buf.append("00000000h");
        } else {
            int strLen = hexStr.length();
            for (int i = 0; i < length - strLen; i++) {
                buf.append("0");
            }
            buf.append(hexStr).append("h");
        }
        return buf;
    }

    /**
     * 过滤掉字节数组中不能显示的ascii码，生成字符串
     */
    private static String _filterString(byte[] bytes, int offset, int count) {
        byte[] buffer = new byte[count];
        System.arraycopy(bytes, offset, buffer, 0, count);
        for (int i = 0; i < count; i++) {
            if (buffer[i] < 0x20 || buffer[i] > 0x7E) {
                buffer[i] = 0x2e;
            }
        }
        return new String(buffer);
    }

    /**
     * 以16进制方式打印字节数组，用于调试二进制的内容
     */
    public static String printHexString(byte[] bytes) {
        if (null == bytes || bytes.length == 0) {
            return "";
        }
        StringBuilder buffer = new StringBuilder(bytes.length);
        int startIndex = 0;
        int column = 0;
        for (int i = 0; i < bytes.length; i++) {
            column = i % 16;
            switch (column) {
            case 0:
                startIndex = i;
                _fixHexString(buffer, Integer.toHexString(i), 8).append(": ");
                buffer.append(_toHex(bytes[i]));
                buffer.append(" ");
                break;
            case 15:
                buffer.append(_toHex(bytes[i]));
                buffer.append(" ");
                buffer.append(_filterString(bytes, startIndex, column + 1));
                buffer.append("\n");
                break;
            default:
                buffer.append(_toHex(bytes[i]));
                buffer.append(" ");
            }
        }
        if (column != 15) {
            for (int i = 0; i < (15 - column); i++) {
                buffer.append("   ");
            }
            buffer.append(_filterString(bytes, startIndex, column + 1));
            buffer.append("\n");
        }
        return buffer.toString();
    }

    /**
     * 如果传入的value为null，则返回defaultValue，否则就返回value
     */
    public static String getString(String value, String defaultValue) {
        return null == value ? defaultValue : value;
    }

    /**
     * 字符串型转换为Boolean
     *
     * @return 字符串为true/y/1时都返回true
     */
    public static Boolean getBool(String value, Boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value.equals("true") || value.equalsIgnoreCase("y") || value.equals("1")) {
            return true;
        }
        return false;
    }

    /**
     * 字符串型转换为Byte
     */
    public static Byte getByte(String value, Byte defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Byte.valueOf(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 字符串转换为Character，默认取字符串的第1个字符
     */
    public static char getChar(String value, Character defaultValue) {
        if (isEmpty(value)) {
            return defaultValue;
        }
        return value.charAt(0);
    }

    /**
     * 字符串转换为Short，注意不能处理小数，会导致返回defaultValue
     */
    public static Short getShort(String value, Short defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Short.valueOf(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 字符串转换为Integer，注意不能处理小数，会导致返回defaultValue
     */
    public static Integer getInt(String value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 字符串转换为Long，注意不能处理小数，会导致返回defaultValue
     */
    public static Long getLong(String value, Long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 字符串转换为Float
     */
    public static Float getFloat(String value, Float defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Float.valueOf(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 字符串转换为Double
     */
    public static Double getDouble(String value, Double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 字符串转换成Character型，直接取第一个字符，如果是空字符串会返回null
     */
    public static Character getChar(String str) {
        return isEmpty(str) ? null : str.charAt(0);
    }

    /**
     * 获得基本类型-基本类型转到String类型的方法的 映射列表
     */
    private static Map<Type, Method> initValueOfMethod() {
        Map<Type, Method> valueOfMethods = new HashMap<Type, Method>();
        try {
            valueOfMethods.put(int.class, Integer.class.getMethod("valueOf", String.class));
            valueOfMethods.put(Integer.class, Integer.class.getMethod("valueOf", String.class));
            valueOfMethods.put(long.class, Long.class.getMethod("valueOf", String.class));
            valueOfMethods.put(Long.class, Long.class.getMethod("valueOf", String.class));
            valueOfMethods.put(float.class, Float.class.getMethod("valueOf", String.class));
            valueOfMethods.put(Float.class, Float.class.getMethod("valueOf", String.class));
            valueOfMethods.put(double.class, Double.class.getMethod("valueOf", String.class));
            valueOfMethods.put(Double.class, Double.class.getMethod("valueOf", String.class));
            valueOfMethods.put(short.class, Short.class.getMethod("valueOf", String.class));
            valueOfMethods.put(Short.class, Short.class.getMethod("valueOf", String.class));
            valueOfMethods.put(boolean.class, Boolean.class.getMethod("valueOf", String.class));
            valueOfMethods.put(Boolean.class, Boolean.class.getMethod("valueOf", String.class));
            valueOfMethods.put(byte.class, Byte.class.getMethod("valueOf", String.class));
            valueOfMethods.put(Byte.class, Byte.class.getMethod("valueOf", String.class));
            valueOfMethods.put(String.class, String.class.getMethod("valueOf", Object.class));
            // 由于Character.valueOf方法的参数为char，对此需要使用一个特殊的方法
            valueOfMethods.put(char.class, StringTools.class.getMethod("getChar", String.class));
            valueOfMethods.put(Character.class, StringTools.class.getMethod("getChar", String.class));
        } catch (Exception e) {
            throw new RuntimeException(e); // 这里有问题一定要抛出来
        }
        return valueOfMethods;
    }

    /**
     * 获取由String类型转到基本类型的方法
     */
    private static Method getValueOfMethod(Type clazz) {
        Method m = valueOfMethod.get(clazz);
        if (m == null) {
            throw classNotBasicTypeException;
        }
        return m;
    }

    /**
     * 将指定类型转化成目标类型
     *
     * @param <T>       泛型
     * @param value     待转换的对象
     * @param destClazz 目标类类型
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static <T> T valueOf(String value, Type destClazz) throws Exception {
        Method method = getValueOfMethod(destClazz);
        return (T) method.invoke(null, value != null ? value.trim() : value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T valueOf(String value, T defaultValue, Class<T> destClazz) throws Exception {
        if (isEmpty(value)) {
            return defaultValue;
        }
        try {
            Method method = getValueOfMethod(destClazz);
            return (T) method.invoke(null, value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 计算输入字符串的md5值（返回长度为32的小写字符串）
     */
    public static String md5AsHex(String str, Charset charset) {
        return Hashing.md5().hashString(str, charset).toString();
    }

    /**
     * 计算输入字符串的md5值（返回长度为32的小写字符串）
     */
    public static String md5AsHex(String str) {
        return Hashing.md5().hashString(str, Charsets.UTF_8).toString();
    }

    /**
     * Base64编码
     *
     * @param str
     * @return
     */
    public static String base64Encode(String str) {
        Base64 b64 = new Base64();
        byte[] b = b64.encode(str.getBytes());
        return new String(b);
    }

    /**
     * Base64解码
     *
     * @param base64EncodedStr
     * @return
     */
    public static String base64Decode(String base64EncodedStr) {
        Base64 b64 = new Base64();
        byte[] b = b64.decode(base64EncodedStr.getBytes());
        return new String(b);
    }

    /**
     * 计算输入字符串的sha1值（返回长度为40的小写字符串）
     */
    public static String sha1AsHex(String str, Charset charset) {
        return Hashing.sha1().hashString(str, charset).toString();
    }

    /**
     * 计算输入字符串的sha1值（返回长度为40的小写字符串）
     */
    public static String sha1AsHex(String str) {
        return Hashing.sha1().hashString(str, Charsets.UTF_8).toString();
    }

    /**
     * 按照首字母大写规则进行分词，如HelloWorld，就会被分为Hello和World两个词，REST就会被分为R E S T四个词
     */
    public static String[] splitByUppercaseLetter(String str) {
        List<String> split = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (Character.isUpperCase(ch) && sb.length() > 0) {
                split.add(sb.toString());
                sb = new StringBuilder();
            }
            sb.append(ch);
        }
        if (sb.length() > 0) {
            split.add(sb.toString());
        }
        return split.toArray(new String[1]);
    }

    /**
     * 将传入字符串的首字母小写，如果第2个字母也是大写首字母就不转为小写了（跟Spring的beanName规则保持一致，这也是java的get/set方法名生成规则）
     */
    public static String headLetterToLowerCase(String str) {
        if (isNotEmpty(str)) {
            if (str.length() == 1) {
                return str.toLowerCase(Locale.getDefault());
            } else if (str.charAt(1) >= 'A' && str.charAt(1) <= 'Z') {
                return str;
            }
            return Character.toLowerCase(str.charAt(0)) + str.substring(1);
        }
        return "";
    }

    /**
     * 将传入字符串的首字母大写，如果第2个字母也是大写首字母就不转为大写了（跟Spring的beanName规则保持一致，这也是java的get/set方法名生成规则）
     */
    public static String headLetterToUpperCase(String str) {
        if (isNotEmpty(str)) {
            if (str.length() == 1) {
                return str.toUpperCase(Locale.getDefault());
            } else if (str.charAt(1) >= 'A' && str.charAt(1) <= 'Z') {
                return str;
            }
            return Character.toUpperCase(str.charAt(0)) + str.substring(1);
        }
        return "";
    }

    /**
     * 统计一个字符在另一个字符串里面出现的次数
     */
    public static int count(String src, char find) {
        int c = 0;
        for (int i = 0; i < src.length(); i++) {
            if (src.charAt(i) == find) {
                c++;
            }
        }
        return c;
    }

    /**
     * 统计一个字符在另一个字符串里面出现的次数
     */
    public static int count(char[] src, char find) {
        int c = 0;
        for (int i = 0; i < src.length; i++) {
            if (src[i] == find) {
                c++;
            }
        }
        return c;
    }

    /**
     * 处理 /test///// 这样在后面加了/的情况，删除多余的/，这个例子会返回 /test，如果只传入/就返回/，传入//还是返回/
     */
    public static String removePathSlash(String pathInfo) {
        if (pathInfo.endsWith("/")) {
            int i = pathInfo.length() - 1;
            while (i > 0 && pathInfo.charAt(i) == '/') {
                i--;
            }
            return pathInfo.substring(0, i + 1);
        }
        return pathInfo;
    }

    /**
     * 生成预备转义的字符串数组列表
     */
    private static String[] _buildEscapeArray() {
        String[] ret = new String['\u00a0' + 1]; // 最大的需要转义的就是\u00a0 （WEB提交的空格）
        for (int i = 0; i < ret.length; i++) {
            ret[i] = "" + (char) i;
        }
        return ret;
    }

    /**
     * 处理字符串的html转义字符
     */
    public static String escapeHtml(String source) {
        if (isNotEmpty(source)) {
            StringBuilder sb = new StringBuilder(source.length() + 16);
            for (int i = 0; i < source.length(); i++) {
                char ch = source.charAt(i);
                if (ch < HTML_ESCAPE_LIST.length) {
                    String append = HTML_ESCAPE_LIST[ch];
                    sb.append(append);
                } else {
                    sb.append(ch);
                }
            }
            return sb.toString();
        }
        return "";
    }

    /**
     * 处理字符串的xml转义字符
     */
    public static String escapeXml(String source) {
        if (isNotEmpty(source)) {
            StringBuilder sb = new StringBuilder(source.length() + 16);
            for (int i = 0; i < source.length(); i++) {
                char ch = source.charAt(i);
                if (ch < XML_ESCAPE_LIST.length) {
                    String append = XML_ESCAPE_LIST[ch];
                    sb.append(append);
                } else {
                    sb.append(ch);
                }
            }
            return sb.toString();
        }
        return "";
    }

    /**
     * 转义为""中可用的字符串，诸如\n \t等将会被转义
     */
    public static String escapeQuotedString(String source) {
        if (isNotEmpty(source)) {
            StringBuilder sb = new StringBuilder(source.length() + 16);
            for (int i = 0; i < source.length(); i++) {
                char ch = source.charAt(i);
                if (ch < QUOTED_STRING_ESCAPE_LIST.length) {
                    String append = QUOTED_STRING_ESCAPE_LIST[ch];
                    sb.append(null != append ? append : ch);
                } else {
                    sb.append(ch);
                }
            }
            return sb.toString();
        }
        return "";
    }

    /**
     * 转义字符串中的空白字符，包括\r \n \t等
     */
    public static String escapeWhitespace(String source) {
        if (isNotEmpty(source)) {
            StringBuilder sb = new StringBuilder(source.length() + 16);
            for (int i = 0; i < source.length(); i++) {
                char ch = source.charAt(i);
                if (ch < WHITESPACE_ESCAPE_LIST.length) {
                    String append = WHITESPACE_ESCAPE_LIST[ch];
                    sb.append(null != append ? append : ch);
                } else {
                    sb.append(ch);
                }
            }
            return sb.toString();
        }
        return "";
    }

    /**
     * <pre>
     * 转义sql的like语句部分，防注入
     * 如果想查询 john开头的name，sql写法为 where name like 'john%'
     * 使用{@link PreparedStatement}来写，就是写成 where name like ?
     * 然后?中传参john%
     * 如果不是john而是包含%或_的字符串，就必须要进行转义，否则有被注入的风险
     * 本方法就用于生成?的那部分内容，写法为StringTools.escapeSqlLikePattern("john")+"%"
     * </pre>
     */
    public static String escapeSqlLikePattern(String keyword) {
        if (isEmpty(keyword)) {
            return "";
        }
        StringBuilder sb = new StringBuilder(keyword.length());
        for (int i = 0; i < keyword.length(); i++) {
            char ch = keyword.charAt(i);
            if (ch < SQL_LIKE_PATTERN.length) {
                String append = SQL_LIKE_PATTERN[ch];
                sb.append(null != append ? append : ch);
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /**
     * 对传入的字符串进行截断，如果长度超过length就截取前length个字符的子串，如果小于length就原样返回
     */
    public static String truncate(String source, int length) {
        if (isEmpty(source) || length <= 0) {
            return source;
        }
        return source.length() > length ? source.substring(0, length) : source;
    }

    /**
     * 在一个已有的URL上添加参数
     */
    public static String addParamsToUrl(String url, Object... keyValues) {
        Map<String, Object> map = new DbMap(keyValues);

        String urlHref = url;
        String anchor = "";

        // 如果有锚点,先取出,后补.
        int anchorPos = url.indexOf("#");
        if (anchorPos > 0) {
            urlHref = url.substring(0, anchorPos);
            anchor = url.substring(anchorPos);
        }

        boolean first = true;// 用于第一次判断链接上是否已有问号

        for (Entry<String, Object> e : map.entrySet()) {
            if (first && urlHref.indexOf("?") < 0) {
                // 如果是第一个参数,然后链接中没有含有问号,那要补问号
                try {
                    urlHref += "?" + e.getKey() + "=" + URLEncoder.encode(String.valueOf(e.getValue()), Charsets.UTF_8.name());
                } catch (UnsupportedEncodingException e1) {
                }
            } else {
                try {
                    urlHref += "&" + e.getKey() + "=" + URLEncoder.encode(String.valueOf(e.getValue()), Charsets.UTF_8.name());
                } catch (UnsupportedEncodingException e1) {
                }
            }
            first = false;
        }
        return urlHref += anchor;
    }

    /**
     * 将若干个对象存入StringBuilder中，起字符串连接作用
     *
     * @param tmp  StringBuilder对象的引用
     * @param args 对象列表
     */
    public static StringBuilder append(StringBuilder tmp, Object... args) {
        for (Object s : args) {
            tmp.append(s);
        }
        return tmp;
    }

    public static String digestString(String src) {
        return digestString(src, 50);
    }

    public static String digestString(String src, int lengthThreshold) {
        if (src.length() > lengthThreshold * 2 + 20) {
            return src.substring(0, lengthThreshold) + "...(" + src.length() + ")..." + src.substring(src.length() - lengthThreshold, src.length());
        }
        return src;
    }

    public static String getAlertJs(String msg) {
        return "alert(\"" + msg + "\");";
    }

    public static String getAddCookieJs(String key, Object value, int maxAge) {
        return "document.cookie = \"" + key + " = \" + escape(\"" + value.toString() + "\") + \"; expires=\" + new Date(new Date().getTime() + maxAge * 1000).toGMTString();";
    }

    public static String getDelCookieJs(String key) {
        return "document.cookie = \"" + key + " =; expires = \" + " + "new Date(0).toGMTString();";
    }

    public static String getJumpJs(String href) {
        return "document.location.href = \"" + href + "\";";
    }

    public static String getJumpTopJs(String href) {
        return "parent.location.href = \"" + href + "\";";
    }

    public static String getBackJs() {
        return "window.history.go(-1);";
    }

    /**
     * 生成一条超链接，用于文本消息使用，参数部分会自动进行编码处理
     */
    public static String getLinkHtml(String text, String url, Object... args) {
        String href = addParamsToUrl(url, args);
        return "<a href=\"" + href + "\">" + escapeHtml(text) + "</a>";
    }

    /**
     * 字符编码集合
     */
    public static class Charsets {

        public static final Charset UTF_8 = Charset.forName("UTF-8");
        public static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");
        public static final Charset GBK = Charset.forName("GBK");
        public static final Charset GB18030 = Charset.forName("GB18030");
        public static final Charset BIG5 = Charset.forName("BIG5");
    }

    /**
     * 下划线分割命名转成驼峰命名
     * @return
     */
    public static String underScoreToCamelName(String name) {
        if (isEmpty(name)) {
            return name;
        }
        String[] p = name.split("_");
        if (p.length > 1) {
            StringBuilder camel = new StringBuilder();
            camel.append(p[0]);
            for (int i = 1; i < p.length; i++) {
                camel.append(headLetterToUpperCase(p[i]));
            }
            return camel.toString();
        }else {
            return name;
        }

    }

    /**
     * 驼峰命名转成下划线命名
     * @return
     */
    public static String camelToUnderScoreName(String name) {
        if (isEmpty(name)) {
            return name;
        }
        String[] p = splitByUppercaseLetter(name);
        if (p.length > 1) {
            StringBuilder underScore = new StringBuilder();
            underScore.append(p[0]);
            for (int i = 1; i < p.length; i++) {
                underScore.append("_");
                underScore.append(headLetterToLowerCase(p[i]));
            }
            return underScore.toString();
        }else {
            return headLetterToLowerCase(name);
        }
    }
}
