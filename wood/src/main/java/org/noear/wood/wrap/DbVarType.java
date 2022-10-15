package org.noear.wood.wrap;

/**
 * Created by noear on 14-6-12.
 * 数据变量类型
 */
public enum DbVarType {
    //
    // 摘要:
    //     未知或未定义。
    Unknown,

    //
    // 摘要:
    //     变量。
    IVariate,

    //
    // 摘要:
    //     二进制数据的可变长度流，范围在 1 到 8,000 个字节之间。
    Binary,
    //
    // 摘要:
    //     一个 8 位无符号整数，范围在 0 到 255 之间。
    Byte,
    //
    // 摘要:
    //     简单类型，表示 true 或 false 的布尔值。
    Boolean,
    //
    // 摘要:
    //     货币值，范围在 -2 63（即 -922,337,203,685,477.5808）到 2 63 -1（即 +922,337,203,685,477.5807）之间，精度为千分之十个货币单位。
    Currency,
    //
    // 摘要:
    //     表示日期值的类型。
    Date,
    //
    // 摘要:
    //     表示一个日期和时间值的类型。
    DateTime,
    //
    // 摘要:
    //     简单类型，表示从 1.0 x 10 -28 到大约 7.9 x 10 28 且有效位数为 28 到 29 位的值。
    Decimal,
    //
    // 摘要:
    //     浮点型，表示从大约 5.0 x 10 -324 到 1.7 x 10 308 且精度为 15 到 16 位的值。
    Double,
    //
    // 摘要:
    //     全局唯一标识符（或 GUID）。
    Guid,
    //
    // 摘要:
    //     整型，表示值介于 -32768 到 32767 之间的有符号 16 位整数。
    Int16,
    //
    // 摘要:
    //     整型，表示值介于 -2147483648 到 2147483647 之间的有符号 32 位整数。
    Int32,
    //
    // 摘要:
    //     整型，表示值介于 -9223372036854775808 到 9223372036854775807 之间的有符号 64 位整数。
    Int64,
    //
    // 摘要:
    //     常规类型，表示任何没有由其他 DbType 值显式表示的引用或值类型。
    Object,
    //
    // 摘要:
    //     整型，表示值介于 -128 到 127 之间的有符号 8 位整数。
    SByte,
    //
    // 摘要:
    //     浮点型，表示从大约 1.5 x 10 -45 到 3.4 x 10 38 且精度为 7 位的值。
    Single,
    //
    // 摘要:
    //     表示 Unicode 字符串的类型。
    String,
    //
    // 摘要:
    //     一个表示 SQL Server DateTime 值的类型。如果要使用 SQL Server time 值，请使用 System.Data.SqlDbType.Time。
    Time,
    //
    // 摘要:
    //     整型，表示值介于 0 到 65535 之间的无符号 16 位整数。
    UInt16,
    //
    // 摘要:
    //     整型，表示值介于 0 到 4294967295 之间的无符号 32 位整数。
    UInt32,
    //
    // 摘要:
    //     整型，表示值介于 0 到 18446744073709551615 之间的无符号 64 位整数。
    UInt64,
    //
    // 摘要:
    //     变长数值。
    VarNumeric,
    //
    // 摘要:
    //     非 Unicode 字符的固定长度流。
    AnsiStringFixedLength,
    //
    // 摘要:
    //     Unicode 字符的定长串。
    StringFixedLength ,
    //
    // 摘要:
    //     XML 文档或片段的分析表示。
    Xml,
    //
    // 摘要:
    //     日期和时间数据。日期值范围从公元 1 年 1 月 1 日到公元 9999 年 12 月 31 日。时间值范围从 00:00:00 到 23:59:59.9999999，精度为
    //     100 毫微秒。
    DateTime2,
    //
    // 摘要:
    //     显示时区的日期和时间数据。日期值范围从公元 1 年 1 月 1 日到公元 9999 年 12 月 31 日。时间值范围从 00:00:00 到 23:59:59.9999999，精度为
    //     100 毫微秒。时区值范围从 -14:00 到 +14:00。
    DateTimeOffset,
}
