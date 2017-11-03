package it.bologna.ausl.baborg.config.spring;

import it.bologna.ausl.baborg.security.auth.AuthenticationFailureHandler;
import it.bologna.ausl.baborg.security.auth.AuthenticationSuccessHandler;
import it.bologna.ausl.baborg.security.jwt.CustomUserDetailsService;
import it.bologna.ausl.baborg.security.auth.LogoutSuccess;
import it.bologna.ausl.baborg.security.auth.TokenAuthenticationFilter;
import it.bologna.ausl.baborg.security.auth.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Created by fan.jin on 2016-10-19.
 */
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

//    @Value("${jwt.cookie}")
//    private String TOKEN_COOKIE;
//
//    @Bean
//    public TokenAuthenticationFilter jwtAuthenticationTokenFilter() throws Exception {
//        return new TokenAuthenticationFilter();
//    }
//
//    @Autowired
//    private CustomUserDetailsService jwtUserDetailsService;
//
//    @Autowired
//    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;
//
//    @Autowired
//    private LogoutSuccess logoutSuccess;
//
//    @Autowired
//    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
//        auth.userDetailsService(jwtUserDetailsService);
//    }
//
//    @Autowired
//    private AuthenticationSuccessHandler authenticationSuccessHandler;
//
//    @Autowired
//    private AuthenticationFailureHandler authenticationFailureHandler;
    @Override
    protected void configure(HttpSecurity http) throws Exception {
//        http
//                .csrf()
//                //todo csrf disabilitato in attesa di implementazione lato front end
//                .disable()
//                //.ignoringAntMatchers("/auth/login")
//                //.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()).and()
//                .sessionManagement().sessionCreationPolicy( SessionCreationPolicy.STATELESS ).and()
//                .exceptionHandling().authenticationEntryPoint( restAuthenticationEntryPoint ).and()
//                .addFilterBefore(jwtAuthenticationTokenFilter(), BasicAuthenticationFilter.class)
//                .authorizeRequests()
//                .anyRequest()
//                .authenticated().and()
//                .formLogin()
//                .loginPage("/auth/login")
//                .successHandler(authenticationSuccessHandler)
//                .failureHandler(authenticationFailureHandler).and()
//                .logout()
//                .logoutRequestMatcher(new AntPathRequestMatcher("/auth/logout"))
//                .logoutSuccessHandler(logoutSuccess)
//                .deleteCookies(TOKEN_COOKIE);
        http.csrf().disable().authorizeRequests().anyRequest().permitAll();

    }

}
