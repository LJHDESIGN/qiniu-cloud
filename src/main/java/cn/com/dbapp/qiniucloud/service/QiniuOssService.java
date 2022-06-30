package cn.com.dbapp.qiniucloud.service;


import cn.com.dbapp.qiniucloud.vo.FileVo;
import cn.com.dbapp.qiniucloud.vo.GetFileParam;

public interface QiniuOssService {

    /**
     * 文件上传
     * @param request 参数
     * @return 结果
     */
    boolean uploadFile(FileVo request);

    /**
     * 获取七牛云文件地址
     * @param param param
     * @return 文件url
     */
    String getFileUrl(GetFileParam param);

    /**
     * 获取文件信息
     * @param param
     * @return
     */
    String getFileInfo(GetFileParam param);

    /**
     * 获取压缩文件
     * @param param
     * @return
     */
    String downloadZipUrl(GetFileParam param);
}
