# 编译

* maven

编译打包

    mvn clean && maven package
    mvn package -Pdist

dist/elastticsearch-jdbc-1.7.0.1-dist.zip 即为打包结果

* ant

    ant dist

dist/elastticsearch-jdbc-1.7.0.1-dist.zip 即为打包结果


# 运行

解压 elastticsearch-jdbc-1.7.0.1-dist.zip

根据部署环境修改导入配置文件（bin/gensee/下，confonlineusers.json对应confonlineusers表，其他类似），如userstat.json（配置项说明见readme.md），主要修改内容：

* jdbc 修改为待导入mysql地址和账号

* row_max 修改join_type为导入起始点，即导入join_type大于等于设定值的记录，如 1970-01-01 00:00:00

* row_extend 修改为 conf_id -> site_id 映射关系表所在的mysql地址

* elasticsearch.cluster，elasticsearch.host，elasticsearch.port 修改为elasticsearch的地址，默认为本机

* index，type 修改为数据在elasticsearch中的索引名和类型名


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

    bin/import.sh bin/gensee/userstat.json