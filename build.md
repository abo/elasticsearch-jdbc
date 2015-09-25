# 编译

* maven

编译打包

    mvn clean && maven package
    mvn package -Pdist

dist/elastticsearch-jdbc-1.7.0.1-dist.zip 即为打包结果

* ant



# 运行

解压 elastticsearch-jdbc-1.7.0.1-dist.zip， 修改 bin/import.sh 中 JDBC_IMPORTER_HOME 为解压地址，如

    JDBC_IMPORTER_HOME=/home/kevin/Desktop/elasticsearch-jdbc-1.7.0.1

根据部署环境编写导入配置文件，如userstat.json（配置项说明见readme.md）：

    {
        "type" : "jdbc",
        "jdbc" : {
            "url" : "jdbc:mysql://127.0.0.1:3306/webcastingraw",
            "user" : "root",
            "password" : "",
            "sql" : [{
                "statement":"select *,substring(area FROM 1 FOR 2) as location,concat(conf_id,'.',user_id,'.',unix_timestamp(join_time)) as _id, date_format(convert_tz(join_time,'+00:00','+08:00'), '%Y-%m-%dT%T.000+08:00') as join_time_local from userstat where join_time >= ?",
                "parameter":["$max.join_time"]
            }],
        "row_max":{
            "join_time":"2015-07-25 00:00:00"
        },
        "timezone":"GMT",
        "row_extend":{
            "type":"siteid_extender",
            "url":"jdbc:mysql://127.0.0.1:3306/webcast",
            "username":"root",
            "password":"",
            "sql":"select site_id from gs_webcast_published where id=?",
            "parameter":["conf_id"]
        },
        "resultset_concurrency":"CONCUR_READ_ONLY",
        "elasticsearch.cluster":"webdevstat",
        "index": "mytest",
        "type": "userstat"
        }
    }

运行

    bin/import.sh userstat.json