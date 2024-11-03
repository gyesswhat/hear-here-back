package com.example.hearhere.service;

import com.example.hearhere.repository.AsmrRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class InvalidAsmrDeleteScheduler {
    @Autowired
    private AsmrRepository asmrRepository;

    @Scheduled(fixedDelay = 1000*60*60)
    public void deleteInvalidAsmr() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        asmrRepository.deleteOldRecordsWithoutUserId(formatter.format(cutoffTime));
    }

}
