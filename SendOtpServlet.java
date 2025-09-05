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
import javax.mail.*;
import javax.mail.internet.*;

@WebServlet("/send-otp")
public class SendOtpServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(SendOtpServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        String email = request.getParameter("email");

        if (email == null || email.trim().isEmpty()) {
            sendAlert(response, "Please enter a valid email address", "login.html");
            return;
        }

        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

        Properties props = new Properties();
        try (InputStream input = getServletContext().getResourceAsStream("/WEB-INF/db.properties")) {
            props.load(input);
        } catch (IOException e) {
            logger.severe("Failed to load DB config: " + e.getMessage());
            sendAlert(response, "Server error occurred", "login.html");
            return;
        }

        String dbUrl = props.getProperty("db.url");
        String dbUser = props.getProperty("db.user");
        String dbPassword = props.getProperty("db.password");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
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
            }

            sendEmail(email, otp);
            sendAlert(response, "OTP sent to your email", "index.html");

        } catch (Exception e) {
            logger.severe("Error sending OTP: " + e.getMessage());
            sendAlert(response, "Server error occurred", "index.html");
        }
    }

    private void sendEmail(String to, String otp) throws MessagingException {
        final String from = "your_email@example.com";
        final String password = "your_email_password";

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props,
            new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(from, password);
                }
            });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject("Your OTP Code");
        message.setText("Your OTP is: " + otp + "\nIt expires in 5 minutes.");

        Transport.send(message);
    }

    private void sendAlert(HttpServletResponse response, String message, String redirectPage) throws IOException {
        PrintWriter out = response.getWriter();
        out.write("<script>alert('" + message + "'); window.location='" + redirectPage + "';</script>");
    }
}
