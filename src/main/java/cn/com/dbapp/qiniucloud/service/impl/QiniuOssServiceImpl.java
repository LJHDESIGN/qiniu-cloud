package cn.com.dbapp.qiniucloud.service.impl;

import cn.com.dbapp.qiniucloud.constant.QiniuConfig;
import cn.com.dbapp.qiniucloud.service.QiniuOssService;
import cn.com.dbapp.qiniucloud.vo.FileVo;
import cn.com.dbapp.qiniucloud.vo.GetFileParam;
import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.processing.OperationManager;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.DownloadUrl;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.storage.model.FileListing;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import com.qiniu.util.UrlSafeBase64;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author linjianhui
 * @description
 * @date 2022/6/14 2:39 下午
 */
@Slf4j
@Service
public class QiniuOssServiceImpl implements QiniuOssService {



    @Override
    public boolean uploadFile(FileVo request) {
        if (Objects.isNull(request) || Objects.isNull(request.getFile())){
            return false;
        }
        System.out.println("begin："+ System.currentTimeMillis() );
        //构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region2());
        cfg.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2;// 指定分片上传版本
        cfg.resumableUploadMaxConcurrentTaskCount = 8;  // 设置分片上传并发，1：采用同步上传；大于1：采用并发上传
        //设置https
        //cfg.useHttpsDomains = true;

        //设置文件名，默认不指定key的情况下，以文件内容的hash值作为文件名
        String fileName = request.getFile().getOriginalFilename();

        String key = "resource/test" + UUID.randomUUID() + "/" + fileName;
        try {
            //文件流
//            InputStream inputStream = request.getFile().getInputStream();

            byte [] bytes = request.getFile().getBytes();
//            byte[] uploadBytes = toByteArray(inputStream);

//            String localTempDir = Paths.get(System.getenv("java.io.tmpdir"), QINIU_BUCKET).toString();
//
//            //设置断点续传文件进度保存目录
//            FileRecorder fileRecorder = new FileRecorder(localTempDir);
            UploadManager uploadManager = new UploadManager(cfg);


            //生成上传凭证，然后准备上传
            Auth auth = Auth.create(QiniuConfig.QINIU_AK, QiniuConfig.QINIU_SK);
            String upToken = auth.uploadToken(QiniuConfig.QINIU_BUCKET);

            try {
//                Response response = uploadManager.put(inputStream, key, upToken,null,null);
                Response response = uploadManager.put(bytes, key, upToken);


                //解析上传成功的结果
                DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
                System.out.println("end："+ System.currentTimeMillis() );

                System.out.println("文件名称 key：" + putRet.key);
                System.out.println("文件hash值：" + putRet.hash);
            } catch (QiniuException ex) {
                Response r = ex.response;
                System.err.println(r.toString());
                try {
                    System.err.println(r.bodyString());
                } catch (QiniuException ex2) {
                    //ignore
                }
            }
        } catch (UnsupportedEncodingException ex) {

            //ignore
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private byte[] toByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        long count = 0L;
        int n;
        byte [] buffer = new byte[4096];
        while (-1 != (n = inputStream.read(buffer))){
            outputStream.write(buffer, 0, n);
            count += n;
        }

        int result = count > 2147483647L ? -1 : (int) count;
        System.out.println("toByteArray count：" + result);
        return outputStream.toByteArray();
    }

    @Override
    public String getFileUrl(GetFileParam param){
        if (StringUtils.isBlank(param.getKey())){
            return "key is null";
        }
        String urlString = null;
        try {

            // domain 下载 domain, eg: qiniu.com【必须】
            // useHttps 是否使用 https【必须】
            // key 下载资源在七牛云存储的 key【必须】
            DownloadUrl url = new DownloadUrl(QiniuConfig.QINIU_DOMAIN, true, param.getKey());
//            url.setAttname(URLEncoder.encode("attname.docx", "utf-8"))
//                .setFop(fop) // 配置 fop
//                .setStyle(style, styleSeparator, styleParam); // 配置 style

            Auth auth = Auth.create(QiniuConfig.QINIU_AK, QiniuConfig.QINIU_SK);

            //链接过期时间
            long deadline = System.currentTimeMillis() / 1000 + 3600;

            urlString = url.buildURL(auth, deadline);

            System.out.println("下载链接：" + urlString);

        } catch (QiniuException e) {
            e.printStackTrace();
        }

        return urlString;
    }

    @Override
    public String getFileInfo(GetFileParam param) {
        //构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region2());

        //...其他参数参考类注释
        Auth auth = Auth.create(QiniuConfig.QINIU_AK, QiniuConfig.QINIU_SK);
        BucketManager bucketManager = new BucketManager(auth, cfg);

        try {
            FileInfo fileInfo = bucketManager.stat(QiniuConfig.QINIU_BUCKET, param.getKey());
            System.out.println(fileInfo.hash);
            System.out.println(fileInfo.fsize);
            System.out.println(fileInfo.mimeType);
            System.out.println(fileInfo.putTime);
        } catch (QiniuException ex) {
            System.err.println(ex.response.toString());
        }
        return null;
    }

    public static void main(String [] a) {
        DownloadUrl url = new DownloadUrl(QiniuConfig.QINIU_DOMAIN, true, "resource/test73071432-9208-4171-b9b7-fdeae2686a84/（2021-1-8）XT平台操作流程手册.docx");
        url.setAttname("exam-stems/shortcut的附件52.txt");

        Auth auth = Auth.create(QiniuConfig.QINIU_AK, QiniuConfig.QINIU_SK);

        //链接过期时间
        long deadline = System.currentTimeMillis() / 1000 + 3600;

        String urlString = null;
        try {
            urlString = url.buildURL(auth, deadline);
        } catch (QiniuException e) {
            e.printStackTrace();
        }

        System.out.println("下载链接：" + urlString);
    }

    @Override
    public String downloadZipUrl(GetFileParam param) {
        //参数目前没用
//        String urlFile = "";
        String urlPdf = "https://resource.dasctf.com/exam-stems/qiniu.pdf?e=1655362133&token=rmfgWQgms5emvhEzgKgyR1ddyuFSkWm-IJjq9Poa:i6h8ZbBEaQZzrnDDixDLVCmq4RM=";
        List<String> uriList = Collections.singletonList(urlPdf);
        //批量下载到指定目录
        List<Future<Map<String,String>>> futures = downFileInPath(uriList);

        //todo 把获取的几个文件压缩
        String zipFileName = getMd5Code() + ".zip";
        File tempFile = new File("/Users/jianhuilin/Downloads/",zipFileName);


        //todo 统一获取，再次上传到七牛云

        return null;
    }

    private List<Future<Map<String,String>>> downFileInPath(List<String> uriList){
        ExecutorService service = Executors.newFixedThreadPool(uriList.size());
        List<Future<Map<String,String>>> futures = new ArrayList<>(uriList.size());

        for (String url : uriList){
            Future<Map<String,String>> future = service.submit(()->{
                Map<String,String> map = new HashMap<>();
                String filePath = download(url);
                map.put(getMd5Code() + "-20220616",filePath);
                return map;
            });

            futures.add(future);
        }
        return futures;
    }

    private static String getMd5Code(){
        return DigestUtils.md5DigestAsHex(UUID.randomUUID().toString().getBytes());
    }

    private String download(String url) {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        HttpGet httpGet = new HttpGet(url);

        //把文件放到本地某个目录
        String newFilePath = "/Users/jianhuilin/Downloads/"+getMd5Code()+".pdf";

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            //do something with resp
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                return "";
            }
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return "";
            }

