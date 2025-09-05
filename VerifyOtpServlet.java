import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;

public class VerifyOtpServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String email = request.getParameter("email");
        String otp = request.getParameter("otp");

        try (Connection conn = DBUtil.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM otp_users WHERE email=? AND otp=? AND created_at >= NOW() - INTERVAL 10 MINUTE"
            );
            ps.setString(1, email);
            ps.setString(2, otp);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // OTP is valid, delete it
                PreparedStatement deletePs = conn.prepareStatement("DELETE FROM otp_users WHERE email=?");
                deletePs.setString(1, email);
                deletePs.executeUpdate();

                response.getWriter().write("{\"success\":true}");
            } else {
                response.getWriter().write("{\"success\":false}");
            }
        } catch (Exception e) {
            response.getWriter().write("{\"success\":false}");
        }
    }
}
