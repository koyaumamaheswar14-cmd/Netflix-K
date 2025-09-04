package com.netflixclone.servlets;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Random;

@WebServlet("/send-otp")
public class SendOtpServlet extends HttpServlet {
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String email = request.getParameter("email");
    String otp = String.valueOf(new Random().nextInt(900000) + 100000);
    LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

    try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/netflix_clone", "root", "your_password")) {
      PreparedStatement ps = conn.prepareStatement(
        "INSERT INTO users (email, otp, otp_expiry) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE otp=?, otp_expiry=?");
      ps.setString(1, email);
      ps.setString(2, otp);
      ps.setTimestamp(3, Timestamp.valueOf(expiry));
      ps.setString(4, otp);
      ps.setTimestamp(5, Timestamp.valueOf(expiry));
      ps.executeUpdate();

      // You can integrate email/SMS sending here
      response.setContentType("text/html");
      response.getWriter().write("<script>alert('OTP sent to your email'); window.location='login.html';</script>");
    } catch (Exception e) {
      e.printStackTrace();
      response.getWriter().write("Error sending OTP");
    }
  }
}
