package com.parvez.blogs.service;

import com.parvez.blogs.repository.PostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostMaintenanceService {

    private final PostRepository postRepository;

    /**
     * Cron expression for 3:00 AM every day.
     * Format: second, minute, hour, day, month, day-of-week
     */

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeOldSoftDeletedPosts(){
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);

        log.info("Starting scheduled purge of soft-deleted posts older than {}", cutoffDate);

        int deletedCount = postRepository.deleteByDeletedTrueAndUpdatedAtBefore(cutoffDate);

        if (deletedCount > 0) {
            log.info("Successfully purged {} expired posts from the database.", deletedCount);
        } else {
            log.info("No expired posts found for purging.");
        }
    }
}
