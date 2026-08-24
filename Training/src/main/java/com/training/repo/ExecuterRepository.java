package com.training.repo;

import com.training.entity.Executer;
import com.training.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExecuterRepository extends JpaRepository<Executer, Long> {
    Optional<Executer> findByUser(User user);
    Optional<Executer> findByUserEmail(String email);
    Optional<Executer> findByExecutorCode(String executorCode);
}
