package com.example.security.config.security;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class SkipPathAntMatcher implements RequestMatcher {
    private List<String> pathsToSkip;

    public SkipPathAntMatcher(List<String> pathsToSkip) {
        this.pathsToSkip = pathsToSkip;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        //System.out.println("请求路径-->" + request.getRequestURL());
        if (!ObjectUtils.isEmpty(pathsToSkip)) {
            for (String s : pathsToSkip) {
                AntPathRequestMatcher antPathRequestMatcher = new AntPathRequestMatcher(s);
                if (antPathRequestMatcher.matches(request)) {
                    return true;
                }
            }
        }
        return false;
    }
}
