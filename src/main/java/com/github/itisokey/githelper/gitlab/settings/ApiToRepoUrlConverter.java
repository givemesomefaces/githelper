package com.github.itisokey.githelper.gitlab.settings;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Lv LiFeng
 * @date 2022/1/16 09:55
 */
public class ApiToRepoUrlConverter {

    public static String convertApiUrlToRepoUrl(String apiUrl) {
        URI uri = null;
        try {
            uri = new URI(apiUrl);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        String domain = uri.getHost();
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

}
