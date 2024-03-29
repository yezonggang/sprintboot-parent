package com.example.security.config.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.security.entity.RefreshTokenEntity;
import com.example.security.mapper.RefreshTokenMapper;
import com.example.security.service.impl.UserServiceImpl;
import com.example.security.utils.JsonWebTokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import execption.ApiError;
import execption.ApiErrorEnum;
import response.ResponseData;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 登录成功后，对此类进行鉴权
 */

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private JsonWebTokenUtil tokenUtil;
    private UserServiceImpl userServiceImpl;
    private RequestMatcher requestMatcher;
    //private List<String> requestMatcher;
    private RefreshTokenMapper refreshTokenMapper;

    public JwtAuthenticationFilter(JsonWebTokenUtil tokenUtil, UserServiceImpl userServiceImpl, RequestMatcher requestMatcher,RefreshTokenMapper refreshTokenMapper) {
        this.tokenUtil = tokenUtil;
        this.userServiceImpl = userServiceImpl;
        this.requestMatcher = requestMatcher;
        this.refreshTokenMapper = refreshTokenMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        logger.info(String.format("请求路径-->%s,是否应该被过滤掉-->%s",request.getRequestURL(),requestMatcher.matches(request)));
        return requestMatcher.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        //从请求头部获取json web token
        String jwt = request.getHeader(tokenUtil.getHeader());
        logger.info("urls:"+request.getRequestURI()+"XXXXXX"+request.getRequestURI().substring(0,request.getRequestURL().indexOf("/")));
            if (StringUtils.hasLength(jwt) && !jwt.equals("null") && !jwt.equals("undefined")) {
                //从jwt中获取用户名,这里应该考虑过期时间，超过过期时间的话获取不到username
                //TODO
                String username = tokenUtil.getUsernameFromToken(jwt);
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    LambdaQueryWrapper<RefreshTokenEntity> queryWrapper = new QueryWrapper<RefreshTokenEntity>().lambda().eq(RefreshTokenEntity::getUsename, username);
                    RefreshTokenEntity refreshTokenEntity = refreshTokenMapper.selectOne(queryWrapper);
                    String tokenInMysql = StringUtils.isEmpty(refreshTokenEntity) ? "null" : refreshTokenEntity.getToken();
                    if (jwt.equals(tokenInMysql)) {
                        logger.info("JwtAuthenticationFilter,username:" + username);
                        //通过用户名查询
                        UserDetails userDetails = userServiceImpl.loadUserByUsername(username);
                        //创建认证信息
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username,
                                userDetails.getPassword(), userDetails.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
                filterChain.doFilter(request, response);
            } else {
                //logger.info(requestMatcher.toString());
                reject(request, response);
            }

    }

    private static void reject(HttpServletRequest request,HttpServletResponse response) throws IOException{
/*        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", true);
        jsonObject.put("code",50054);
        jsonObject.put("data","Token expired, please log in again.");
        PrintWriter out = response.getWriter();
        out.write(new ObjectMapper().writeValueAsString(jsonObject));*/
        logger.info("filtered url:"+request.getRequestURI());
        PrintWriter out = response.getWriter();
        out.write(new ObjectMapper().writeValueAsString(ResponseData.fail(ApiError.from(ApiErrorEnum.HAVE_NO_TOKEN))));
        out.flush();
    }

}
