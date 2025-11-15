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

@WebServlet("/api/meal-ticket-payments")
public class MealTicketPaymentServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // DTO 수정 (새 테이블 구조 반영)
    private class PaymentItem {
        int id;
        String employeeNumber;
        String menuName;
        int amount;
        String restaurant;
        String paymentTime;
        String status;

        public PaymentItem(int id, String employeeNumber, String menuName, int amount, 
                           String restaurant, String paymentTime, String status) {
            this.id = id;
            this.employeeNumber = employeeNumber;
            this.menuName = menuName;
            this.amount = amount;
            this.restaurant = restaurant;
            this.paymentTime = paymentTime;
            this.status = status;
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
            // DB 연결
            conn = DBUtil.getConnection();

            // ★ pos_payments 테이블 조회 ★
            String sql = "SELECT id, employee_number, menu_name, amount, restaurant, payment_time, status " +
                         "FROM pos_payments ORDER BY payment_time DESC";
            
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            List<PaymentItem> list = new ArrayList<>();

            while (rs.next()) {
                list.add(new PaymentItem(
                    rs.getInt("id"),
                    rs.getString("employee_number"),
                    rs.getString("menu_name"),
                    rs.getInt("amount"),
                    rs.getString("restaurant"),
                    rs.getString("payment_time"),
                    rs.getString("status")
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