package com.heima.freemarker.freemarker;


import com.heima.freemarker.entity.Student;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HelloController {

    @GetMapping("/basic")
    public String test (Model model){
        model.addAttribute("name","freemarker");
        Student student =new Student();
        student.setAge(25);
        student.setName("小明");
        //student.setBirthday(new Date());
        model.addAttribute("stu",student);
        return "01-basic";
    }
}
