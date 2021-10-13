package org.example.service;

import org.example.enums.SendCodeEnum;
import org.example.util.JsonData;

public interface NotifyService {

    JsonData sendCode(SendCodeEnum sendCodeEnum, String to);

}
