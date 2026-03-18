package com.melearning.elearning.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    /** Regisztrációs üdvözlő email */
    public void sendRegistrationEmail(String toEmail, String firstName) {
        String subject = "Üdvözlünk a ME-Learning platformon!";
        String body = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
              <div style="background:#0D1321;padding:24px;text-align:center;border-radius:12px 12px 0 0;">
                <h1 style="color:#fff;margin:0;font-size:22px;">🎓 ME-Learning</h1>
              </div>
              <div style="background:#fff;padding:32px;border:1px solid #E5E9F2;">
                <h2 style="color:#111827;">Szia, %s!</h2>
                <p style="color:#4B5563;line-height:1.6;">
                  Sikeresen regisztráltál a ME-Learning platformra. 
                  Mostantól elérheted az összes kurzust, kvízt és prezentációt.
                </p>
                <div style="text-align:center;margin:28px 0;">
                  <a href="%s/courses"
                     style="background:#3B6FE8;color:#fff;padding:14px 28px;border-radius:10px;
                            text-decoration:none;font-weight:700;font-size:15px;">
                    Kurzusok böngészése →
                  </a>
                </div>
                <p style="color:#9CA3AF;font-size:13px;">
                  Ha nem te regisztráltál, hagyd figyelmen kívül ezt az emailt.
                </p>
              </div>
              <div style="background:#F8FAFF;padding:16px;text-align:center;
                          border-radius:0 0 12px 12px;border:1px solid #E5E9F2;border-top:none;">
                <p style="color:#9CA3AF;font-size:12px;margin:0;">
                  © 2024 ME-Learning – me.learning.admin@gmail.com
                </p>
              </div>
            </div>
            """.formatted(firstName, baseUrl);

        sendHtmlEmail(toEmail, subject, body);
    }

    /** Jelszóváltoztatás értesítő email */
    public void sendPasswordChangeEmail(String toEmail, String firstName) {
        String subject = "Jelszavad megváltozott – ME-Learning";
        String body = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
              <div style="background:#0D1321;padding:24px;text-align:center;border-radius:12px 12px 0 0;">
                <h1 style="color:#fff;margin:0;font-size:22px;">🎓 ME-Learning</h1>
              </div>
              <div style="background:#fff;padding:32px;border:1px solid #E5E9F2;">
                <h2 style="color:#111827;">Szia, %s!</h2>
                <div style="background:#FFFBEB;border:1px solid #FDE68A;border-radius:10px;
                            padding:16px;margin-bottom:20px;">
                  <p style="color:#92400E;margin:0;font-weight:600;">
                    ⚠️ Fiókod jelszava megváltozott.
                  </p>
                </div>
                <p style="color:#4B5563;line-height:1.6;">
                  Ha te módosítottad a jelszavad, nincs teendőd.<br>
                  Ha <strong>nem te</strong> változtattad meg, azonnal vedd fel velünk a kapcsolatot!
                </p>
                <div style="text-align:center;margin:28px 0;">
                  <a href="%s/login"
                     style="background:#3B6FE8;color:#fff;padding:14px 28px;border-radius:10px;
                            text-decoration:none;font-weight:700;font-size:15px;">
                    Bejelentkezés →
                  </a>
                </div>
              </div>
              <div style="background:#F8FAFF;padding:16px;text-align:center;
                          border-radius:0 0 12px 12px;border:1px solid #E5E9F2;border-top:none;">
                <p style="color:#9CA3AF;font-size:12px;margin:0;">
                  © 2024 ME-Learning – me.learning.admin@gmail.com
                </p>
              </div>
            </div>
            """.formatted(firstName, baseUrl);

        sendHtmlEmail(toEmail, subject, body);
    }

    /** Fiók törlés értesítő email */
    public void sendAccountDeletionEmail(String toEmail, String firstName) {
        String subject = "Fiókod törölve lett – ME-Learning";
        String body = """
        <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
          <div style="background:#0D1321;padding:24px;text-align:center;border-radius:12px 12px 0 0;">
            <h1 style="color:#fff;margin:0;font-size:22px;">🎓 ME-Learning</h1>
          </div>
          <div style="background:#fff;padding:32px;border:1px solid #E5E9F2;">
            <h2 style="color:#111827;">Szia, %s!</h2>
            <div style="background:#FFF1F2;border:1px solid #FECDD3;border-radius:10px;
                        padding:16px;margin-bottom:20px;">
              <p style="color:#9F1239;margin:0;font-weight:600;">
                ❌ A fiókod véglegesen törölve lett.
              </p>
            </div>
            <p style="color:#4B5563;line-height:1.6;">
              Sajnáljuk, hogy elhagysz minket! A fiókodhoz tartozó összes adat
              (beiratkozások, kvíz eredmények) törlésre kerültek.
            </p>
            <p style="color:#4B5563;line-height:1.6;">
              Ha úgy döntesz, hogy visszatérsz, bármikor regisztrálhatsz újra.
            </p>
            <div style="text-align:center;margin:28px 0;">
              <a href="%s/register"
                 style="background:#3B6FE8;color:#fff;padding:14px 28px;border-radius:10px;
                        text-decoration:none;font-weight:700;font-size:15px;">
                Új fiók létrehozása →
              </a>
            </div>
            <p style="color:#9CA3AF;font-size:13px;">
              Ha nem te kérted a törlést, azonnal vedd fel velünk a kapcsolatot:
              <a href="mailto:me.learning.admin@gmail.com" style="color:#3B6FE8;">me.learning.admin@gmail.com</a>
            </p>
          </div>
          <div style="background:#F8FAFF;padding:16px;text-align:center;
                      border-radius:0 0 12px 12px;border:1px solid #E5E9F2;border-top:none;">
            <p style="color:#9CA3AF;font-size:12px;margin:0;">
              © 2024 ME-Learning – me.learning.admin@gmail.com
            </p>
          </div>
        </div>
        """.formatted(firstName, baseUrl);

        sendHtmlEmail(toEmail, subject, body);
    }

    /** Belső segédmetódus HTML email küldéséhez */
    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML
            mailSender.send(message);
        } catch (MessagingException e) {
            // Logolhatod, de ne állítsd meg az alkalmazást email hiba miatt
            System.err.println("Email küldési hiba: " + e.getMessage());
        }
    }
}