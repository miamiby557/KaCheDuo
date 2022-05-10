package com.szcinda.controller.util;

import com.szcinda.repository.PhoneBill;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.io.Serializable;

@Data
public class PhoneBillExportBill implements Serializable {
    private String caller = ""; // 主叫号
    private String called = ""; // 被叫号
    private String vehicleNo = "";
    private String account = "";
    private String company = "";
    private int duration = 0; //总通话时长，包含ivr时间
    private int ivrTime = 0; // Ivr播放总时长
    private int ivrCount = 0; //Ivr播放次数
    private String callCreateTime = ""; // 通话创建时间
    private String answerTime = ""; // 通话开始时间
    private String releaseTime = ""; // 通话结束时间
    private String status = "";


    public static PhoneBillExportBill generateBill(PhoneBill phoneBill) {
        PhoneBillExportBill bill = new PhoneBillExportBill();
        if (StringUtils.hasText(phoneBill.getCaller())) {
            bill.setCaller(phoneBill.getCaller());
        }
        if (StringUtils.hasText(phoneBill.getCalled())) {
            bill.setCalled(phoneBill.getCalled());
        }
        if (StringUtils.hasText(phoneBill.getVehicleNo())) {
            bill.setVehicleNo(phoneBill.getVehicleNo());
        }
        if (StringUtils.hasText(phoneBill.getAccount())) {
            bill.setAccount(phoneBill.getAccount());
        }
        if (StringUtils.hasText(phoneBill.getCompany())) {
            bill.setCompany(phoneBill.getCompany());
        }
        bill.setDuration(phoneBill.getDuration());
        bill.setIvrTime(phoneBill.getIvrTime());
        bill.setIvrCount(phoneBill.getIvrCount());
        if (phoneBill.getCallCreateTime() != null) {
            bill.setCallCreateTime(phoneBill.getCallCreateTime().toString().replace("T", " "));
        }
        if (phoneBill.getAnswerTime() != null) {
            bill.setAnswerTime(phoneBill.getAnswerTime().toString().replace("T", " "));
            if(bill.getAnswerTime().startsWith("1970")){
                bill.setAnswerTime("");
            }
        }
        if (phoneBill.getReleaseTime() != null) {
            bill.setReleaseTime(phoneBill.getReleaseTime().toString().replace("T", " "));
        }
        if (StringUtils.hasText(phoneBill.getStatus())) {
            bill.setStatus(phoneBill.getStatus());
        }
        return bill;
    }
}
