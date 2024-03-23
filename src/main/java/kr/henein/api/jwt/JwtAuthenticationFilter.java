package kr.henein.api.jwt;

import kr.henein.api.error.ErrorCode;
import kr.henein.api.error.ErrorJwtCode;
import kr.henein.api.error.exception.JwtException;
import kr.henein.api.service.jwtservice.UserDetailsServiceImpl;
import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsServiceImpl;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();
        log.info(path);
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs") || path.startsWith("/swagger-resources")
            || path.startsWith("/auth")
            || (method.equals(HttpMethod.GET.name()) && path.startsWith("/board"))
            || path.contains("/userinfo/character/info") || path.contains("/userinfo/search")
            || path.startsWith("/actuator")) {

            filterChain.doFilter(request,response);
            return;
        }


        // 헤더에서 Token을 따옴
        String accessToken = jwtTokenProvider.resolveAccessToken(request);
        ErrorJwtCode errorCode;

        if (accessToken == null || accessToken.trim().isEmpty()) {
            errorCode = ErrorJwtCode.EMPTY_TOKEN;
            setResponse(response,errorCode);
            return; // 위의 링크에 걸리지 않고 토큰이 없는 경우 엠티처리
        }

        try {
            if (jwtTokenProvider.validateToken(accessToken)) {
                // Get the username from the access token
                String email = jwtTokenProvider.getUserEmailFromAccessToken(accessToken);
                // Load the user details
                UserDetails userDetails = userDetailsServiceImpl.loadUserByUsername(email);
                // Create an authentication object
                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                // Set the authentication object in the security context
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (MalformedJwtException e) {
            log.info("103 error");
            errorCode = ErrorJwtCode.INVALID_TOKEN;
            setResponse(response, errorCode);
            return;
        } catch (ExpiredJwtException e) {
            log.info("101 error");
            errorCode = ErrorJwtCode.EXPIRED_AT;
            setResponse(response, errorCode);
            return;
        } catch (UnsupportedJwtException e) {
            log.info("105 error");
            errorCode = ErrorJwtCode.INVALID_TOKEN;
            setResponse(response, errorCode);
            return;
        } catch (IllegalArgumentException e) {
            log.info("104 error");
            errorCode = ErrorJwtCode.EMPTY_TOKEN;
            setResponse(response, errorCode);
            return;
        } catch (SignatureException e) {
            log.info("106 error");
            errorCode = ErrorJwtCode.INVALID_TOKEN;
            setResponse(response, errorCode);
            return;
        } catch (JwtException e) {
            log.info("4006 error");

            setResponse(response,  e.getErrorCode());
            return;
        }

        filterChain.doFilter(request, response);
    }
    private void setResponse(HttpServletResponse response, ErrorJwtCode errorCode) throws IOException {
        JSONObject json = new JSONObject();
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        json.put("code", errorCode.getCode());
        json.put("message", errorCode.getMessage());

        response.getWriter().print(json);
        response.getWriter().flush();
    }
    private void setResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        JSONObject json = new JSONObject();
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        json.put("code", errorCode.getCode());
        json.put("message", errorCode.getMessage());

        response.getWriter().print(json);
        response.getWriter().flush();
    }
}
