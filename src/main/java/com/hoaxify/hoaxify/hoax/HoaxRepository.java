package com.hoaxify.hoaxify.hoax;

import com.hoaxify.hoaxify.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoaxRepository extends JpaRepository<Hoax, Long> {

    long countByUserUsername(String username);

    Page<Hoax> findByUser(User user, Pageable pageable);

    Page<Hoax> findByIdLessThan(long id, Pageable pageable);

    Page<Hoax> findByIdLessThanAndUser(long id, User user, Pageable pageable);
}
