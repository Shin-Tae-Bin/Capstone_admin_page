package com.example.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.stream.Collectors;
import java.security.MessageDigest;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.mindrot.jbcrypt.BCrypt;
import com.google.gson.Gson;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.nio.charset.StandardCharsets;

@WebServlet("/api/admin/login")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // JWT 키
    private static final String JWT_SECRET_KEY = "JWT_secret_key_must_be_longer_than_32_bytes_very_secure!!";

    private class LoginRequest {
        String username;
        String password;
    }

    // GET 요청 시 로그인 페이지로 리다이렉트
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/login.html");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            // 1. 요청 데이터 파싱
            String requestBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            LoginRequest loginReq = new Gson().fromJson(requestBody, LoginRequest.class);

            String username = loginReq.username;
            String password = loginReq.password;

            // 2. DB 연결 (DBUtil 사용)
            conn = DBUtil.getConnection();

            // 3. 조회
            String sql = "SELECT * FROM admin_accounts WHERE id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                String dbPasswordHash = rs.getString("password_hash");
                boolean isMatch = false;

                // 4. 비밀번호 검증
                if (dbPasswordHash != null && dbPasswordHash.startsWith("$")) {
                    // (A) Bcrypt 방식
                    if (dbPasswordHash.startsWith("$2b$")) {
                        dbPasswordHash = dbPasswordHash.replace("$2b$", "$2a$");
                    }
                    isMatch = BCrypt.checkpw(password, dbPasswordHash);
                } else {
                    // (B) SHA-256 방식
                    String inputHash = sha256(password);
                    isMatch = inputHash.equals(dbPasswordHash);
                }

                if (isMatch) {
                    // 로그인 성공 -> 토큰 발급
                    String token = createJwtToken(rs.getString("id"), rs.getString("name"));
                    out.print("{\"token\": \"" + token + "\"}");
                    out.flush();
                } else {
                    // [보안 강화] 메시지 통일
                    sendErrorResponse(response, out, 401, "아이디 또는 비밀번호가 잘못되었습니다.");
                }
            } else {
                // [보안 강화] 메시지 통일 (아이디가 없어도 비밀번호 틀린 척함)
                sendErrorResponse(response, out, 401, "아이디 또는 비밀번호가 잘못되었습니다.");
            }

        } catch (Exception e) {
            e.printStackTrace(); // 서버 로그에는 남김
            sendErrorResponse(response, out, 500, "서버 오류가 발생했습니다.");
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (pstmt != null) pstmt.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
    }
    
    private String sha256(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private String createJwtToken(String id, String name) {
        Key key = Keys.hmacShaKeyFor(JWT_SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        long nowMillis = System.currentTimeMillis();
        return Jwts.builder()
                .claim("id", id)
                .claim("name", name)
                .setIssuedAt(new Date(nowMillis))
                .setExpiration(new Date(nowMillis + 3600000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private void sendErrorResponse(HttpServletResponse response, PrintWriter out, int statusCode, String message) {
        response.setStatus(statusCode);
        out.print("{\"message\": \"" + message + "\"}");
        out.flush();
    }
}