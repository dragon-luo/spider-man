package xzf.spiderman.worker.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.beans.BeanUtils;
import xzf.spiderman.worker.data.SaveSpiderStoreReq;
import xzf.spiderman.worker.data.SpiderStoreData;

import javax.persistence.*;

@Data
@NoArgsConstructor
@ToString
@Entity
@Table(name = "spider_store")
public class SpiderStore
{
    public static final int TYPE_MONGO = 1;
    public static final int TYPE_ES = 2;


    public static final int URL_TYPE_DB_CONNECT = 1;
    public static final int URL_TYPE_NACOS_URI = 2;


    @Id
    @Column(name = "spider_store_id")
    private String id;

    @Column(name = "host")
    private String host;

    @Column(name = "port")
    private Integer port;

    @Column(name = "database")
    private String database;

    @Column(name = "table_name")
    private String tableName;

    @Column(name = "type")
    private Integer type;

    @Column(name = "url")
    private String url;

    @Column(name = "username")
    private String username ;

    @Column(name = "password")
    private String password;

    @Column(name = "url_type")
    private Integer urlType;



    public static SpiderStore create(SaveSpiderStoreReq req)
    {
        SpiderStore ret = new SpiderStore();
        BeanUtils.copyProperties(req,ret);
        return ret;
    }


    public SpiderStoreData asData()
    {
        SpiderStoreData ret = new SpiderStoreData();
        BeanUtils.copyProperties(this, ret);
        return ret;
    }

    public void update(SaveSpiderStoreReq req) {
        BeanUtils.copyProperties(req,this);
    }
}
