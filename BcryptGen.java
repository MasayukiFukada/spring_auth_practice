import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class BcryptGen {
    public static void main(String[] args) {
        BCryptPasswordEncoder e = new BCryptPasswordEncoder();
        String pw = args.length>0?args[0]:"testpass";
        System.out.println(e.encode(pw));
    }
}
