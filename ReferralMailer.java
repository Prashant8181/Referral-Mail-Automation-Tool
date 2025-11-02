import java.io.*;
import java.util.*;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import com.opencsv.CSVReader;
import java.io.UnsupportedEncodingException;


public class ReferralMailer {

    public static void main(String[] args) {
          int count = 0;
          int count2=0;

        try {
            // Load config.properties
            Properties config = new Properties();
            config.load(new FileInputStream("config.properties"));

            final String senderEmail = config.getProperty("sender.email");
            final String appPassword = config.getProperty("app.password");
            final String resumePath = config.getProperty("resume.path");
            int batchSize = Integer.parseInt(config.getProperty("batch.size"));
            long pauseMs = Long.parseLong(config.getProperty("pause.ms"));

            // Read email template
            String template = new String(java.nio.file.Files.readAllBytes(
                    java.nio.file.Paths.get("email_template.txt")));

            // Mail server settings
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");

            Session session = Session.getInstance(props,
                new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(senderEmail, appPassword);
                    }
                });

            // Read HR data from CSV
           CSVReader reader = new CSVReader(new FileReader("hr_contacts.csv"));
String[] line;
reader.readNext(); // Skip header
while ((line = reader.readNext()) != null) {
    String name = line[1].trim();     // column 1 = Name
    String email = line[2].trim();    // column 2 = Email
    String title = line[3].trim();
    String company = line[4].trim();
count2++;

    // Skip invalid or incomplete email addresses
    if (!email.contains("@") || email.endsWith(".") || !email.contains(".")) {
        System.out.println("Skipping invalid email: " + email);
        continue;
    }


                // Replace placeholders
                String body = template.replace("{name}", name)
                                      .replace("{company}", company);

                String subject = "Interested in " + company + " Could you kindly refer me?";
                
                System.out.println(count2 +" name: " + name + " | email: " + email);

// Create message
Message message = new MimeMessage(session);
message.setFrom(new InternetAddress(senderEmail, "Prashant Natekar", "UTF-8"));
message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
message.setSubject(subject);

// Read and personalize the email template
// Add <b> tags ONLY where you want text bold
String emailBody = template.replace("{name}", "<b>" + name + "</b>")
                           .replace("{company}", "<b>" + company + "</b>")
                           .replace("{importantLine}", "<b>Important Note:</b>"); // example extra bold

// Keep mail format same — just convert newlines to <br> for HTML
String htmlBody = emailBody.replace("\n", "<br>");

// Body part (HTML content)
MimeBodyPart textPart = new MimeBodyPart();
textPart.setContent(htmlBody, "text/html; charset=utf-8");

// Attachment part
MimeBodyPart attachmentPart = new MimeBodyPart();
attachmentPart.attachFile(new File(resumePath));

// Combine both parts
Multipart multipart = new MimeMultipart();
multipart.addBodyPart(textPart);
multipart.addBodyPart(attachmentPart);

// Final message content
message.setContent(multipart);

               
                // Send mail
                Transport.send(message);
                System.out.println("✅ Sent to " + name + " (" + company + ")");

                count++;
                
                if (count % batchSize == 0) {
                    System.out.println("Waiting 1 hour before next batch...");
                    Thread.sleep(pauseMs);
                }
            }

            System.out.println("All emails sent successfully!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
