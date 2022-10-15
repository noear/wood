package benchmark.jmh.weed.model;

import lombok.Data;
import org.noear.wood.annotation.PrimaryKey;
import org.noear.wood.annotation.Table;

@Data
@Table("sys_user")
public class WeedSQLSysUser {
    @PrimaryKey
    private Integer id ;
    private String code ;
}
