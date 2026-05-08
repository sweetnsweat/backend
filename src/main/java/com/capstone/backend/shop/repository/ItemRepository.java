package com.capstone.backend.shop.repository;

import com.capstone.backend.shop.entity.Item;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByActiveTrueOrderByIdAsc();

    List<Item> findByItemTypeAndActiveTrueOrderByIdAsc(String itemType);

    Optional<Item> findByIdAndActiveTrue(Long id);
}
