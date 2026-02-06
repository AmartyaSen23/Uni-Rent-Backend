package com.campusshare.backend.repository;

import com.campusshare.backend.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> {
    
    // Existing Finder Methods
    List<Item> findByLenderEmail(String lenderEmail);
    List<Item> findByBorrowerEmail(String borrowerEmail);
    List<Item> findByStatus(String status, Pageable pageable);
    
    // Danger Zone: Bulk Delete
    void deleteByLenderEmail(String email);

    // 🛡️ SCALABILITY UPGRADE: Atomic Update Lock
    // This query tries to update the row ONLY IF the status is currently 'AVAILABLE'.
    // If someone else grabbed it 1ms ago, 'status' won't be 'AVAILABLE', and this returns 0.
    @Transactional
    @Modifying
    @Query("UPDATE Item i SET i.status = 'REQUESTED', i.borrowerEmail = :email, i.borrowerName = :name, i.borrowerPhone = :phone WHERE i.id = :id AND i.status = 'AVAILABLE'")
    int requestItemAtomic(UUID id, String email, String name, String phone);
}