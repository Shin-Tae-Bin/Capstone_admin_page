package com.example.admin;

import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;

// "/api/*" 로 시작하는 모든 요청을 검사합니다.
@WebFilter("/api/*")
public class AuthFilter extends HttpFilter implements Filter {
    private static final long serialVersionUID = 1L;

    // LoginServlet과 동일한 키 사용
    private static final String JWT_SECRET_KEY = "";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // ★★★ [추가됨] 한글 깨짐 방지 설정 ★★★
        request.setCharacterEncoding("UTF-8");
        httpResponse.setContentType("application/json");
        httpResponse.setCharacterEncoding("UTF-8");
        
        // 접속하려는 주소 확인
        String path = httpRequest.getRequestURI();

        // 1. 로그인 요청과 리프레시 요청은 검사 없이 통과 (문 열어줌)
        if (path.endsWith("/login") || path.endsWith("/refresh-token")) {
            chain.doFilter(request, response);
            return;
        }
        
        // 2. 그 외의 모든 요청(데이터 조회 등)은 토큰 검사
        String authHeader = httpRequest.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // 토큰이 없으면 401 에러 보내고 차단
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getWriter().write("{\"message\": \"인증 토큰이 없습니다.\"}");
            return;
        }

        try {
            // 토큰 위조/만료 검사
            String token = authHeader.substring(7);
            Key key = Keys.hmacShaKeyFor(JWT_SECRET_KEY.getBytes(StandardCharsets.UTF_8));
            
            // 서명이 맞는지 확인 (틀리면 여기서 에러 발생)
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            
            // 통과! 서블릿 실행
            chain.doFilter(request, response);

        } catch (Exception e) {
            // 토큰이 가짜거나 만료됨
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getWriter().write("{\"message\": \"유효하지 않은 토큰입니다.\"}");
        }
    }
}
