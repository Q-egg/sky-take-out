package com.sky.controller.user;

import com.sky.dto.FamilyConfirmDTO;
import com.sky.result.Result;
import com.sky.service.FamilyConfirmService;
import com.sky.vo.FamilyConfirmVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/order")
@Api(tags = "家庭模拟付款")
public class FamilyConfirmController {
    @Autowired
    private FamilyConfirmService familyConfirmService;

    @PutMapping("/familyConfirm")
    @ApiOperation("家庭确认订单，不实际扣款")
    public Result<FamilyConfirmVO> familyConfirm(@RequestBody FamilyConfirmDTO request) {
        return Result.success(familyConfirmService.confirm(request));
    }
}
