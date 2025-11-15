package com.example.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.nio.charset.StandardCharsets;

@WebServlet("/api/admin/refresh-token")
public class RefreshTokenServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // LoginServlet과 똑같은 키여야 합니다! (32글자 이상)
    private static final String JWT_SECRET_KEY = "";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            // 1. 헤더에서 토큰 꺼내기 (Bearer 부분 제거)
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.setStatus(401);
                out.print("{\"message\": \"토큰이 없습니다.\"}");
                return;
            }

            String token = authHeader.substring(7); // "Bearer " 이후 문자열

            // 2. 토큰 검증 및 해석
            Key key = Keys.hmacShaKeyFor(JWT_SECRET_KEY.getBytes(StandardCharsets.UTF_8));
            
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String id = claims.get("id", String.class);
            String name = claims.get("name", String.class);

            // 3. 새 토큰 발급 (1시간 연장)
            long nowMillis = System.currentTimeMillis();
            Date now = new Date(nowMillis);
            long expMillis = nowMillis + 3600000; 
            Date exp = new Date(expMillis);

            String newToken = Jwts.builder()
                    .claim("id", id)
                    .claim("name", name)
                    .setIssuedAt(now)
                    .setExpiration(exp)
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();

            // 4. 응답
            JsonObject jsonResponse = new JsonObject();
            jsonResponse.addProperty("token", newToken);
            out.print(jsonResponse.toString());
            out.flush();

        } catch (Exception e) {
            // 토큰 만료나 위조 시 에러 발생
            e.printStackTrace();
            response.setStatus(401);
            out.print("{\"message\": \"유효하지 않은 토큰입니다.\"}");
        }
    }
}
