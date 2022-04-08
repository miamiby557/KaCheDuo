package com.szcinda.service.report;

import java.util.List;

public interface ReportService {
    List<ReportDto> generate(String owner);
}
