package com.jurisfacil.iam.repository;

import com.jurisfacil.iam.model.entity.UserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    Optional<UserEntity> findByResetTokenHash(String resetTokenHash);

    boolean existsByEmail(String email);
}
