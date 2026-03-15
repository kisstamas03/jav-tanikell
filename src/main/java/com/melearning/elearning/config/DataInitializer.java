package com.melearning.elearning.config;

import com.melearning.elearning.model.Course;
import com.melearning.elearning.model.Lesson;
import com.melearning.elearning.model.Role;
import com.melearning.elearning.model.User;
import com.melearning.elearning.repository.CourseRepository;
import com.melearning.elearning.repository.LessonRepository;
import com.melearning.elearning.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Ellenőrizzük, hogy már vannak-e felhasználók az adatbázisban
        if (userRepository.count() == 0) {
            initializeUsers();
            //initializeCourses();
        }
    }

    private void initializeUsers() {
        // Admin felhasználó létrehozása
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@elearning.hu");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setFirstName("Admin");
        admin.setLastName("Felhasználó");
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        // Test oktató létrehozása
        User instructor = new User();
        instructor.setUsername("instructor");
        instructor.setEmail("instructor@elearning.hu");
        instructor.setPassword(passwordEncoder.encode("instructor123"));
        instructor.setFirstName("Teszt");
        instructor.setLastName("Oktató");
        instructor.setRole(Role.INSTRUCTOR);
        userRepository.save(instructor);

        // Test hallgató létrehozása
        User student = new User();
        student.setUsername("student");
        student.setEmail("student@elearning.hu");
        student.setPassword(passwordEncoder.encode("student123"));
        student.setFirstName("Teszt");
        student.setLastName("Hallgató");
        student.setRole(Role.STUDENT);
        userRepository.save(student);

        // További oktatók létrehozása
        User instructor2 = new User();
        instructor2.setUsername("kovacs.janos");
        instructor2.setEmail("kovacs.janos@elearning.hu");
        instructor2.setPassword(passwordEncoder.encode("password123"));
        instructor2.setFirstName("János");
        instructor2.setLastName("Kovács");
        instructor2.setRole(Role.INSTRUCTOR);
        userRepository.save(instructor2);

        User instructor3 = new User();
        instructor3.setUsername("nagy.anna");
        instructor3.setEmail("nagy.anna@elearning.hu");
        instructor3.setPassword(passwordEncoder.encode("password123"));
        instructor3.setFirstName("Anna");
        instructor3.setLastName("Nagy");
        instructor3.setRole(Role.INSTRUCTOR);
        userRepository.save(instructor3);

        System.out.println("=== TESZT FELHASZNÁLÓK LÉTREHOZVA ===");
        System.out.println("Admin - Username: admin, Password: admin123");
        System.out.println("Oktató - Username: instructor, Password: instructor123");
        System.out.println("Hallgató - Username: student, Password: student123");
        System.out.println("Oktató 2 - Username: kovacs.janos, Password: password123");
        System.out.println("Oktató 3 - Username: nagy.anna, Password: password123");
        System.out.println("=====================================");
    }

    /*
    private void initializeCourses() {
        // Oktatók lekérése
        User instructor1 = userRepository.findByUsername("instructor").orElse(null);
        User instructor2 = userRepository.findByUsername("kovacs.janos").orElse(null);
        User instructor3 = userRepository.findByUsername("nagy.anna").orElse(null);

        if (instructor1 != null) {
            // Java alapok kurzus
            Course javaCourse = new Course();
            javaCourse.setTitle("Java Programozás Alapjai");
            javaCourse.setDescription("Tanuld meg a Java programozási nyelv alapjait nulláról. A kurzus során megismered a változókat, ciklusokat, függvényeket és az objektumorientált programozás alapjait.");
            javaCourse.setInstructor(instructor1);
            courseRepository.save(javaCourse);

            // Leckék hozzáadása
            createLesson(javaCourse, "Bevezetés a Java-ba", "A Java programozási nyelv története és jellemzői. Fejlesztői környezet beállítása.", 1);
            createLesson(javaCourse, "Változók és adattípusok", "Primitív adattípusok, változók deklarálása és inicializálása.", 2);
            createLesson(javaCourse, "Operátorok és kifejezések", "Aritmetikai, logikai és összehasonlító operátorok használata.", 3);
            createLesson(javaCourse, "Vezérlési szerkezetek", "If-else, switch, ciklusok (for, while, do-while).", 4);
            createLesson(javaCourse, "Tömbök", "Egydimenziós és többdimenziós tömbök kezelése.", 5);

            // Web fejlesztés kurzus
            Course webCourse = new Course();
            webCourse.setTitle("Modern Webfejlesztés");
            webCourse.setDescription("Komplett webfejlesztői kurzus HTML5, CSS3, JavaScript és React technológiákkal. Gyakorlati projektekkel.");
            webCourse.setInstructor(instructor1);
            courseRepository.save(webCourse);

            createLesson(webCourse, "HTML5 alapok", "Szemantikus HTML elemek, űrlapok, multimédia beágyazás.", 1);
            createLesson(webCourse, "CSS3 és reszponzív design", "Modern CSS technikák, Flexbox, Grid, Media Queries.", 2);
            createLesson(webCourse, "JavaScript alapok", "Változók, függvények, eseménykezelés, DOM manipuláció.", 3);
            createLesson(webCourse, "React bevezetés", "Komponensek, props, state kezelés.", 4);
        }

        if (instructor2 != null) {
            // Python kurzus
            Course pythonCourse = new Course();
            pythonCourse.setTitle("Python Adatelemzés");
            pythonCourse.setDescription("Python programozás adatelemzéshez és gépi tanuláshoz. Pandas, NumPy, Matplotlib könyvtárak használata.");
            pythonCourse.setInstructor(instructor2);
            courseRepository.save(pythonCourse);

            createLesson(pythonCourse, "Python alapok", "Szintaxis, adattípusok, vezérlési szerkezetek.", 1);
            createLesson(pythonCourse, "NumPy bevezetés", "Tömbök kezelése, matematikai műveletek.", 2);
            createLesson(pythonCourse, "Pandas DataFrame", "Adatok betöltése, tisztítása, transzformálása.", 3);
            createLesson(pythonCourse, "Adatvizualizáció", "Grafikonok készítése Matplotlib és Seaborn-nal.", 4);

            // Adatbázis kurzus
            Course dbCourse = new Course();
            dbCourse.setTitle("Adatbázis Tervezés és SQL");
            dbCourse.setDescription("Relációs adatbázisok tervezése, SQL lekérdezések, indexelés és optimalizálás.");
            dbCourse.setInstructor(instructor2);
            courseRepository.save(dbCourse);

            createLesson(dbCourse, "Adatbázis alapfogalmak", "Relációs modell, entitások, kapcsolatok.", 1);
            createLesson(dbCourse, "SQL alapok", "SELECT, INSERT, UPDATE, DELETE műveletek.", 2);
            createLesson(dbCourse, "Összetett lekérdezések", "JOIN műveletek, alkérdések, agregáló függvények.", 3);
        }

        if (instructor3 != null) {
            // UI/UX Design kurzus
            Course designCourse = new Course();
            designCourse.setTitle("UI/UX Design Alapjai");
            designCourse.setDescription("Felhasználói élmény tervezése, interface design, prototípus készítés, használhatósági tesztelés.");
            designCourse.setInstructor(instructor3);
            courseRepository.save(designCourse);

            createLesson(designCourse, "UX alapelvek", "Felhasználóközpontú tervezés, user journey mapping.", 1);
            createLesson(designCourse, "Wireframing", "Alacsony szintű prototípusok készítése.", 2);
            createLesson(designCourse, "UI Design", "Színek, tipográfia, ikonok, képek használata.", 3);
            createLesson(designCourse, "Prototípus készítés", "Interaktív prototípusok Figma-ban.", 4);

            // Digital Marketing kurzus
            Course marketingCourse = new Course();
            marketingCourse.setTitle("Digitális Marketing Stratégia");
            marketingCourse.setDescription("Online marketing eszközök és stratégiák. SEO, SEM, social media marketing, email marketing.");
            marketingCourse.setInstructor(instructor3);
            courseRepository.save(marketingCourse);

            createLesson(marketingCourse, "Digitális marketing alapok", "Online marketing csatornák áttekintése.", 1);
            createLesson(marketingCourse, "SEO optimalizálás", "Keresőoptimalizálás technikái és eszközei.", 2);
            createLesson(marketingCourse, "Google Ads", "Fizetett hirdetések beállítása és optimalizálása.", 3);
            createLesson(marketingCourse, "Social Media Marketing", "Facebook, Instagram, LinkedIn marketing.", 4);
        }

        System.out.println("=== TESZT KURZUSOK LÉTREHOZVA ===");
        System.out.println("Összesen " + courseRepository.count() + " kurzus lett létrehozva");
        System.out.println("Összesen " + lessonRepository.count() + " lecke lett létrehozva");
        System.out.println("==================================");
    }*/

    private void createLesson(Course course, String title, String content, int order) {
        Lesson lesson = new Lesson();
        lesson.setTitle(title);
        lesson.setContent(content);
        lesson.setOrderIndex(order);
        lesson.setCourse(course);
        lessonRepository.save(lesson);
    }
}