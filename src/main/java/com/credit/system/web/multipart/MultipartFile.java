package com.credit.system.web.multipart;

import java.io.InputStream;

public class MultipartFile {
    private String originalFilename;
    private String name;
    private long size;
    private byte[] content;
    private String contentType;

    public MultipartFile(String originalFilename, byte[] content) {
        this.originalFilename = originalFilename;
        this.content = content;
        this.size = content != null ? content.length : 0;
        this.name = originalFilename;
        this.contentType = "application/octet-stream";
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public byte[] getBytes() {
        return content;
    }

    public InputStream getInputStream() {
        return content != null ? new java.io.ByteArrayInputStream(content) : null;
    }

    public String getContentType() {
        return contentType;
    }

    public boolean isEmpty() {
        return size == 0;
    }
}
