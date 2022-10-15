package benchmark.jmh.wood.model;

import lombok.Data;
import org.noear.wood.annotation.PrimaryKey;
import org.noear.wood.annotation.Table;

@Data
@Table("sys_order")
public class WoodSysOrder {
    @PrimaryKey
    private Integer id;
    private String name;
    private Integer customerId;
}
