package com.capstone.backend.shop.repository;

import com.capstone.backend.shop.entity.UserItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserItemRepository extends JpaRepository<UserItem, Long> {

    List<UserItem> findByUser_Id(Long userId);

    Optional<UserItem> findByUser_IdAndItem_Id(Long userId, Long itemId);
}
