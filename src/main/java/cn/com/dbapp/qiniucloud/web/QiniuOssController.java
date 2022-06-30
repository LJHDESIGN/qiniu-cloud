package cn.com.dbapp.qiniucloud.web;

import cn.com.dbapp.qiniucloud.service.QiniuOssService;
import cn.com.dbapp.qiniucloud.vo.FileVo;
import cn.com.dbapp.qiniucloud.vo.GetFileParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author linjianhui
 * @description
 * @date 2022/6/14 2:35 下午
 */
@RestController
@RequestMapping({"/qiniu/oss"})
public class QiniuOssController {

    @Resource
    private QiniuOssService qiniuOssService;

    @PostMapping(value = "/upload-file")
    @ResponseBody
    public boolean uploadFile(FileVo request){
        return qiniuOssService.uploadFile(request);
    }

    @GetMapping(value = "/getFileUrl")
    @ResponseBody
    public String getFileUrl(@RequestParam String key){

        GetFileParam param = new GetFileParam();
        param.setKey(key);

        return qiniuOssService.getFileUrl(param);
    }

    @GetMapping(value = "/getFileInfo")
    @ResponseBody
    public String getFileInfo(@RequestParam String key){

        GetFileParam param = new GetFileParam();
        param.setKey(key);

        return qiniuOssService.getFileInfo(param);
    }

    @GetMapping(value = "/downloadZipUrl")
    @ResponseBody
    public String downloadZipUrl(@RequestParam String key){

        GetFileParam param = new GetFileParam();
        param.setKey(key);

        return qiniuOssService.downloadZipUrl(param);
    }
}
