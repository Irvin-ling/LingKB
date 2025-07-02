package com.ling.lingkb.controller;
/*
 * ------------------------------------------------------------------
 * Copyright @ 2025 Hangzhou Ling Technology Co.,Ltd. All rights reserved.
 * ------------------------------------------------------------------
 * Product: LingKB
 * Module Name: LingKB
 * Date Created: 2025/6/30
 * Description:
 * ------------------------------------------------------------------
 * Modification History
 * DATE            Name           Description
 * ------------------------------------------------------------------
 * 2025/6/30       spt
 * ------------------------------------------------------------------
 */

import com.ling.lingkb.llm.ModelTrainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The change of model data
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/30
 */
@RestController
@RequestMapping("/model")
public class ModelController {
    private ModelTrainer modelTrainer;

    @Autowired
    public ModelController(ModelTrainer modelTrainer) {
        this.modelTrainer = modelTrainer;
    }
}
