package mw.nwra.ewaterpermit.service;

import java.util.Base64;
import java.util.Base64.Decoder;
import java.io.InputStream;
import javax.activation.DataSource;

import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.email.EmailPopulatingBuilder;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.config.ConfigLoader;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;

import mw.nwra.ewaterpermit.exception.ForbiddenException;
import mw.nwra.ewaterpermit.model.SysConfig;
import mw.nwra.ewaterpermit.model.SysEmailTemplate;
import mw.nwra.ewaterpermit.model.SysUserAccount;

@Service(value = "mailingService")
public class MailingService {
	SysConfig config;
	SysEmailTemplate template;
	@Autowired
	SysConfigService configService;
	@Autowired
	SysEmailTemplateService templateService;

	@Value("${server.port:8080}")
	private int serverPort;

	@Value("${server.servlet.context-path:/api/nwra-apis/ewaterpermit-ws}")
	private String contextPath;

	private Mailer mailer;
	private Boolean messageSent = false;

	public void init(String templateName) {
		ConfigLoader.loadProperties("simplejavamail.properties", false); // optional default
		// Load config from database including email credentials
		this.config = this.configService.getSystemConfig();
		this.template = this.templateService.getSysEmailTemplatesByNameAndStatus(templateName, (short) 1).get(0);
		try {
			// Use database email configuration
			String mailHost = this.config.getSystemEmailSmtp();
			int mailPort = this.config.getSystemEmailPort();
			String mailUsername = this.config.getSystemEmailAddress();
			String mailPassword = this.config.getSystemEmailAuth();

			System.out.println("=== EMAIL CONFIG FROM DATABASE ===");
			System.out.println("SMTP Host: " + mailHost);
			System.out.println("SMTP Port: " + mailPort);
			System.out.println("Email From: " + mailUsername);
			System.out.println("Security: " + this.config.getSystemEmailSmtpSecurity());
			System.out.println("===================================");

			this.mailer = MailerBuilder
					.withSMTPServer(mailHost, mailPort, mailUsername, mailPassword)
					.withTransportStrategy(TransportStrategy.SMTP_TLS) // Use STARTTLS for Gmail
					.withSessionTimeout(10 * 1000)
					.withProperty("mail.smtp.sendpartial", true)
					.async().buildMailer();
		} catch (Exception e) {
			e.printStackTrace();
			throw new ForbiddenException("Failed to send email");
		}
	}

	public boolean send(String templateName, String token, SysUserAccount user) {
		return send(templateName, token, user, null);
	}

