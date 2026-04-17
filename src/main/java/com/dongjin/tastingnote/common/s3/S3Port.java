package com.dongjin.tastingnote.common.s3;

import org.springframework.web.multipart.MultipartFile;

public interface S3Port {
    String upload(MultipartFile file, String key);
    void delete(String key);
}
