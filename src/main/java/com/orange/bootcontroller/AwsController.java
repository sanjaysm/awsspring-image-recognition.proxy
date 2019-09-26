package com.orange.bootcontroller;


import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.orange.errorhandler.ResourceNotFoundException;
import com.sun.istack.NotNull;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;


@RestController
public class AwsController {

    @RequestMapping(path = "/", method = RequestMethod.GET)
    @ResponseBody
    public String home() {

        /*Regions clientRegion = Regions.DEFAULT_REGION;
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(clientRegion)
                .withCredentials(new ProfileCredentialsProvider())
                .build();*/

        return "Orange face recognition API !";
    }




    @RequestMapping(path = "/upload_image/", method = RequestMethod.POST, produces = "application/json; charset=UTF-8")
    @ResponseBody
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response getImage(@RequestPart("file") @Valid @NotNull @NotBlank MultipartFile file) throws ResourceNotFoundException {

        final String UPLOAD_FOLDER = "c:/uploadedFiles/";

        // check if all form parameters are provided
        if (file == null)
            return Response.status(400).entity("Invalid form data").build();

        // create our destination folder, if it not exists
        try {
            createFolderIfNotExists(UPLOAD_FOLDER);
        } catch (SecurityException se) {
            return Response.status(500)
                    .entity("Can not create destination folder on server")
                    .build();
        }
        String uploadedFileLocation = UPLOAD_FOLDER + file.getName();
        try {

            store(file);
        } catch (Exception e) {
            return Response.status(500).entity("Can not save file").build();
        }
        return Response.status(200)
                .entity("File saved to " + uploadedFileLocation).build();


    }

    public void store(MultipartFile file) {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        System.out.println("filename  "+filename);
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file " + filename);
            }

            // This is a security check
            if (filename.contains("..")) {
                throw new RuntimeException("Cannot store file with relative path outside current directory " + filename);
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, Paths.get("C:\\uploadedFiles\\" + filename), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file " + filename, e);
        }
    }


    private void createFolderIfNotExists(String dirName)
            throws SecurityException {
        File theDir = new File(dirName);
        if (!theDir.exists()) {
            theDir.mkdir();
        }
    }
}
