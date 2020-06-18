/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.externalcommunications;

import java.io.File;
import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 *
 * @author Salo
 */
public class RequestBodyBuilder {

    public static final okhttp3.MediaType JSON = okhttp3.MediaType.parse("application/json; charset=utf-8");
    public static final okhttp3.MediaType OCTET_STREAM_MEDIA_TYPE = okhttp3.MediaType.parse("application/octet-stream; charset=utf-8");

    public static RequestBody createRequestBody(String mediaTypeString, String bodyString) {
        return RequestBody.create(MediaType.get(mediaTypeString), bodyString);
    }

    public static okhttp3.RequestBody createJSONRequestBody(String content) {
        return okhttp3.RequestBody.create(JSON, content);
    }

    public static okhttp3.RequestBody createJSONRequestBody(File file) {
        return okhttp3.RequestBody.create(JSON, file);
    }

    public static okhttp3.RequestBody createJSONRequestBody(byte[] content) {
        return okhttp3.RequestBody.create(JSON, content);
    }

    public static okhttp3.RequestBody createOctetStreamRequestBody(byte[] content) {
        return okhttp3.RequestBody.create(OCTET_STREAM_MEDIA_TYPE, content);
    }

}
