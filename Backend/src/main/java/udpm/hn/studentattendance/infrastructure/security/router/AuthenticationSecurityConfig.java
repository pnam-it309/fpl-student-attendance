package udpm.hn.studentattendance.infrastructure.security.router;

import lombok.RequiredArgsConstructor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import udpm.hn.studentattendance.core.authentication.oauth2.CustomOAuth2FailureHandler;
import udpm.hn.studentattendance.core.authentication.oauth2.CustomOAuth2SuccessHandler;
import udpm.hn.studentattendance.core.authentication.oauth2.CustomOAuth2UserService;
import udpm.hn.studentattendance.core.authentication.oauth2.JwtAuthenticationFilter;
import udpm.hn.studentattendance.helpers.RouterHelper;
import udpm.hn.studentattendance.infrastructure.constants.RoleConstant;
import udpm.hn.studentattendance.infrastructure.constants.RoutesConstant;
import udpm.hn.studentattendance.infrastructure.constants.router.RouteAuthenticationConstant;

import static udpm.hn.studentattendance.helpers.RouterHelper.appendPrefixApi;
import static udpm.hn.studentattendance.helpers.RouterHelper.appendWildcard;

@Component
@RequiredArgsConstructor
public class AuthenticationSecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler;

    private final CustomOAuth2FailureHandler customOAuth2FailureHandler;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public void configure(HttpSecurity http) throws Exception {
        http.rememberMe(AbstractHttpConfigurer::disable);

        http.oauth2Login(oauth2 -> {
            oauth2.loginPage(RouteAuthenticationConstant.REDIRECT_LOGIN);
            oauth2.userInfoEndpoint(userInfoEndpointConfig -> {
                userInfoEndpointConfig.userService(customOAuth2UserService);
            });
            oauth2.successHandler(customOAuth2SuccessHandler);
            oauth2.failureHandler(customOAuth2FailureHandler);
        });

        http.authorizeHttpRequests(authorization -> {
            authorization.requestMatchers(appendPrefixApi(RouteAuthenticationConstant.API_GET_ALL_FACILITY)).permitAll();
            authorization.requestMatchers(appendPrefixApi(RouteAuthenticationConstant.API_GET_ALL_SEMESTER)).permitAll();
            authorization.requestMatchers(appendPrefixApi(RouteAuthenticationConstant.API_SETTINGS)).permitAll();
            authorization.requestMatchers(appendPrefixApi(RouteAuthenticationConstant.API_REFRESH_TOKEN)).authenticated();
            authorization.requestMatchers(appendPrefixApi(RouteAuthenticationConstant.API_GET_INFO_USER)).authenticated();
            authorization.requestMatchers(appendPrefixApi(RouteAuthenticationConstant.API_STUDENT_REGISTER)).permitAll();
            authorization.requestMatchers(appendPrefixApi(RouteAuthenticationConstant.API_STUDENT_INFO)).hasAuthority(RoleConstant.STUDENT.name());
            authorization.requestMatchers(RouteAuthenticationConstant.REDIRECT_LOGIN).permitAll();
            authorization.requestMatchers(RouterHelper.appendWildcard(RoutesConstant.PREFIX_API_NOTIFICATION)).authenticated();
        });

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }

}
