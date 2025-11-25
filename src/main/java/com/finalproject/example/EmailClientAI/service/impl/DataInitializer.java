package com.finalproject.example.EmailClientAI.service.impl;

import com.finalproject.example.EmailClientAI.entity.Email;
import com.finalproject.example.EmailClientAI.entity.Mailbox;
import com.finalproject.example.EmailClientAI.entity.User;
import com.finalproject.example.EmailClientAI.repository.EmailRepository;
import com.finalproject.example.EmailClientAI.repository.MailboxRepository;
import com.finalproject.example.EmailClientAI.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MailboxRepository mailboxRepository;
    private final EmailRepository emailRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Only initialize if database is empty
        if (userRepository.count() == 0) {
            log.info("Initializing mock data...");
            initializeMockData();
            log.info("Mock data initialized successfully!");
        }
    }

    private void initializeMockData() {
        // Create demo user
        User demoUser = User.builder()
                .email("demo@emailclient.com")
                .password(passwordEncoder.encode("demo123456"))
                .name("Demo User")
                .build();
        demoUser = userRepository.save(demoUser);

        // Create mailboxes
        List<Mailbox> mailboxes = Arrays.asList(
                Mailbox.builder().name("Inbox").unreadCount(5).userId(demoUser.getId()).build(),
                Mailbox.builder().name("Starred").unreadCount(2).userId(demoUser.getId()).build(),
                Mailbox.builder().name("Sent").unreadCount(0).userId(demoUser.getId()).build(),
                Mailbox.builder().name("Drafts").unreadCount(1).userId(demoUser.getId()).build(),
                Mailbox.builder().name("Archive").unreadCount(0).userId(demoUser.getId()).build(),
                Mailbox.builder().name("Trash").unreadCount(0).userId(demoUser.getId()).build());
        mailboxes = mailboxRepository.saveAll(mailboxes);

        Long inboxId = mailboxes.get(0).getId();

        // Create sample emails
        List<Email> emails = Arrays.asList(
                Email.builder()
                        .mailboxId(inboxId)
                        .userId(demoUser.getId())
                        .fromAddress("john.doe@company.com")
                        .toAddress(demoUser.getEmail())
                        .subject("Project Update - Q4 2024")
                        .preview("Hi team, I wanted to share the latest updates on our Q4 project...")
                        .body(
                                "<p>Hi team,</p><p>I wanted to share the latest updates on our Q4 project. We've made significant progress on the backend architecture and are now moving into the testing phase.</p><p>Key achievements:</p><ul><li>Completed API integration</li><li>Deployed to staging environment</li><li>Initial testing completed</li></ul><p>Best regards,<br>John</p>")
                        .timestamp(LocalDateTime.now().minusHours(2))
                        .isRead(false)
                        .isStarred(false)
                        .build(),

                Email.builder()
                        .mailboxId(inboxId)
                        .userId(demoUser.getId())
                        .fromAddress("sarah.wilson@marketing.com")
                        .toAddress(demoUser.getEmail())
                        .ccAddress("team@marketing.com")
                        .subject("Marketing Campaign Results")
                        .preview("Excited to share our latest campaign results! We exceeded our targets by 25%...")
                        .body(
                                "<p>Hello everyone,</p><p>Excited to share our latest campaign results! We exceeded our targets by 25% and received excellent customer feedback.</p><p>Campaign metrics:</p><ul><li>Impressions: 1.2M</li><li>Click-through rate: 4.5%</li><li>Conversions: 2,500</li></ul><p>Great work team!</p><p>Sarah</p>")
                        .timestamp(LocalDateTime.now().minusHours(5))
                        .isRead(false)
                        .isStarred(true)
                        .build(),

                Email.builder()
                        .mailboxId(inboxId)
                        .userId(demoUser.getId())
                        .fromAddress("notifications@github.com")
                        .toAddress(demoUser.getEmail())
                        .subject("[GitHub] New Pull Request #142")
                        .preview("A new pull request has been opened on your repository EmailClientAI...")
                        .body(
                                "<p>A new pull request has been opened on your repository EmailClientAI.</p><p><strong>PR #142:</strong> Add authentication endpoints</p><p>Changes include:</p><ul><li>JWT token implementation</li><li>Login/logout endpoints</li><li>Google OAuth integration</li></ul><p>View on GitHub: https://github.com/user/EmailClientAI/pull/142</p>")
                        .timestamp(LocalDateTime.now().minusHours(8))
                        .isRead(true)
                        .isStarred(false)
                        .build(),

                Email.builder()
                        .mailboxId(inboxId)
                        .userId(demoUser.getId())
                        .fromAddress("admin@company.com")
                        .toAddress(demoUser.getEmail())
                        .subject("System Maintenance Notification")
                        .preview("Scheduled maintenance will be performed this Saturday from 2 AM to 6 AM...")
                        .body(
                                "<p>Dear user,</p><p>Scheduled maintenance will be performed this Saturday from 2 AM to 6 AM EST.</p><p>During this time, the following services will be unavailable:</p><ul><li>Email access</li><li>File storage</li><li>API endpoints</li></ul><p>We apologize for any inconvenience.</p><p>IT Department</p>")
                        .timestamp(LocalDateTime.now().minusDays(1))
                        .isRead(false)
                        .isStarred(false)
                        .build(),

                Email.builder()
                        .mailboxId(inboxId)
                        .userId(demoUser.getId())
                        .fromAddress("newsletter@techblog.com")
                        .toAddress(demoUser.getEmail())
                        .subject("Weekly Tech Digest - Top 10 Articles")
                        .preview("Here are this week's most popular articles on web development, cloud computing, and AI...")
                        .body(
                                "<p>Hi there,</p><p>Here are this week's most popular articles:</p><ol><li>Building Scalable Microservices with Spring Boot</li><li>Introduction to React Hooks</li><li>AWS Lambda Best Practices</li><li>GraphQL vs REST API</li><li>Machine Learning in Production</li></ol><p>Happy reading!</p>")
                        .timestamp(LocalDateTime.now().minusDays(2))
                        .isRead(false)
                        .isStarred(true)
                        .build(),

                Email.builder()
                        .mailboxId(inboxId)
                        .userId(demoUser.getId())
                        .fromAddress("hr@company.com")
                        .toAddress(demoUser.getEmail())
                        .subject("Annual Performance Review Reminder")
                        .preview("This is a friendly reminder that your annual performance review is scheduled for next week...")
                        .body(
                                "<p>Dear employee,</p><p>This is a friendly reminder that your annual performance review is scheduled for next week.</p><p><strong>Date:</strong> November 25, 2024<br><strong>Time:</strong> 10:00 AM<br><strong>Location:</strong> Conference Room B</p><p>Please prepare your self-assessment and bring any supporting documents.</p><p>Best regards,<br>HR Team</p>")
                        .timestamp(LocalDateTime.now().minusDays(3))
                        .isRead(true)
                        .isStarred(false)
                        .build(),

                Email.builder()
                        .mailboxId(inboxId)
                        .userId(demoUser.getId())
                        .fromAddress("billing@cloudservice.com")
                        .toAddress(demoUser.getEmail())
                        .subject("Your November Invoice is Ready")
                        .preview("Your invoice for November 2024 is now available. Total amount: $149.99...")
                        .body(
                                "<p>Hello,</p><p>Your invoice for November 2024 is now available.</p><p><strong>Invoice Details:</strong></p><ul><li>Invoice #: INV-2024-11-001</li><li>Total Amount: $149.99</li><li>Due Date: December 5, 2024</li></ul><p>View your invoice at: https://cloudservice.com/billing/invoices</p><p>Thank you for your business!</p>")
                        .timestamp(LocalDateTime.now().minusDays(5))
                        .isRead(true)
                        .isStarred(false)
                        .build(),

                Email.builder()
                        .mailboxId(inboxId)
                        .userId(demoUser.getId())
                        .fromAddress("support@emailclient.com")
                        .toAddress(demoUser.getEmail())
                        .subject("Welcome to EmailClientAI!")
                        .preview("Thank you for signing up! Let's get you started with our powerful email management features...")
                        .body(
                                "<p>Welcome to EmailClientAI!</p><p>Thank you for signing up! We're excited to help you manage your emails more efficiently.</p><p><strong>Getting Started:</strong></p><ol><li>Organize your emails with smart folders</li><li>Use AI-powered search to find anything instantly</li><li>Set up filters and rules for automatic sorting</li><li>Connect multiple email accounts</li></ol><p>Need help? Visit our Help Center or contact support@emailclient.com</p><p>Happy emailing!</p>")
                        .timestamp(LocalDateTime.now().minusDays(7))
                        .isRead(true)
                        .isStarred(true)
                        .build());

        emailRepository.saveAll(emails);
    }
}
