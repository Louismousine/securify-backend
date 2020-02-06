package com.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.entities.*;

@Repository
public interface SystemRepository extends JpaRepository<PlaylistMetadataEntity, Integer>{
}
