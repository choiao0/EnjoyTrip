package com.enjoytrip.service;

import com.enjoytrip.model.Gugun;
import com.enjoytrip.model.Sido;
import com.enjoytrip.repository.GugunRepository;
import com.enjoytrip.repository.SidoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SidoService {

    private final SidoRepository sidoRepository;
    private final GugunRepository gugunRepository;

    public SidoService(SidoRepository sidoRepository, GugunRepository gugunRepository) {
        this.sidoRepository = sidoRepository;
        this.gugunRepository = gugunRepository;
    }

    public List<Sido> findAllSidos() {
        return sidoRepository.findAll();
    }

    public List<Gugun> findGugunsBySidoCode(int sidoCode) {
        return gugunRepository.findBySidoCode(sidoCode);
    }
}
