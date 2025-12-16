package com.finalproject.example.EmailClientAI.controller;

import com.finalproject.example.EmailClientAI.dto.StatusDTO;
import com.finalproject.example.EmailClientAI.dto.email.EmailDTO;
import com.finalproject.example.EmailClientAI.entity.Email;
import com.finalproject.example.EmailClientAI.entity.Status;
import com.finalproject.example.EmailClientAI.repository.EmailRepository;
import com.finalproject.example.EmailClientAI.repository.StatusRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statuses")
@RequiredArgsConstructor
@Transactional
public class StatusController {
    private final StatusRepository statusRepository;
    private final EmailRepository emailRepository;

    @GetMapping("/all")
    public ResponseEntity<List<StatusDTO>> getAll() {

        List<Status> statuses = statusRepository.findAll();
        List<StatusDTO> statusDTOs = statuses.stream()
                .map(status -> new StatusDTO(status.getId(), status.getName(), status.getOrderIndex()))
                .toList();
        return ResponseEntity.ok(statusDTOs);
    }

    @GetMapping("/all-visible")
    public ResponseEntity<List<StatusDTO>> getAllVisible() {

        List<Status> statuses = statusRepository.findAll();
        List<StatusDTO> statusDTOs = statuses.stream()
                .map(status -> new StatusDTO(status.getId(), status.getName(), status.getOrderIndex()))
                .filter(statusDTO -> !statusDTO.getName().equals("SNOOZED") && !statusDTO.getName().equals("REMOVED"))
                .toList();
        return ResponseEntity.ok(statusDTOs);
    }

    @PutMapping("/update")
    public ResponseEntity<StatusDTO> updateStatus(@RequestParam Long id, @RequestParam String name) {
        Status status = statusRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Status not found"));
        status.setName(name);
        statusRepository.save(status);
        StatusDTO statusDTO = new StatusDTO(status.getId(), status.getName(), status.getOrderIndex());
        return ResponseEntity.ok(statusDTO);
    }

    @PostMapping("/create")
    public ResponseEntity<StatusDTO> createStatus(@RequestParam String name, @RequestParam Integer orderIndex) {
        Status status = new Status();
        status.setName(name);
        status.setOrderIndex(orderIndex);
        List<Status> existingStatuses = statusRepository.findAll();
        for( Status existingStatus : existingStatuses) {
            if(existingStatus.getOrderIndex() >= orderIndex) {
                existingStatus.setOrderIndex(existingStatus.getOrderIndex() + 1);
                statusRepository.save(existingStatus);
            }
        }
        statusRepository.save(status);
        StatusDTO statusDTO = new StatusDTO(status.getId(), status.getName(), status.getOrderIndex());
        return ResponseEntity.ok(statusDTO);
    }

    @DeleteMapping("/delete")
    @Transactional // Ensures the move and delete happen atomically
    public ResponseEntity<Void> deleteStatus(@RequestParam Long deletedId, @RequestParam Long moveToId) {

        // 1. Get the destination status
        Status moveToStatus = statusRepository.findById(moveToId)
                .orElseThrow(() -> new RuntimeException("Status to move to not found"));

        // 2. Find emails linked to the status being deleted
        // (Note: findAll() is slow; consider adding findByStatusId(Long id) to your repository)
        List<Email> emailsToUpdate = emailRepository.findAll().stream()
                .filter(email -> email.getStatus() != null && email.getStatus().getId().equals(deletedId))
                .toList();

        // 3. UPDATE BOTH ID AND OBJECT
        for (Email email : emailsToUpdate) {
            // A. Update the DB column (Since this is the 'owner' in your entity)
            email.setStatusId(moveToStatus.getId());

            // B. Update the in-memory object (THE FIX)
            // Even though this field is 'updatable=false', setting it here breaks the
            // link to the 'deletedStatus' object in memory. Hibernate will now see
            // that this email points to a valid, existing status.
            email.setStatus(moveToStatus);

            emailRepository.save(email);
        }

        // 4. Handle Deletion and Reordering
        Status deletedStatus = statusRepository.findById(deletedId)
                .orElseThrow(() -> new RuntimeException("Status to delete not found"));

        Integer deletedOrderIndex = deletedStatus.getOrderIndex();

        // Now safe to delete because no Email objects in memory reference this status anymore
        statusRepository.deleteById(deletedId);

        // Reorder remaining statuses
        List<Status> statusesToReorder = statusRepository.findAll().stream()
                .filter(s -> s.getOrderIndex() > deletedOrderIndex)
                .toList();

        for (Status s : statusesToReorder) {
            s.setOrderIndex(s.getOrderIndex() - 1);
            statusRepository.save(s);
        }

        return ResponseEntity.ok().build();
    }
}
