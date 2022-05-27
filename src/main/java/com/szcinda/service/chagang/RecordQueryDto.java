package com.szcinda.service.chagang;

import com.szcinda.service.PageParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class RecordQueryDto extends PageParams {
    private LocalDate createTimeStart;
    private LocalDate createTimeEnd;
    private LocalDate inquireTimeStart;
    private LocalDate inquireTimeEnd;
    private String answerUser;
}
