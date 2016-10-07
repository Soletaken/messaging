package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class MyController {

    @Autowired
    private MyGateway gateway;

    @RequestMapping(path = "/{word}", method = RequestMethod.GET)
    public String find(@PathVariable String word){
        String result =
                gateway.process(word);
        return "Hello World: " + result;
//        return "asd";
    }
}
