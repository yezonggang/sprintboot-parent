package com.example.springbootmybatisplus.config.security;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.springbootmybatisplus.entity.RefreshTokenEntity;
import com.example.springbootmybatisplus.mapper.RefreshTokenMapper;
import com.example.springbootmybatisplus.service.IRefreshTokenService;
import com.example.springbootmybatisplus.service.impl.UserServiceImpl;
import com.example.springbootmybatisplus.utils.ResponseData;
import com.example.springbootmybatisplus.utils.ResponseMsgUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * spring security 配置类
 *
 * @author yzg
 */
@EnableWebSecurity
public class MasSecurity extends WebSecurityConfigurerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(MasSecurity.class);

    @Autowired
    LoginAuthProvider loginAuthProvider;

    @Autowired
    JsonWebTokenUtil jsonWebTokenUtil;
    @Autowired
    IRefreshTokenService refreshTokenService;

    @Autowired
    RefreshTokenMapper refreshTokenMapper;

    @Autowired
    UserServiceImpl userService;

    @Autowired
    JsonWebTokenProperty jsonWebTokenProperty;

    // 先认证后鉴权
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(loginAuthProvider);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //允许跨域，配置后SpringSecurity会自动寻找name=corsConfigurationSource的Bean
        http.cors();
        http.csrf().disable();
        //当访问接口失败的配置
        http.exceptionHandling().authenticationEntryPoint(new InterfaceAccessException());
        http.authorizeRequests()
                .antMatchers("/login", "/refreshToken", "/user/getInfo", "swagger-ui.html").permitAll()
                .anyRequest().authenticated()
                .and()
                .formLogin().loginProcessingUrl("/login")
                .and().addFilterAt(jsonAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);//因为用不到session，所以选择禁用
        http.logout().logoutUrl("/logout").logoutSuccessHandler(logoutSuccessHandler()).deleteCookies(jsonWebTokenProperty.getHeader()).clearAuthentication(true);
        http.addFilterAfter(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        List<String> uris = new LinkedList<>();
        uris.add("/login");
        List<AntPathRequestMatcher> matchers = uris.stream().map(AntPathRequestMatcher::new).collect(Collectors.toList());
        return new JwtAuthenticationFilter(jsonWebTokenUtil, userService,
                request -> matchers.stream().anyMatch(m -> m.matches(request)), refreshTokenMapper);
    }


    @Bean
    public JsonAuthenticationFilter jsonAuthenticationFilter() throws Exception {
        JsonAuthenticationFilter filter = new JsonAuthenticationFilter();
        filter.setAuthenticationManager(authenticationManagerBean());
        filter.setFilterProcessesUrl("/login");
        filter.setAuthenticationSuccessHandler(new MySuccessHandler());
        filter.setAuthenticationFailureHandler(new MyFailHandler());
        return filter;
    }


    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return new LogoutSuccessHandler() {
            @Autowired
            public void setObjectMapper(ObjectMapper objectMapper) {
                this.objectMapper = objectMapper;
            }

            private ObjectMapper objectMapper;

            @Override
            public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
                response.setContentType("application/json;charset=utf-8");
                PrintWriter out = response.getWriter();
                out.write(objectMapper.writeValueAsString(ResponseData.success("logout success.")));
                out.flush();
                out.close();
            }

/*          // 这种写法更简洁
            @Override
            public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
                response.setContentType("application/json;charset=utf-8");
                PrintWriter out = response.getWriter();
                out.write(new ObjectMapper().writeValueAsString(ResponseData.success("logout success.")));
                out.flush();
                out.close();*/
        };
    }

    //登录成功的处理类
    class MySuccessHandler implements AuthenticationSuccessHandler {
        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                            Authentication authentication) throws IOException, ServletException {
            UserDetails details = (UserDetails) authentication.getPrincipal();
            List<GrantedAuthority> roles = (List<GrantedAuthority>) details.getAuthorities();
            //登录时同时生成refreshToken，保存到表中
            logger.info("begin to onAuthenticationSuccess.");
            RefreshTokenEntity token = new RefreshTokenEntity();
            token.setUsename(details.getUsername());
            String refreshToken = jsonWebTokenUtil.generateRefreshToken(details, roles.get(0).getAuthority());
            token.setToken(refreshToken);
            LambdaQueryWrapper<RefreshTokenEntity> queryWrapper = new QueryWrapper<RefreshTokenEntity>().lambda().eq(RefreshTokenEntity::getUsename, details.getUsername());
            RefreshTokenEntity refreshTokenTemp = refreshTokenMapper.selectOne(queryWrapper);
            if (refreshTokenTemp != null) {
                refreshTokenTemp.setToken(refreshToken);
                refreshTokenMapper.update(refreshTokenTemp, queryWrapper);
            } else {
                logger.info("begin to insert token");
                refreshTokenMapper.insert(token);
                logger.info("end to insert token");
            }
            response.setHeader(jsonWebTokenUtil.getHeader(), refreshToken);
/*          Cookie cookie = new Cookie("token", refreshToken);
            cookie.setPath("/");
            response.addCookie(cookie)
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("status", true);
            jsonObject.put("code",20000);
            jsonObject.put("data",refreshToken);
            out.write(new ObjectMapper().writeValueAsString(jsonObject));
            ;*/
            PrintWriter out = response.getWriter();
            out.write(new ObjectMapper().writeValueAsString(ResponseData.success(refreshToken)));
            out.flush();
            out.close();
            ResponseData.success(response);

        }
    }

    //登录失败的处理
    public class MyFailHandler implements AuthenticationFailureHandler {

        @Override
        public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                            AuthenticationException e) throws IOException, ServletException {
            ResponseMsgUtil.sendFailMsg(e.getMessage(), response);
        }
    }


}




