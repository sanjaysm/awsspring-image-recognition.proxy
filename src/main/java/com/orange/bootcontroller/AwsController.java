package com.orange.bootcontroller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AwsController {
    @RequestMapping(path = "/", method = RequestMethod.GET)
    @ResponseBody
    public String home() {
        return "Orange face recognition API !";
    }

/*  @RequestMapping(path = "/getAll/{id}", method = RequestMethod.GET, produces = "application/json; charset=UTF-8")
    @ResponseBody
    public ResponseEntity<Emp_Info> getEmpDetails(@PathVariable(value = "id") Integer id)
            throws ResourceNotFoundException {

        Emp_Info employee = emp_service.getById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found for this id :: " + id));
        return ResponseEntity.ok().body(employee);

    }*/



}
