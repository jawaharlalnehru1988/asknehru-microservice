package com.asknehru.user.repository;

import com.asknehru.user.model.DjangoUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DjangoUserRepository extends JpaRepository<DjangoUser, Integer> {

    List<DjangoUser> findAllByOrderByIdAsc();

    Optional<DjangoUser> findByUsernameIgnoreCase(String username);

    Optional<DjangoUser> findByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Integer id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);
}
