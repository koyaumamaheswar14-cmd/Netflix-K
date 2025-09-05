package com.netflixclone.servlets;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.Random;
import java.util.logging.*;

@WebServlet("/send-otp")
public class SendOtpServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(SendOtpServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String email = request.getParameter("email");

        // ✅ Validate email input
        if (email == null || email.trim().isEmpty()) {
            out.write("<script>alert('Please enter a valid email address'); window.location='login.html';</script>");
            return;
        }

        // 🔢 Generate 6-digit OTP
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

        // 📦 Load DB credentials from properties file
        Properties props = new Properties();
        try (InputStream input = getServletContext().getResourceAsStream("/WEB-INF/db.properties")) {
            if (input == null) {
                logger.severe("db.properties file not found");
                out.write("<script>alert('Server configuration error'); window.location='login.html';</script>");
                return;
            }
            props.load(input);
        } catch (IOException e) {
            logger.severe("Failed to load DB configuration: " + e.getMessage());
            out.write("<script>alert('Server error occurred'); window.location='login.html';</script>");
            return;
        }

        String dbUrl = props.getProperty("db.url");
        String dbUser = props.getProperty("db.user");
        String dbPassword = props.getProperty("db.password");

        try {
            // ✅ Load JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 🛠️ Connect to DB and insert/update OTP
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (email, otp, otp_expiry) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE otp = ?, otp_expiry = ?");

                ps.setString(1, email);
                ps.setString(2, otp);
                ps.setTimestamp(3, Timestamp.valueOf(expiry));
                ps.setString(4, otp);
                ps.setTimestamp(5, Timestamp.valueOf(expiry));
                ps.executeUpdate();

                // 📬 Placeholder for email/SMS integration
                logger.info("OTP for " + email + " is " + otp); // For debugging/logging

                out.write("<script>alert('OTP sent to your email'); window.location='index.html';</script>");
            }
        } catch (ClassNotFoundException e) {
            logger.severe("JDBC Driver not found: " + e.getMessage());
            out.write("<script>alert('Server error: Driver issue'); window.location='index.html';</script>");
        } catch (SQLException e) {
            logger.severe("Database error: " + e.getMessage());
            out.write("<script>alert('Server error occurred'); window.location='index.html';</script>");
        }
    }
}
