package com.github.itisokey.githelper.gitlab.settings;

import lombok.SneakyThrows;

import java.net.URI;

/**
 * @author Lv LiFeng
 * @date 2022/1/16 09:55
 */
public class ApiToRepoUrlConverter {

    @SneakyThrows
    public static String convertApiUrlToRepoUrl(String apiUrl) {
        URI uri = new URI(apiUrl);
        String domain = uri.getHost();
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

}