	public boolean send(String templateName, String token, SysUserAccount user, String registrationHost) {
		this.init(templateName);
		String htmlTemplate = this.template.getValue();

		// Get backend URL from database configuration
		String backendBaseUrl = this.config.getBackendUrl();

		// If backend URL is not configured in database, try to derive it dynamically
		if (backendBaseUrl == null || backendBaseUrl.trim().isEmpty()) {
			// Fallback: derive from frontend URL or use request headers
			if (registrationHost != null && !registrationHost.trim().isEmpty()) {
				if (registrationHost.contains("localhost")) {
					// Local development
					backendBaseUrl = "http://localhost:" + serverPort + contextPath;
				} else {
					// Production/Staging - derive backend URL from frontend URL
					String protocol = registrationHost.startsWith("https") ? "https" : "http";
					String frontendHost = registrationHost.replaceAll("^https?://", "");
					String backendHost = "api." + frontendHost;
					backendBaseUrl = protocol + "://" + backendHost + contextPath;
				}
			} else {
				// No info available - use local as default
				backendBaseUrl = "http://localhost:" + serverPort + contextPath;
			}
		}

		System.out.println("=== BACKEND URL CONFIGURATION ===");
		System.out.println("Database Backend URL: " + this.config.getBackendUrl());
		System.out.println("Derived Backend URL: " + backendBaseUrl);
		System.out.println("================================");

		// Handle different template types - password reset needs frontend URL
		if (templateName != null && templateName.toUpperCase().contains("PASSWORD_RESET")) {
			// Get frontend URL from database config first, then fallback to registrationHost
			String frontendUrl = this.config.getFrontendUrl();
			if (frontendUrl == null || frontendUrl.trim().isEmpty()) {
				if (registrationHost != null && !registrationHost.trim().isEmpty()) {
					frontendUrl = registrationHost;
				} else {
					// Last resort fallback - try to derive from backend URL
					frontendUrl = backendBaseUrl.replace("/api/nwra-apis/ewaterpermit-ws", "");
				}
			}

			System.out.println("=== PASSWORD RESET URL CONFIGURATION ===");
			System.out.println("Database Frontend URL: " + this.config.getFrontendUrl());
			System.out.println("Registration Host: " + registrationHost);
			System.out.println("Final Frontend URL: " + frontendUrl);
			System.out.println("========================================");

			// Create frontend password reset URL
			String passwordResetUrl = frontendUrl + "/auth/reset-password?token=" + token;

			// Replace password reset specific placeholders with frontend URL
			htmlTemplate = htmlTemplate.replaceAll("rbm_system_url_rbm/auth/reset-password/rbm_id_rbm", passwordResetUrl);
			htmlTemplate = htmlTemplate.replaceAll("rbm_system_url_rbm/auth/rbm_id_rbm/reset-password", passwordResetUrl);
			htmlTemplate = htmlTemplate.replaceAll("rbm_system_url_rbm", frontendUrl);
			htmlTemplate = htmlTemplate.replaceAll("rbm_system_name_rbm", this.config.getSystemName());
			htmlTemplate = htmlTemplate.replaceAll("rbm_id_rbm", token);
		} else {
			// Account confirmation or other templates - use backend URL
			String confirmationUrl = backendBaseUrl + "/v1/auth/" + token + "/confirm-account";

			// Replace placeholders in template - handle specific patterns first
			htmlTemplate = htmlTemplate.replaceAll("rbm_system_url_rbm/auth/confirm-account/rbm_id_rbm", confirmationUrl);
			htmlTemplate = htmlTemplate.replaceAll("rbm_system_url_rbm/auth/rbm_id_rbm/confirm-account", confirmationUrl);
			htmlTemplate = htmlTemplate.replaceAll("rbm_system_url_rbm", backendBaseUrl);
			htmlTemplate = htmlTemplate.replaceAll("rbm_system_name_rbm", this.config.getSystemName());
			htmlTemplate = htmlTemplate.replaceAll("rbm_id_rbm", token);
		}

		// Ensure logo is embedded in the template - check if it's already there
		if (!htmlTemplate.contains("cid:logo")) {
			// Logo not in template - add it based on template structure
			String logoHtml = "<div style=\"text-align: center; padding: 20px 0; margin-bottom: 20px; border-bottom: 1px solid #e2e8f0;\"><img src=\"cid:logo\" alt=\"NWRA Logo\" style=\"width: 100px; height: 100px; background-color: white; padding: 8px; border-radius: 6px;\"></div>";

			// Try to find the best insertion point
			boolean logoInjected = false;

			// Strategy 1: Replace rbm_logo_rbm placeholder if it exists
			if (htmlTemplate.contains("rbm_logo_rbm")) {
				htmlTemplate = htmlTemplate.replaceAll("rbm_logo_rbm", logoHtml);
				logoInjected = true;
			}
			// Strategy 2: Inject after opening body tag
			else if (htmlTemplate.contains("<body")) {
				htmlTemplate = htmlTemplate.replaceFirst("(<body[^>]*>)", "$1" + logoHtml);
				logoInjected = true;
			}
			// Strategy 3: Look for white background container or content div
			else if (htmlTemplate.contains("<div") && (htmlTemplate.contains("background") || htmlTemplate.contains("container"))) {
				htmlTemplate = htmlTemplate.replaceFirst("(<div[^>]*(?:background[^>]*white|container)[^>]*>)", "$1" + logoHtml);
				logoInjected = true;
			}

			// Strategy 4: Last resort - prepend to entire content
			if (!logoInjected) {
				htmlTemplate = logoHtml + htmlTemplate;
			}

			System.out.println("Logo HTML injected into template (was missing cid:logo reference)");
		} else {
			System.out.println("Template already contains cid:logo reference");
		}
		
		// Replace any direct links with clickable button HTML
		if (htmlTemplate.contains("<a href=\"") && !htmlTemplate.contains("style=\"")) {
			// Convert plain links to styled buttons
			htmlTemplate = htmlTemplate.replaceAll(
				"<a href=\"([^\"]+)\">([^<]+)</a>",
				"<a href=\"$1\" style=\"display: inline-block; padding: 12px 24px; background-color: #007bff; color: white; text-decoration: none; border-radius: 4px; font-weight: bold; margin: 10px 0;\">$2</a>"
			);
		}

		htmlTemplate = htmlTemplate.replaceAll("rbm_action_type_rbm", templateName.toLowerCase().replaceAll("_", " "));
		htmlTemplate = htmlTemplate.replaceAll("rbm_contact_postal_address_rbm", this.config.getContactPostalAddress());

		// Debug: Print template snippet to verify logo injection
		System.out.println("=== EMAIL TEMPLATE PREVIEW (first 500 chars) ===");
		System.out.println(htmlTemplate.substring(0, Math.min(500, htmlTemplate.length())));
		System.out.println("================================================");

		// Build email - use database email address
		EmailPopulatingBuilder emailBuilder = EmailBuilder.startingBlank()
				.from(this.config.getSystemEmailAddress())
				.to(user.getFirstName() + " " + user.getLastName(), user.getEmailAddress())
				.withSubject(this.config.getSystemName())
				.withHTMLText(htmlTemplate);

		// Add logo as inline embedded image (required for cid:logo to work)
		try {
			ClassPathResource logoResource = new ClassPathResource("static/images/logo_docs.png");
			if (logoResource.exists()) {
				InputStream logoStream = logoResource.getInputStream();
				emailBuilder.withEmbeddedImage("logo", logoStream.readAllBytes(), "image/png");
				logoStream.close();
				System.out.println("Logo embedded successfully in email");
			} else {
				System.out.println("Warning: Logo not found at static/images/logo_docs.png");
			}
		} catch (Exception logoEx) {
			System.out.println("Warning: Could not embed logo: " + logoEx.getMessage());
			// Continue without logo if not found
		}

		Email email = emailBuilder.buildEmail();
		try {
			// Send email synchronously to avoid async timing issues
			mailer.sendMail(email, false); // false = synchronous
			this.messageSent = true;
			System.out.println("Email sent successfully!");
		} catch (Exception e) {
			System.err.println("Failed to send email: " + e.getMessage());
			e.printStackTrace();
			this.messageSent = false;
			throw new ForbiddenException("Failed to send email: " + e.getMessage());
		} finally {
			if (this.mailer != null) {
				try {
					this.mailer.shutdownConnectionPool();
				} catch (Exception e) {
					System.err.println("Error shutting down mailer: " + e.getMessage());
				}
			}
		}
		return this.messageSent;
	}

	public boolean sendEmailWithAttachment(String toEmail, String subject, String body, byte[] attachment, String attachmentName) {
		try {
			// Initialize with default template
			this.init("default");

			// Build email with attachment - use database email address
			EmailPopulatingBuilder emailBuilder = EmailBuilder.startingBlank()
				.from(this.config.getSystemEmailAddress())
				.to(toEmail)
				.withSubject(subject)
				.withHTMLText(body)
				.withAttachment(attachmentName, attachment, "application/pdf");

			Email email = emailBuilder.buildEmail();
			
			// Send email
			mailer.sendMail(email, true);
			mailer.testConnection(true).whenComplete((result, ex) -> {
				if (ex != null) {
					System.err.printf("Execution failed %s", ex);
				} else {
					System.err.printf("Execution completed: %s", result);
					this.messageSent = true;
				}
			});
			
			return this.messageSent;
		} catch (Exception e) {
			e.printStackTrace();
			throw new ForbiddenException("Failed to send email with attachment");
		} finally {
			this.mailer.shutdownConnectionPool();
		}
	}
}
