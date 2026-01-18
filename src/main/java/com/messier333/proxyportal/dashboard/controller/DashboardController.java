package com.messier333.proxyportal.dashboard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;


@Controller
@RequiredArgsConstructor
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboardView(){
        return "dashboard/index";
    }


    @GetMapping("/dashboard/account/add")
    public String addAccountView(@RequestParam(name="error", required=false) String error, Model model){
        if(error != null){
            model.addAttribute("error", error);
        }
        return "dashboard/account-add";
    }
}
