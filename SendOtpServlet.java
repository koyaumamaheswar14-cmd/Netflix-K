import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

public class SendOtpServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String email = request.getParameter("email");

        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.\\w{2,}$")) {
            response.getWriter().write("{\"message\":\"Invalid email format\"}");
            return;
        }

        String otp = String.valueOf(new Random().nextInt(900000) + 100000);

        try (Connection conn = DBUtil.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO otp_users (email, otp, created_at) VALUES (?, ?, NOW()) ON DUPLICATE KEY UPDATE otp=?, created_at=NOW()"
            );
            ps.setString(1, email);
            ps.setString(2, otp);
            ps.setString(3, otp);
            ps.executeUpdate();
        } catch (Exception e) {
            response.getWriter().write("{\"message\":\"Database error\"}");
            return;
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");

            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication("your_email@gmail.com", "your_app_password");
                }
            });

            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress("your_email@gmail.com"));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            msg.setSubject("Your OTP Code");
            msg.setText("Your OTP is: " + otp);
            Transport.send(msg);

            response.getWriter().write("{\"message\":\"OTP sent successfully\"}");
        } catch (Exception e) {
            response.getWriter().write("{\"message\":\"Email sending failed\"}");
        }
    }
}
