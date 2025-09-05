package com.netflixclone.servlets;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.logging.*;

@WebServlet("/verify-otp")
public class VerifyOtpServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(VerifyOtpServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");

        String email = request.getParameter("email");
        String otp = request.getParameter("otp");

        if (email == null || otp == null || email.isEmpty() || otp.isEmpty()) {
            response.getWriter().write("<script>alert('Email and OTP are required'); window.location='index.html';</script>");
            return;
        }

        try {
            // Load JDBC driver (optional in modern setups)
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Use environment variables or config file for credentials in production
            try (Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/netflix_clone",
                    "root",
                    "uma123")) {

                PreparedStatement ps = conn.prepareStatement(
                        "SELECT otp, otp_expiry FROM users WHERE email = ?");
                ps.setString(1, email);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    String dbOtp = rs.getString("otp");
                    Timestamp expiry = rs.getTimestamp("otp_expiry");

                    if (dbOtp != null && dbOtp.equals(otp) &&
                            expiry != null && expiry.toLocalDateTime().isAfter(LocalDateTime.now())) {

                        // Optional: Invalidate OTP after successful verification
                        PreparedStatement clearOtp = conn.prepareStatement(
                                "UPDATE users SET otp = NULL, otp_expiry = NULL WHERE email = ?");
                        clearOtp.setString(1, email);
                        clearOtp.executeUpdate();

                        HttpSession session = request.getSession();
                        session.setAttribute("email", email);
                        response.sendRedirect("Main.html");
                    } else {
                        response.getWriter().write("<script>alert('Invalid or expired OTP'); window.location='index.html';</script>");
                    }
                } else {
                    response.getWriter().write("<script>alert('User not found'); window.location='index.html';</script>");
                }
            }
        } catch (ClassNotFoundException e) {
            logger.severe("JDBC Driver not found: " + e.getMessage());
            response.getWriter().write("<script>alert('Server error: Driver issue'); window.location='index.html';</script>");
        } catch (SQLException e) {
            logger.severe("Database error: " + e.getMessage());
            response.getWriter().write("<script>alert('Server error: Database issue'); window.location='index.html';</script>");
        }
    }
}
