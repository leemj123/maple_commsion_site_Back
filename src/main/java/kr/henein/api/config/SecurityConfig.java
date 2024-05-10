package kr.henein.api.config;

import kr.henein.api.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;


@EnableWebSecurity
@RequiredArgsConstructor
@EnableGlobalMethodSecurity(securedEnabled = true)
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors()
                .and()
                .csrf().disable()
                .formLogin().disable()
                .oauth2Login()
                .and()
                .logout()
                .logoutSuccessUrl("/")
                .and()
                .headers()
                // X-Frame-Options 헤더 설정
                    .frameOptions()
                    //클릭재킹 방지
                    .sameOrigin()// 'SAMEORIGIN' 정책을 설정. 이 페이지를 같은 출처의 페이지 내의 프레임에서만 표시 가능.
                    //X-content-Type-Options 헤더 설정 content-type 무시 비허용
                    .contentTypeOptions()
                    .and()
                    //강제 Https 접속 실행, HSTS - 중간자 공격 방지
                    .httpStrictTransportSecurity()
                    .includeSubDomains(true) //서브 도메인도 포함인지
                    .maxAgeInSeconds(31536000) //1년
                    .and()
                .and()
                .authorizeRequests()
                // .antMatchers(HttpMethod.POST,"/board/recommend").hasAnyAuthority("USER","ADMIN")
                .antMatchers(HttpMethod.OPTIONS,"/**").permitAll()//userinfo불러올때의 CORS를 해결하기 위해 OPTIONS에 대한 설정함
                .antMatchers("/userinfo/test").permitAll()
                .antMatchers("/auth/**").permitAll()
                .antMatchers("/actuator/**").permitAll()
                .antMatchers(HttpMethod.POST, "/userinfo/character/info").permitAll()
                .antMatchers(HttpMethod.GET,"/board/**").permitAll() // /board/**에 대한 GET 요청을 허용합니다.
                .antMatchers(HttpMethod.POST,"/userinfo/character/info").permitAll()
                .antMatchers("/userinfo/search").permitAll()
                .antMatchers(HttpMethod.POST, "/board/**").authenticated()
                .antMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/swagger-ui.html").permitAll()
                .antMatchers("/userinfo/active/**").permitAll()
                .anyRequest().authenticated()

                .and()
                .addFilterBefore(jwtAuthenticationFilter, BasicAuthenticationFilter.class)
                .exceptionHandling()
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
        return http.build();

    }

}