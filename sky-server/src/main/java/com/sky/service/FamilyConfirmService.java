package com.sky.service;

import com.sky.dto.FamilyConfirmDTO;
import com.sky.vo.FamilyConfirmVO;

public interface FamilyConfirmService {
    FamilyConfirmVO confirm(FamilyConfirmDTO request);
}
