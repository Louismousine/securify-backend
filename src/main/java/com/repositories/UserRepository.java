package com.repositories;
import org.springframework.data.jpa.repository.JpaRepository;

import com.entities.*;

public interface UserRepository extends JpaRepository<UserEntity, Long>{
	UserEntity findByEmail(String email);

	UserEntity findByUsername(String username);
	UserEntity findByToken(String token);
}
