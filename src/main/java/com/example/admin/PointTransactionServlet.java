package com.example.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

// 포인트 내역 대신 'QR 식권 발급 내역'을 보여주는 서블릿입니다.
@WebServlet("/api/point-transactions")
public class PointTransactionServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // 프론트엔드(JS)가 기대하는 필드명에 맞춰 DTO를 구성했습니다.
    private class PointTransaction {
        String user_id;        // -> 학번/사번 (employee_number)
        int amount;            // -> 식권 수량 (ticket_count)
        String transaction_type; // -> 상태 (발급됨/사용됨)
        String payment_key;    // -> QR 해시값 앞부분 (식별용)
        String created_at;     // -> 발급 시간 (issued_at)
        String name;           // -> 사용자 이름

        public PointTransaction(String user_id, int amount, String transaction_type, String payment_key, String created_at, String name) {
            this.user_id = user_id;
            this.amount = amount;
            this.transaction_type = transaction_type;
            this.payment_key = payment_key;
            this.created_at = created_at;
            this.name = name;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();

            // ★★★ [수정됨] qr_issued_tokens 테이블 조회 ★★★
            // users 테이블과 조인하여 이름까지 가져옵니다.
            String sql = "SELECT qt.employee_number, qt.ticket_count, qt.is_used, qt.qr_hash, qt.issued_at, u.name " +
                         "FROM qr_issued_tokens qt " +
                         "LEFT JOIN users u ON qt.user_id = u.id " +
                         "ORDER BY qt.issued_at DESC";

            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            List<PointTransaction> list = new ArrayList<>();

            while (rs.next()) {
                // is_used 값(0/1)에 따라 상태 텍스트 결정
                int isUsed = rs.getInt("is_used");
                String status = (isUsed == 1) ? "사용완료" : "사용가능";
                
                // qr_hash가 너무 길 수 있으니 앞 8자리만 잘라서 보여줌 (선택사항)
                String qrHash = rs.getString("qr_hash");
                String shortHash = (qrHash != null && qrHash.length() > 8) ? qrHash.substring(0, 8) + "..." : qrHash;

                list.add(new PointTransaction(
                    rs.getString("employee_number"), // user_id 자리에 사번 넣기
                    rs.getInt("ticket_count"),       // amount 자리에 식권 수량 넣기
                    status,                          // transaction_type 자리에 상태 넣기
                    shortHash,                       // payment_key 자리에 QR 해시 넣기
                    rs.getString("issued_at"),       // created_at 자리에 발급일시 넣기
                    rs.getString("name")             // 이름
                ));
            }

            String json = new Gson().toJson(list);
            out.print(json);
            out.flush();

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            out.print("{\"message\": \"서버 오류: " + e.getMessage() + "\"}");
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (pstmt != null) pstmt.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
    }
}