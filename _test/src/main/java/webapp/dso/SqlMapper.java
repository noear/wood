package webapp.dso;

import java.math.*;
import java.sql.SQLException;
import java.time.*;
import java.util.*;

import org.noear.wood.BaseMapper;
import org.noear.wood.DataItem;
import org.noear.wood.DataList;
import org.noear.wood.annotation.Db;
import org.noear.wood.xml.Namespace;

import webapp.model.AppxModel;

@Namespace("webapp.dso.SqlMapper")
public interface SqlMapper{
    //随便取条数据的ID
    int appx_get() throws SQLException;

    //根据id取条数据
    AppxModel appx_get2(int app_id) throws SQLException;

    //取一批ID
    Map<String,Object> appx_get3(String tb, int app_id) throws SQLException;

    List<AppxModel> appx_getlist(int app_id) throws SQLException;

    List<AppxModel> appx_getlist_byid(int app_id) throws SQLException;

    List<Integer> appx_getids() throws SQLException;
}
