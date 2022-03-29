package com.szcinda.service.driver;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.metadata.BaseRowModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DriverImportDto extends BaseRowModel {
    @ExcelProperty(value = "姓名")
    private String name;
    @ExcelProperty(value = "手机号码")
    private String phone;
    @ExcelProperty(value = "车牌号码")
    private String vehicleNo;
    @ExcelProperty(value = "公司全称")
    private String company;
}
