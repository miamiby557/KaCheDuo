package com.szcinda.service.driver;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class DriverExcelListener extends AnalysisEventListener<DriverImportDto> {
    private List<DriverImportDto> importDatas = new ArrayList<>();

    @Override
    public void invoke(DriverImportDto importDto, AnalysisContext analysisContext) {
        if (StringUtils.hasLength(importDto.getVehicleNo())) {
            importDatas.add(importDto);
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
    }
}
