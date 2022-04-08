package com.szcinda.controller;

import com.szcinda.service.callback.CallParams;
import com.szcinda.service.callback.CallService;
import com.szcinda.service.callback.CallbackData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("callback")
public class CallbackController {

    private final CallService callService;

    private final Logger logger = LoggerFactory.getLogger(CallbackController.class);

    public CallbackController(CallService callService) {
        this.callService = callService;
    }

    @GetMapping("test/{phone}")
    public Result<String> test(@PathVariable String phone) {
        CallParams callParams = new CallParams();
        callParams.setPhone(phone);
        callParams.setDataId("test");
        callParams.setTemplateId("27667");
        callService.call(callParams);
        return Result.success();
    }

    @PostMapping({"data"})
    public Result<String> callback2(@RequestBody CallbackData callbackData) {
        logger.info(callbackData.toString());
        callService.create(callbackData);
        return Result.success();
    }
}
