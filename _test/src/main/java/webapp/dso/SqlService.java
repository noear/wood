package webapp.dso;

import java.sql.SQLException;
import java.util.*;

import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import webapp.model.AppxModel;

@Component
public class SqlService{
    @Inject
    webapp.dso.SqlMapper mapper;

    //随便取条数据的ID
    public int appx_get() throws SQLException{
        return mapper.appx_get();
    }

    //根据id取条数据
    public AppxModel appx_get2(int app_id) throws SQLException{
        return mapper.appx_get2(app_id);
    }

    //取一批ID
    public Map<String,Object> appx_get3(String tb, int app_id) throws SQLException{
        return mapper.appx_get3(tb,app_id);
    }

    //取一批ID
    public Map<String,Object> appx_get4(String tb, int app_id) throws SQLException {
        Map<String, Object> map = new HashMap<>();
        map.put("tb", tb);
        map.put("app_id", app_id);

        return mapper.appx_get4(map);
    }

    public List<AppxModel> appx_getlist(int app_id) throws SQLException{
        return mapper.appx_getlist(app_id);
    }

    public List<Integer> appx_getids() throws SQLException{
        return mapper.appx_getids();
    }
}
