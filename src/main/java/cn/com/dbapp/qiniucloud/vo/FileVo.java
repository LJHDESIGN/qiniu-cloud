package cn.com.dbapp.qiniucloud.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;

/**
 * @author linjianhui
 * @description 文件上传
 * @date 2022/6/14 2:43 下午
 */
@Data
public class FileVo {

    @NotNull(message = "validate.required")
    private MultipartFile file;


    @JsonIgnore
    private  Integer creatorId;

}
