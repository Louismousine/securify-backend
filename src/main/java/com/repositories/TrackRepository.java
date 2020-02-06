package com.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.entities.*;

@Repository
public interface TrackRepository extends JpaRepository<TrackEntity, Long>{
	TrackEntity findBySongid(String songID);
	TrackEntity findByUserid(Long userID);
	TrackEntity findByPosition(int position);
	boolean existsByPosition(int position);
}
