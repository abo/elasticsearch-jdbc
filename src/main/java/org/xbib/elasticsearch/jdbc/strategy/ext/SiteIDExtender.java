package org.xbib.elasticsearch.jdbc.strategy.ext;

import com.google.common.base.*;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 2015/9/15.
 */
public class SiteIDExtender implements FieldExtender {

    public static final String NAME = "siteid_extender";

    private final static Logger logger = LogManager.getLogger(SiteIDExtender.class);

    private int cache_max_size = 300;
    private int cache_expire_minutes = 60 * 24;

    private String url;
    private String username;
    private String password;
    private String sql;
    private List<String> sqlParams;

    private LoadingCache<Sequence<Object>, Map<String,Object>> cache = null;


    @Override
    public void setParams(Map<String, Object> params) {
        this.url = (String) params.get("url");
        this.username = (String) params.get("username");
        this.password = (String) params.get("password");
        this.sql = (String) params.get("sql");
        this.sqlParams =(List<String>) params.get("parameter");
    }

    @Override
    public Map<String, Object> extend( List<String> originalKeys,  List<Object> originalValues) {
        if(cache == null){
            cache = CacheBuilder.newBuilder().maximumSize(cache_max_size)
                    .expireAfterWrite(cache_expire_minutes, TimeUnit.MINUTES)
                    .build(new CacheLoader<Sequence<Object>, Map<String, Object>>() {
                        @Override
                        public Map<String, Object> load(Sequence<Object> params) throws Exception {
                            Connection connection = null;
                            PreparedStatement statement = null;
                            ResultSet results = null;
                            try {
                                connection = DriverManager.getConnection(url, username, password);
                                statement = connection.prepareStatement(sql);

                                for (int i = 0; i < params.content.size(); i++) {
                                    SQLUtil.bind(statement, i + 1, params.content.get(i));
                                }

                                results = statement.executeQuery();
                                ResultSetMetaData metadata = results.getMetaData();
                                if (results.first()) {
                                    ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder();
                                    for (int i = 1; i <= metadata.getColumnCount(); i++) {
                                        String key = metadata.getColumnLabel(i);
                                        Object value = SQLUtil.parseType(results, i, metadata.getColumnType(i));
                                        builder.put(key, value);
                                    }
                                    return builder.build();
                                }
                            } finally {
                                close(connection, statement, results);
                            }

                            return Collections.emptyMap();
                        }
                    });
        }

        Sequence<Object> key = new Sequence<Object>();
        for(int i = 0 ; i < sqlParams.size() ; i ++ ){
            Object val = originalValues.get(originalKeys.indexOf(sqlParams.get(i)));
            key.add(val);
        }

        try {
            return cache.get(key);
        } catch (ExecutionException e) {
            logger.error("fail to extend by key:" + key, e);
            return Collections.emptyMap();
        }
    }


    private void close(Connection connection, Statement statement, ResultSet results){
        try {
            if (results != null && !results.isClosed()) {
                results.close();
            }

            if (statement != null && !statement.isClosed()) {
                statement.close();
            }

            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        }catch(SQLException e){
            logger.error("close connection cause exception:", e);
        }
    }


    class Sequence<T>{
        List<T> content = new ArrayList<T>();

        public Sequence add(T item){
            content.add(item);
            return this;
        }

        @Override
        public boolean equals(Object obj) {
            if(!(obj instanceof Sequence)){
                return false;
            }
            return Arrays.equals(content.toArray(), ((Sequence)obj).content.toArray());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(content.toArray());
        }

        @Override
        public String toString() {
            MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this);
            for( T t : content){
                helper.addValue(t);
            }
            return helper.toString();
        }
    }
}
