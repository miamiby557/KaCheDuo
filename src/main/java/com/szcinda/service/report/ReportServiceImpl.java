package com.szcinda.service.report;

import com.szcinda.repository.DriverRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    private final DriverRepository driverRepository;

    public ReportServiceImpl(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    @Override
    public List<ReportDto> generate(String owner) {

        return null;
    }
}
