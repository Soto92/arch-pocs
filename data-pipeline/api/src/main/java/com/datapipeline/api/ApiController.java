package com.datapipeline.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ApiController {

    @Autowired
    private TopSalesPerCityRepository topSalesPerCityRepository;

    @Autowired
    private TopSalesmanRepository topSalesmanRepository;

    @GetMapping("/topsalespercity")
    public List<TopSalesPerCity> getTopSalesPerCity() {
        return topSalesPerCityRepository.findAll();
    }

    @GetMapping("/topsalesman")
    public List<TopSalesman> getTopSalesman() {
        return topSalesmanRepository.findAll();
    }
}
