package com.netflixclone.servlets;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.logging.*;

@WebServlet("/verify-otp")
public class VerifyOtpServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(VerifyOtpServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");

        String email = request.getParameter("email");
        String otp = request.getParameter("otp");

        if (email == null || otp == null || email.trim().isEmpty() || otp.trim().isEmpty()) {
            sendAlert(response, "Email and OTP are required", "index.html");
            return;
        }

        Properties dbProps = new Properties();
        try (InputStream input = getServletContext().getResourceAsStream("/WEB-INF/db.properties")) {
            dbProps.load(input);
        } catch (IOException e) {
            logger.severe("Failed to load DB config: " + e.getMessage());
            sendAlert(response, "Server error occurred", "index.html");
            return;
        }

        String dbUrl = dbProps.getProperty("db.url");
        String dbUser = dbProps.getProperty("db.user");
        String dbPassword = dbProps.getProperty("db.password");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT otp, otp_expiry FROM users WHERE email = ?");
                ps.setString(1, email);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    String dbOtp = rs.getString("otp");
                    Timestamp expiry = rs.getTimestamp("otp_expiry");

                    if (dbOtp != null && dbOtp.equals(otp) &&
                        expiry != null && expiry.toLocalDateTime().isAfter(LocalDateTime.now())) {

                        PreparedStatement clearOtp = conn.prepareStatement(
                            "UPDATE users SET otp = NULL, otp_expiry = NULL WHERE email = ?");
                        clearOtp.setString(1, email);
                        clearOtp.executeUpdate();

                        HttpSession session = request.getSession();
                        session.setAttribute("email", email);
                        response.sendRedirect("Main.html");
                    } else {
                        sendAlert(response, "Invalid or expired OTP", "index.html");
                    }
                } else {
                    sendAlert(response, "User not found", "index.html");
                }
            }
        } catch (Exception e) {
            logger.severe("Error verifying OTP: " + e.getMessage());
            sendAlert(response, "Server error occurred", "index.html");
        }
    }

    private void sendAlert(HttpServletResponse response, String message, String redirectPage) throws IOException {
        PrintWriter out = response.getWriter();
        out.write("<script>alert('" + message + "'); window.location='" + redirectPage + "';</script>");
    }
}
