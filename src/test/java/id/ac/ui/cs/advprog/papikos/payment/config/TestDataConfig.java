package id.ac.ui.cs.advprog.papikos.payment.config;

import id.ac.ui.cs.advprog.papikos.payment.model.User;
import id.ac.ui.cs.advprog.papikos.payment.model.UserRole;
import id.ac.ui.cs.advprog.papikos.payment.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class TestDataConfig {

    @Bean
    public CommandLineRunner initTestData(UserRepository userRepository) {
        return args -> {
            User penyewa = new User();
            penyewa.setId("user1");
            penyewa.setUsername("penyewa1");
            penyewa.setEmail("penyewa1@example.com");
            penyewa.setRole(UserRole.PENYEWA);

            User pemilik = new User();
            pemilik.setId("user2");
            pemilik.setUsername("pemilik1");
            pemilik.setEmail("pemilik1@example.com");
            pemilik.setRole(UserRole.PEMILIK_KOS);

            userRepository.save(penyewa);
            userRepository.save(pemilik);
        };
    }
}
