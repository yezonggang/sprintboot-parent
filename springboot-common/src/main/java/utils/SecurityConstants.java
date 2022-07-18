package utils;

import org.apache.tomcat.util.descriptor.web.SecurityConstraint;

public class SecurityConstants extends SecurityConstraint {
/*    public static final String JWT_TOKEN_HEADER = "authentication";
    public static final String USER_ID_HEADER = "userId";*/
    public static final String AUTHORIZATION_KEY = "authentication";
    public static final String BASIC_PREFIX = "Basic";
    public static final String REFRESH_TOKEN_KEY = "Token";
}
