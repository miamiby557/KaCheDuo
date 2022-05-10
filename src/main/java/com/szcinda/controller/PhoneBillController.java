package com.szcinda.controller;

import com.szcinda.controller.util.PhoneBillExportBill;
import com.szcinda.repository.PhoneBill;
import com.szcinda.service.PageResult;
import com.szcinda.service.bill.BillQuery;
import com.szcinda.service.bill.PhoneBillService;
import net.sf.jxls.transformer.XLSTransformer;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("phoneBill")
public class PhoneBillController {

    private final PhoneBillService phoneBillService;

    public PhoneBillController(PhoneBillService phoneBillService) {
        this.phoneBillService = phoneBillService;
    }


    @PostMapping("query")
    public PageResult<PhoneBill> query(@RequestBody BillQuery query) {
        return phoneBillService.query(query);
    }


    static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @PostMapping("export")
    public void export(@RequestParam(required = false) String createDateStart, @RequestParam(required = false) String createDateEnd,
                       @RequestParam(required = false) String phone, @RequestParam(required = false) String status,
                       @RequestParam(required = false) String vehicleNo, @RequestParam(required = false) String account,
                       @RequestParam(required = false) String company,
                       HttpServletResponse response) throws Exception {
        BillQuery params = new BillQuery();
        if (StringUtils.hasText(createDateStart)) {
            params.setCallTimeStart(LocalDate.parse(createDateStart, dateTimeFormatter));
        }
        if (StringUtils.hasText(createDateEnd)) {
            params.setCallTimeEnd(LocalDate.parse(createDateEnd, dateTimeFormatter));
        }
        params.setPhone(phone);
        params.setStatus(status);
        params.setCompany(company);
        params.setVehicleNo(vehicleNo);
        params.setAccount(account);
        List<PhoneBill> phoneBillList = phoneBillService.queryAll(params);
        List<PhoneBillExportBill> exportBills = new ArrayList<>();
        if (phoneBillList.size() > 0) {
            for (PhoneBill phoneBill : phoneBillList) {
                exportBills.add(PhoneBillExportBill.generateBill(phoneBill));
            }
        }
        Map<String, Object> beans = new HashMap<>();
        beans.put("phoneBillList", exportBills);
        InputStream is = null;
        OutputStream os = response.getOutputStream();
        // 下载EXCEL
        String fName = URLEncoder.encode("电话账单", "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fName + ".xls");
        try {
            // 获取模板文件
            is = this.getClass().getClassLoader().getResourceAsStream("电话账单.xls");
            // 实例化 XLSTransformer 对象
            XLSTransformer xlsTransformer = new XLSTransformer();
            // 获取 Workbook ，传入 模板 和 数据
            Workbook workbook = xlsTransformer.transformXLS(is, beans);
            // 输出
            workbook.write(os);
            os.flush();
            // 关闭和刷新管道，不然可能会出现表格数据不齐，打不开之类的问题
        } catch (Exception ignored) {

        } finally {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        }
    }
}
