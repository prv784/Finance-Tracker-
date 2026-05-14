package com.financetracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Async
    public void sendWelcomeEmail(String toEmail, String firstName) {
        String subject = "Welcome to AI Finance Tracker! 🎉";
        String body = buildWelcomeEmailBody(firstName);
        sendHtmlEmail(toEmail, subject, body);
    }

    @Async
    public void sendOtpEmail(String toEmail, String firstName, String otp) {
        String subject = "Your Verification OTP - AI Finance Tracker";
        String body = buildOtpEmailBody(firstName, otp);
        sendHtmlEmail(toEmail, subject, body);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String firstName, String resetToken) {
        String subject = "Reset Your Password - AI Finance Tracker";
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        String body = buildResetPasswordEmailBody(firstName, resetLink);
        sendHtmlEmail(toEmail, subject, body);
    }

    @Async
    public void sendMonthlyFinanceSummary(String toEmail, String firstName,
                                           BigDecimal income, BigDecimal expenses,
                                           BigDecimal savings, String month) {
        String subject = "Your Monthly Finance Summary - " + month;
        String body = buildMonthlySummaryEmailBody(firstName, income, expenses, savings, month);
        sendHtmlEmail(toEmail, subject, body);
    }

    @Async
    public void sendBudgetAlertEmail(String toEmail, String firstName,
                                      String budgetName, BigDecimal spent,
                                      BigDecimal total, double percentage) {
        String subject = "⚠️ Budget Alert: " + budgetName + " - AI Finance Tracker";
        String body = buildBudgetAlertEmailBody(firstName, budgetName, spent, total, percentage);
        sendHtmlEmail(toEmail, subject, body);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String buildWelcomeEmailBody(String firstName) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><style>
              body { font-family: 'Segoe UI', sans-serif; background: #f4f7f9; margin: 0; padding: 20px; }
              .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
              .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 40px; text-align: center; }
              .header h1 { color: white; margin: 0; font-size: 28px; }
              .body { padding: 40px; color: #333; }
              .btn { display: inline-block; background: linear-gradient(135deg, #667eea, #764ba2); color: white; padding: 14px 32px; border-radius: 8px; text-decoration: none; font-weight: 600; margin: 20px 0; }
              .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #888; font-size: 12px; }
            </style></head>
            <body>
              <div class="container">
                <div class="header"><h1>💰 AI Finance Tracker</h1></div>
                <div class="body">
                  <h2>Welcome, %s! 🎉</h2>
                  <p>Your account has been created successfully. You're now part of the AI Finance Tracker family!</p>
                  <p>Here's what you can do:</p>
                  <ul>
                    <li>📊 Track expenses and income</li>
                    <li>🤖 Get AI-powered spending insights</li>
                    <li>💡 Receive smart saving suggestions</li>
                    <li>📈 Visualize your financial health</li>
                    <li>🎯 Set and manage budgets</li>
                  </ul>
                  <a href="%s/dashboard" class="btn">Go to Dashboard →</a>
                </div>
                <div class="footer"><p>© 2024 AI Finance Tracker. All rights reserved.</p></div>
              </div>
            </body></html>
            """.formatted(firstName, frontendUrl);
    }

    private String buildOtpEmailBody(String firstName, String otp) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><style>
              body { font-family: 'Segoe UI', sans-serif; background: #f4f7f9; margin: 0; padding: 20px; }
              .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
              .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 40px; text-align: center; }
              .header h1 { color: white; margin: 0; font-size: 28px; }
              .body { padding: 40px; color: #333; }
              .otp-box { background: linear-gradient(135deg, #667eea20, #764ba220); border: 2px solid #667eea; border-radius: 12px; padding: 24px; text-align: center; margin: 24px 0; }
              .otp-code { font-size: 42px; font-weight: 900; letter-spacing: 12px; color: #667eea; font-family: monospace; }
              .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #888; font-size: 12px; }
            </style></head>
            <body>
              <div class="container">
                <div class="header"><h1>💰 AI Finance Tracker</h1></div>
                <div class="body">
                  <h2>Verify Your Account</h2>
                  <p>Hi %s, use the OTP below to verify your account. This OTP is valid for 10 minutes.</p>
                  <div class="otp-box">
                    <div class="otp-code">%s</div>
                    <p style="margin:8px 0 0; color:#888;">Expires in 10 minutes</p>
                  </div>
                  <p>If you didn't request this, please ignore this email.</p>
                </div>
                <div class="footer"><p>© 2024 AI Finance Tracker. All rights reserved.</p></div>
              </div>
            </body></html>
            """.formatted(firstName, otp);
    }

    private String buildResetPasswordEmailBody(String firstName, String resetLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><style>
              body { font-family: 'Segoe UI', sans-serif; background: #f4f7f9; margin: 0; padding: 20px; }
              .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
              .header { background: linear-gradient(135deg, #f093fb 0%%, #f5576c 100%%); padding: 40px; text-align: center; }
              .header h1 { color: white; margin: 0; font-size: 28px; }
              .body { padding: 40px; color: #333; }
              .btn { display: inline-block; background: linear-gradient(135deg, #f093fb, #f5576c); color: white; padding: 14px 32px; border-radius: 8px; text-decoration: none; font-weight: 600; margin: 20px 0; }
              .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #888; font-size: 12px; }
            </style></head>
            <body>
              <div class="container">
                <div class="header"><h1>🔒 Reset Password</h1></div>
                <div class="body">
                  <h2>Hi %s,</h2>
                  <p>You requested to reset your password. Click the button below to set a new password. This link expires in 1 hour.</p>
                  <a href="%s" class="btn">Reset My Password →</a>
                  <p style="color:#888; font-size:12px;">If the button doesn't work, copy and paste this link: <br>%s</p>
                  <p><strong>If you didn't request a password reset, please ignore this email.</strong></p>
                </div>
                <div class="footer"><p>© 2024 AI Finance Tracker. All rights reserved.</p></div>
              </div>
            </body></html>
            """.formatted(firstName, resetLink, resetLink);
    }

    private String buildMonthlySummaryEmailBody(String firstName, BigDecimal income,
                                                  BigDecimal expenses, BigDecimal savings, String month) {
        double savingsRate = income.compareTo(BigDecimal.ZERO) > 0
                ? savings.doubleValue() / income.doubleValue() * 100 : 0;
        String grade = savingsRate >= 30 ? "Excellent 🌟" : savingsRate >= 20 ? "Good 👍" : savingsRate >= 10 ? "Fair ⚠️" : "Needs Improvement 📉";

        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><style>
              body { font-family: 'Segoe UI', sans-serif; background: #f4f7f9; margin: 0; padding: 20px; }
              .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
              .header { background: linear-gradient(135deg, #11998e 0%%, #38ef7d 100%%); padding: 40px; text-align: center; }
              .header h1 { color: white; margin: 0; font-size: 28px; }
              .body { padding: 40px; color: #333; }
              .stat-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin: 24px 0; }
              .stat-card { background: #f8f9fa; border-radius: 12px; padding: 20px; text-align: center; }
              .stat-label { font-size: 12px; color: #888; text-transform: uppercase; letter-spacing: 1px; }
              .stat-value { font-size: 24px; font-weight: 700; color: #333; margin-top: 4px; }
              .income-color { color: #11998e; }
              .expense-color { color: #f5576c; }
              .savings-color { color: #667eea; }
              .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #888; font-size: 12px; }
            </style></head>
            <body>
              <div class="container">
                <div class="header"><h1>📊 Monthly Summary</h1><p style="color:rgba(255,255,255,0.9); margin:8px 0 0">%s</p></div>
                <div class="body">
                  <h2>Hi %s!</h2>
                  <p>Here's your financial summary for %s:</p>
                  <div class="stat-grid">
                    <div class="stat-card"><div class="stat-label">Total Income</div><div class="stat-value income-color">$%.2f</div></div>
                    <div class="stat-card"><div class="stat-label">Total Expenses</div><div class="stat-value expense-color">$%.2f</div></div>
                    <div class="stat-card"><div class="stat-label">Net Savings</div><div class="stat-value savings-color">$%.2f</div></div>
                    <div class="stat-card"><div class="stat-label">Savings Rate</div><div class="stat-value">%.1f%%</div></div>
                  </div>
                  <p><strong>Financial Health: %s</strong></p>
                </div>
                <div class="footer"><p>© 2024 AI Finance Tracker. All rights reserved.</p></div>
              </div>
            </body></html>
            """.formatted(month, firstName, month,
                income.doubleValue(), expenses.doubleValue(),
                savings.doubleValue(), savingsRate, grade);
    }

    private String buildBudgetAlertEmailBody(String firstName, String budgetName,
                                               BigDecimal spent, BigDecimal total, double percentage) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><style>
              body { font-family: 'Segoe UI', sans-serif; background: #f4f7f9; margin: 0; padding: 20px; }
              .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 16px; overflow: hidden; }
              .header { background: linear-gradient(135deg, #f7971e 0%%, #ffd200 100%%); padding: 40px; text-align: center; }
              .header h1 { color: white; margin: 0; font-size: 28px; }
              .body { padding: 40px; color: #333; }
              .alert-box { background: #fff3cd; border: 1px solid #ffc107; border-radius: 12px; padding: 20px; margin: 20px 0; }
              .progress-bar { background: #e9ecef; border-radius: 8px; height: 12px; margin: 12px 0; }
              .progress-fill { background: linear-gradient(135deg, #f7971e, #f5576c); border-radius: 8px; height: 100%%; width: %.0f%%; }
              .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #888; font-size: 12px; }
            </style></head>
            <body>
              <div class="container">
                <div class="header"><h1>⚠️ Budget Alert</h1></div>
                <div class="body">
                  <h2>Hi %s,</h2>
                  <div class="alert-box">
                    <p>Your <strong>%s</strong> budget has reached <strong>%.1f%%</strong> of the limit.</p>
                    <p>Spent: <strong>$%.2f</strong> / Budget: <strong>$%.2f</strong></p>
                    <div class="progress-bar"><div class="progress-fill"></div></div>
                  </div>
                  <p>Consider reviewing your spending to stay within budget.</p>
                </div>
                <div class="footer"><p>© 2024 AI Finance Tracker. All rights reserved.</p></div>
              </div>
            </body></html>
            """.formatted(percentage, firstName, budgetName, percentage,
                spent.doubleValue(), total.doubleValue());
    }
}
