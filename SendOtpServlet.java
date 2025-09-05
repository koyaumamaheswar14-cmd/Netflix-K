package com.netflixclone.servlets;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.Random;

@WebServlet("/send-otp")
public class SendOtpServlet extends HttpServlet {
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("text/html");
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
        out.write("<script>alert('Database configuration file not found'); window.location='login.html';</script>");
        return;
      }
      props.load(input);
    } catch (IOException e) {
      e.printStackTrace();
      out.write("<script>alert('Failed to load DB configuration'); window.location='login.html';</script>");
      return;
    }

    String dbUrl = props.getProperty("db.url");
    String dbUser = props.getProperty("db.user");
    String dbPassword = props.getProperty("db.password");

    // 🛠️ Connect to DB and insert OTP
    try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
      PreparedStatement ps = conn.prepareStatement(
        "INSERT INTO users (email, otp, otp_expiry) VALUES (?, ?, ?) " +
        "ON DUPLICATE KEY UPDATE otp=?, otp_expiry=?");

      ps.setString(1, email);
      ps.setString(2, otp);
      ps.setTimestamp(3, Timestamp.valueOf(expiry));
      ps.setString(4, otp);
      ps.setTimestamp(5, Timestamp.valueOf(expiry));
      ps.executeUpdate();

      // 📬 You can integrate email/SMS sending here
      System.out.println("OTP for " + email + " is " + otp); // For debugging

      out.write("<script>alert('OTP sent to your email'); window.location='index.html';</script>");

    } catch (SQLException e) {
      e.printStackTrace();
      out.write("<script>alert('Database error: " + e.getMessage() + "'); window.location='index.html';</script>");
    }
  }
}
