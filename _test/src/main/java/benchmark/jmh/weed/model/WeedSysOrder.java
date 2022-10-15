package benchmark.jmh.weed.model;

import lombok.Data;
import org.noear.wood.annotation.PrimaryKey;
import org.noear.wood.annotation.Table;

@Data
@Table("sys_order")
public class WeedSysOrder {
    @PrimaryKey
    private Integer id;
    private String name;
    private Integer customerId;
}
