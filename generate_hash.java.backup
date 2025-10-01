import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class generate_hash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "admin123";
        String encodedPassword = encoder.encode(rawPassword);
        System.out.println("Generated hash: " + encodedPassword);
        System.out.println("Verification: " + encoder.matches(rawPassword, encodedPassword));
    }
}