            InputStream is = entity.getContent();
            File file = new File(newFilePath);

            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] bytes = new byte[1024];
            int ch = 0;
            while ((ch = is.read(bytes)) != -1){
                outputStream.write(bytes,0,ch);
            }
            is.close();
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            //ignore
        }
        return newFilePath;
    }


    /****   下面是测试七牛云批量压缩   *****/

    /**
     * 七牛回调URL
     */
    public static final String NOTIFY_URL = "*******";
    /**
     * 七牛间隔符
     */
    public static final String QN_SEPARATOR = "/";
    /**
     * txt换行符
     */
    public static final String QN_NEWLINE = "";
    /**
     * 索引文件名称
     */
    public static final String TXT_NAME = "index.txt";

    /**
     * @Description: 大量文件压缩
     * @author ljwang
     * @date 2017年9月5日
     */
    public static void mkzip(String prefix) {

        //密钥配置
        Auth auth = Auth.create(QiniuConfig.QINIU_AK, QiniuConfig.QINIU_SK);

        //自动识别要上传的空间(bucket)的存储区域是华东、华北、华南。
//        Zone z = Zone.autoZone();
        Configuration cfg = new Configuration(Region.region2());


        //实例化一个BucketManager对象
        BucketManager bucketManager = new BucketManager(auth, cfg);


        //创建上传对象
        UploadManager uploadManager = new UploadManager(cfg);

        try {
            //调用listFiles方法列举指定空间的指定文件
            //参数一：bucket    空间名
            //参数二：prefix    文件名前缀
            //参数三：marker    上一次获取文件列表时返回的 marker
            //参数四：limit     每次迭代的长度限制，最大1000，推荐值 100
            //参数五：delimiter 指定目录分隔符，列出所有公共前缀（模拟列出目录效果）。缺省值为空字符串
            FileListing fileListing = bucketManager.listFiles(QiniuConfig.QINIU_BUCKET, prefix, null, 100, null);
            FileInfo[] items = fileListing.items;

            //压缩索引文件内容
            String content = "";
            for(FileInfo fileInfo : items){
                //拼接原始链接
                String url = "http://" + QiniuConfig.QINIU_DOMAIN + QN_SEPARATOR + fileInfo.key;
                //链接加密并进行Base64编码，别名去除前缀目录。
                String safeUrl = "/url/" + UrlSafeBase64.encodeToString(auth.privateDownloadUrl(url)) + "/alias/" + UrlSafeBase64.encodeToString(fileInfo.key.substring(prefix.length()));
                content += ((StringUtils.isBlank(content) ? "" : QN_NEWLINE) + safeUrl);
            }
            //            System.out.println(content);

            //索引文件路径
            String txtKey = prefix + TXT_NAME;
            //生成索引文件token（覆盖上传）
            String uptoken = auth.uploadToken(QiniuConfig.QINIU_BUCKET, txtKey, 3600, new StringMap().put("insertOnly", 0));
            //上传索引文件
            Response res = uploadManager.put(content.getBytes(), txtKey, uptoken);

            //默认utf-8，但是中文显示乱码，修改为gbk
            String fops = "mkzip/4/encoding/" + UrlSafeBase64.encodeToString("gbk") + "|saveas/" +
                    UrlSafeBase64.encodeToString(QiniuConfig.QINIU_BUCKET + ":"  + prefix + "压缩文件名.zip");

            OperationManager operater = new OperationManager(auth, cfg);

            StringMap params = new StringMap();
            //压缩完成后，七牛回调URL,我就是测试下哈哈
            //params.put("notifyURL", NOTIFY_URL);

            String id = operater.pfop(QiniuConfig.QINIU_BUCKET, txtKey, fops, params);
            String purl = "http://api.qiniu.com/status/get/prefop?id=" + id;
            System.out.println(purl);
        } catch (QiniuException e) {
            Response res = e.response;
            System.out.println(res);
            try {
                System.out.println(res.bodyString());
            } catch (QiniuException e1) {
                e1.printStackTrace();
            }
        }
    }

}
