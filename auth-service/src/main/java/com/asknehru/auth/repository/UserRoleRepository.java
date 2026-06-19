package com.asknehru.auth.repository;

import com.asknehru.auth.model.UserRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    boolean existsByUserIdAndRoleId(Long userId, Long roleId);

    @Query(value = """
            SELECT r.name
            FROM auth_user_roles ur
            JOIN auth_roles r ON r.id = ur.role_id
            WHERE ur.user_id = :userId
            """, nativeQuery = true)
    List<String> findRoleNamesByUserId(@Param("userId") Long userId);
}
