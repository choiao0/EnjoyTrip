package com.enjoytrip.controller;

import com.enjoytrip.model.Attraction;
import com.enjoytrip.model.Gugun;
import com.enjoytrip.model.Sido;
import com.enjoytrip.service.AttractionService;
import com.enjoytrip.service.SidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AttractionController {

    @Autowired
    private AttractionService attractionService;

    @Autowired
    private SidoService sidoService;

    @GetMapping("/attractions")
    public List<Attraction> search(
            @RequestParam(required = false) Integer sidoCode,
            @RequestParam(required = false) Integer gugunCode,
            @RequestParam(required = false) Integer contentTypeId,
            @RequestParam(required = false) String keyword) {
        return attractionService.search(sidoCode, gugunCode, contentTypeId, keyword);
    }

    @GetMapping("/sidos")
    public List<Sido> sidos() {
        return sidoService.findAllSidos();
    }

    @GetMapping("/guguns")
    public List<Gugun> guguns(@RequestParam int sidoCode) {
        return sidoService.findGugunsBySidoCode(sidoCode);
    }
}
