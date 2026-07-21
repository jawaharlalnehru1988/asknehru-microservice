import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class CheckPassword {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "Murari#1988";
        String encodedPassword = "$2a$12$FBK5uNRp1pstYeSJ9FoTl.vzs7CiHYI/xdBxcblYh0i4Jb7HfEqy6";
        boolean isMatch = encoder.matches(rawPassword, encodedPassword);
        System.out.println("Match: " + isMatch);
    }
}
