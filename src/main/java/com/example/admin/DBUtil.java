package com.example.admin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtil {
    

    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "myapp";
    private static final String DB_USER = "kbu"; 
    private static final String DB_PASSWORD = "qhdks12!@"; 

    // 접속 주소 자동 생성
    private static final String DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;

    // ==========================================
    // 연결을 빌려주는 메소드
    // ==========================================
    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        // 1. 드라이버 로드
        Class.forName("com.mysql.cj.jdbc.Driver");
        
        // 2. 연결 객체 생성 및 반환
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}