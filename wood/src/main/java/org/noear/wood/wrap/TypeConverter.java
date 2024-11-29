package org.noear.wood.wrap;

import org.noear.wood.utils.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.*;

public class TypeConverter {
    /**
     * 转换数据
     */
    public Object convert(Object val, Class<?> target) throws SQLException, IOException {
        if (val instanceof Number) {
            Number number = (Number) val;

            if (Long.class == target || Long.TYPE == target) {
                return number.longValue();
            }

            if (Integer.class == target || Integer.TYPE == target) {
                return number.intValue();
            }

            if (Short.class == target || Short.TYPE == target) {
                return number.shortValue();
            }

            if (Double.class == target || Double.TYPE == target) {
                return number.doubleValue();
            }

            if (Float.class == target || Float.TYPE == target) {
                return number.floatValue();
            }

            if (Boolean.class == target || Boolean.TYPE == target) {
                return number.intValue() > 0;
            }

            if (Date.class == target) {
                return new Date(number.longValue());
            }

            if (LocalDateTime.class == target) {
                return new Timestamp(number.longValue()).toLocalDateTime();
            }
        }

        if (target == String.class) {
            if (val instanceof Blob) {
                return IOUtils.transferToString(((Blob) val).getBinaryStream());
            }

            if (val instanceof Clob) {
                return IOUtils.transferToString(((Clob) val).getAsciiStream());
            }
        }

        if (target == InputStream.class) {
            if (val instanceof Blob) {
                return ((Blob) val).getBinaryStream();
            }

            if (val instanceof Clob) {
                return ((Clob) val).getAsciiStream();
            }
        }

        if (target == java.util.Date.class) {
            if (val instanceof String) {
                return Timestamp.valueOf((String) val);
            }

            if (val instanceof Long) {
                return new Timestamp((Long) val);
            }

            if (val instanceof LocalDateTime) {
                return Date.from(((LocalDateTime) val).atZone(ZoneId.systemDefault()).toInstant());
            }

            if (val instanceof LocalDate) {
                return Date.from(((LocalDate) val).atStartOfDay(ZoneId.systemDefault()).toInstant());
            }
        }

        if (target == LocalDateTime.class) {
            if (val instanceof java.sql.Timestamp) {
                return ((Timestamp) val).toLocalDateTime();
            }

            if (val instanceof String) {
                return LocalDateTime.parse((String) val);
            }
        }

        if (target == LocalDate.class) {
            if (val instanceof java.sql.Date) {
                return ((Date) val).toLocalDate();
            }

            if (val instanceof java.sql.Timestamp) {
                return ((Timestamp) val).toLocalDateTime().toLocalDate();
            }

            if (val instanceof String) {
                return LocalDate.parse((String) val);
            }
        }

        if (target == LocalTime.class) {
            if (val instanceof java.sql.Time) {
                return ((Time) val).toLocalTime();
            }

            if (val instanceof String) {
                return LocalTime.parse((String) val);
            }
        }

        if (target == Boolean.TYPE) {
            if (val instanceof Boolean) {
                return val;
            }

            if (val instanceof Number) {
                return ((Number) val).byteValue() > 0;
            }
        }

        return val;
    }

    /**
     * 填充数据
     */
    public void filling(PreparedStatement ps, int idx, Object val) throws SQLException {
        if (val == null) {
            ps.setNull(idx, Types.NULL);
        } else if (val instanceof java.util.Date) {
            if (val instanceof java.sql.Date) {
                ps.setDate(idx, (java.sql.Date) val);
            } else if (val instanceof java.sql.Timestamp) {
                ps.setTimestamp(idx, (java.sql.Timestamp) val);
            } else {
                java.util.Date v1 = (java.util.Date) val;
                ps.setTimestamp(idx, new java.sql.Timestamp(v1.getTime()));
            }
        } else {
            ps.setObject(idx, val);
        }
    }
}
