/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kaisquare.playframework;

import com.kaisquare.util.HTTPUtil;
import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.results.Result;

import java.io.File;

/**
 * Result type for rendering JavaScript with Access-Control-Allow-Origin header set to *.
 *
 * @author kdp
 */
public class RenderJavaScript extends Result {

    private static final String jsCacheDuration = "5mn";

    File file;

    public RenderJavaScript(File file) {
        this.file = file;
    }

    @Override
    public void apply(Request request, Response response) {
        try {
            setContentTypeIfNotSet(response, "application/javascript");
            response.accessControl(HTTPUtil.getUriForRequest(request));
            response.cacheFor(jsCacheDuration);

            if (file != null) {
                if (!file.exists()) {
                    throw new UnexpectedException("Your file does not exists (" + file + ")");
                }
                if (!file.canRead()) {
                    throw new UnexpectedException("Can't read your file (" + file + ")");
                }
                if (!file.isFile()) {
                    throw new UnexpectedException("Your file is not a real file (" + file + ")");
                }
                response.direct = file;
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

}
