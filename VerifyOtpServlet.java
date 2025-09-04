package com.netflixclone.servlets;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;

@WebServlet("/verify-otp")
public class VerifyOtpServlet extends HttpServlet {
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String email = request.getParameter("email");
    String otp = request.getParameter("otp");

    try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/netflix_clone", "root", "your_password")) {
      PreparedStatement ps = conn.prepareStatement("SELECT otp, otp_expiry FROM users WHERE email=?");
      ps.setString(1, email);
      ResultSet rs = ps.executeQuery();

      if (rs.next()) {
        String dbOtp = rs.getString("otp");
        Timestamp expiry = rs.getTimestamp("otp_expiry");

        if (dbOtp.equals(otp) && expiry.toLocalDateTime().isAfter(LocalDateTime.now())) {
          HttpSession session = request.getSession();
          session.setAttribute("email", email);
          response.sendRedirect("index.html");
        } else {
          response.setContentType("text/html");
          response.getWriter().write("<script>alert('Invalid or expired OTP'); window.location='login.html';</script>");
        }
      } else {
        response.getWriter().write("User not found");
      }
    } catch (Exception e) {
      e.printStackTrace();
      response.getWriter().write("Error verifying OTP");
    }
  }
}
