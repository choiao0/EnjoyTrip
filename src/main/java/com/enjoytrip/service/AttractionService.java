package com.enjoytrip.service;

import com.enjoytrip.model.Attraction;
import com.enjoytrip.repository.AttractionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AttractionService {

    private final AttractionRepository attractionRepository;

    public AttractionService(AttractionRepository attractionRepository) {
        this.attractionRepository = attractionRepository;
    }

    public List<Attraction> search(Integer sidoCode, Integer gugunCode, Integer contentTypeId, String keyword) {
        return attractionRepository.search(sidoCode, gugunCode, contentTypeId, keyword);
    }
}
