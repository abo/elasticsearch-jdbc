package org.xbib.elasticsearch.jdbc.strategy.ext;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2015/9/15.
 */
public interface FieldExtender {

    void setParams(Map<String,Object> params);

    Map<String, Object> extend(List<String> originalKeys, List<Object> originalValues);

}
