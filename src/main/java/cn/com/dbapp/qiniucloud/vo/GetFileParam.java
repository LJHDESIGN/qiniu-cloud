package cn.com.dbapp.qiniucloud.vo;

import lombok.Data;

/**
 * @author linjianhui
 * @description
 * @date 2022/6/14 3:15 下午
 */
@Data
public class GetFileParam {

    /**
     * 下载资源在七牛云存储的 key
     */
    private String key;
}
