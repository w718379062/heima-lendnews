package com.heima.wemedia.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class WmNewsAutoScan {
    @Autowired
    private WmNewsAutoScanService wmNewsAutoScanService;



    @Test
    void autoScanWmNews() {
        wmNewsAutoScanService.autoScanWmNews(6245);
    }
}