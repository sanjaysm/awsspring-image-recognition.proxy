package com.orange.bootcontroller;



import com.orange.errorhandler.ResourceNotFoundException;
import com.orange.service.ServiceImpl;
import com.sun.istack.NotNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;



@RestController
public class AwsController {

    /*public  AwsController(){

    }*/

    @RequestMapping(path = "/uploadtoCollection", method = RequestMethod.GET)
    @ResponseBody
    public String uploadtoCollection() {

        ServiceImpl serImp = new ServiceImpl();
        serImp.s3_to_collection();
        return "Orange face recognition API !";
    }


    @RequestMapping(path = "/create_collection", method = RequestMethod.POST, produces = "application/json; charset=UTF-8")
    @ResponseBody
    public Response createCollection() {

        ServiceImpl ser= new ServiceImpl();
        return ser.make_Collection();
    }


    @RequestMapping(path = "/upload_image", method = RequestMethod.POST, produces = "application/json; charset=UTF-8")
    @ResponseBody
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response getImage(@RequestPart("file") @Valid @NotNull @NotBlank MultipartFile file) throws ResourceNotFoundException {

        final String UPLOAD_FOLDER = "c:/uploadedFiles/";
        ServiceImpl serviceIMP = new ServiceImpl();
        return serviceIMP.upload_imageServer(UPLOAD_FOLDER, file);

    }

    @RequestMapping(path = "/faceValidate", method = RequestMethod.POST, produces = "application/json; charset=UTF-8",consumes=MediaType.MULTIPART_FORM_DATA)
    @ResponseBody
    public Response faceValidate(@RequestPart("file") @Valid @NotNull @NotBlank MultipartFile file) {


        final String UPLOAD_FOLDER = "c:/validateimage/";
        ServiceImpl service= new ServiceImpl();
        return service.upload_imageServer(UPLOAD_FOLDER,file);
    }

}
