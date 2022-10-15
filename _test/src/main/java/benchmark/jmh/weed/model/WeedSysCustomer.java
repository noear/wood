package benchmark.jmh.weed.model;


import lombok.Data;
import org.noear.wood.annotation.PrimaryKey;
import org.noear.wood.annotation.Table;

@Data
@Table("sys_customer")
public class WeedSysCustomer {
    @PrimaryKey
    private Integer id;
    private String code;
    private String name;

    //@FetchMany("customerId")
    //private List<BeetlSysOrder> order;
}
