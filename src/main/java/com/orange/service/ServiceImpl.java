package com.orange.service;


import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.*;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;


public class ServiceImpl {
    public final String SUFFIX = "/";

    public Response upload_imageServer(String path, MultipartFile file) {

        if (file == null)
            return Response.status(400).entity("Invalid form data").build();
        // create our destination folder, if it not exists
        try {
            createFolderIfNotExists(path);
        } catch (SecurityException se) {
            return Response.status(500)
                    .entity("Error in upload_imageServer : Can not create destination folder on server ")
                    .build();
        }

        try {

            store(file);
        } catch (Exception e) {
            return Response.status(500).entity("Error in upload_imageServer : Can not save file").build();
        }
        return Response.status(200).entity("upload_imageServer Response : File saved to "+path ).build();
    }

    public void store(MultipartFile file) {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        System.out.println("filename  " + filename);
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Error in store : Failed to store empty file " + filename);
            }
            // This is a security check
            if (filename.contains("..")) {
                throw new RuntimeException("Error in store : Cannot store file with relative path outside current directory " + filename);
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, Paths.get("C:\\uploadedFiles\\" + filename), StandardCopyOption.REPLACE_EXISTING);
                upload_S3("C:\\uploadedFiles\\" + filename, filename);
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

    private final Response upload_S3(String filepath, String filename) {

        Regions clientRegion = Regions.DEFAULT_REGION;
        String bucketName = "mys3-bucket-orange2019";

        File file = new File(filepath);
        long contentLength = file.length();
        long partSize = 5 * 1024 * 1024; // Set part size to 5 MB.

        try {
            AWSCredentials credentials = new BasicAWSCredentials("AKIAW4LYQJANW6JSDWUY", "rZPFImMbWGsj4xnYuW1EvzrEBAEkjG7Ga45eujdA");
            AmazonS3 s3Client = new AmazonS3Client(credentials);

            PutObjectResult por = s3Client.putObject(new PutObjectRequest(bucketName, filename, new File(filepath))
                    .withCannedAcl(CannedAccessControlList.PublicRead));

        } catch (Exception e) {
            return Response.status(500).entity("Error in upload_S3 : "+e).build();
        }

        return   Response.status(200).entity("File store successfully store in s3 ").build();
    }

    public void createFolder(String bucketName, String folderName, AmazonS3 client) {
        // create meta-data for your folder and set content-length to 0
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(0);
        // create empty content
        InputStream emptyContent = new ByteArrayInputStream(new byte[0]);
        // create a PutObjectRequest passing the folder name suffixed by /
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName,
                folderName + SUFFIX, emptyContent, metadata);
        // send request to S3 to create folder
        client.putObject(putObjectRequest);

    }


    public void s3_to_collection() {
        final String collectionId = "MyCollection";
        final String bucket = "mytests3bucket19";//"mys3-bucket-orange2019";
        final String photo = "sachin1.jpg";
        try {
            AWSCredentials credentials = new BasicAWSCredentials("AKIAW4LYQJANW6JSDWUY", "rZPFImMbWGsj4xnYuW1EvzrEBAEkjG7Ga45eujdA");
            AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard().withRegion("ap-south-1").withCredentials(new AWSStaticCredentialsProvider(credentials)).build();

            Image image = new Image()
                    .withS3Object(new S3Object()
                            .withBucket(bucket)
                            .withName(photo));

            IndexFacesRequest indexFacesRequest = new IndexFacesRequest()
                    .withImage(image)
                    .withQualityFilter(QualityFilter.AUTO)
                    .withMaxFaces(1)
                    .withCollectionId(collectionId)
                    .withExternalImageId(photo)
                    .withDetectionAttributes("DEFAULT");

            IndexFacesResult indexFacesResult = rekognitionClient.indexFaces(indexFacesRequest);

            System.out.println("Results for " + photo);
            System.out.println("Faces indexed:");
            List<FaceRecord> faceRecords = indexFacesResult.getFaceRecords();
            for (FaceRecord faceRecord : faceRecords) {
                System.out.println("  Face ID: " + faceRecord.getFace().getFaceId());
                System.out.println("  Location:" + faceRecord.getFaceDetail().getBoundingBox().toString());
            }

            List<UnindexedFace> unindexedFaces = indexFacesResult.getUnindexedFaces();
            System.out.println("Faces not indexed:" + unindexedFaces.size());
            for (UnindexedFace unindexedFace : unindexedFaces) {
                System.out.println("  Location:" + unindexedFace.getFaceDetail().getBoundingBox().toString());
                System.out.println("  Reasons:");
                for (String reason : unindexedFace.getReasons()) {
                    System.out.println("   " + reason);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Response make_Collection() {
        try {
            AWSCredentials credentials = new BasicAWSCredentials("AKIAW4LYQJANW6JSDWUY", "rZPFImMbWGsj4xnYuW1EvzrEBAEkjG7Ga45eujdA");
            AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard().withRegion("ap-south-1").withCredentials(new AWSStaticCredentialsProvider(credentials)).build();

            String collectionId = "MyCollection";
            System.out.println("Creating collection: " + collectionId);

            CreateCollectionRequest request = new CreateCollectionRequest().withCollectionId(collectionId);
            CreateCollectionResult createCollectionResult = rekognitionClient.createCollection(request);
            //  System.out.println("CollectionArn : " + createCollectionResult.getCollectionArn());
            //  System.out.println("Status code : " + createCollectionResult.getStatusCode().toString());
        } catch (Exception e) {
            System.out.println("Exception   " + e);

        }

        return null;
    }

    public Response check_face_validation() {

        final String collectionId = "MyCollection";
        final String bucket = "mytests3bucket19";
        final String photo = "sachin1.jpg";

        try {
            AWSCredentials credentials = new BasicAWSCredentials("AKIAW4LYQJANW6JSDWUY", "rZPFImMbWGsj4xnYuW1EvzrEBAEkjG7Ga45eujdA");
            AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard().withRegion("ap-south-1").withCredentials(new AWSStaticCredentialsProvider(credentials)).build();

            ObjectMapper objectMapper = new ObjectMapper();

            // Get an image object from S3 bucket.
            Image image = new Image()
                    .withS3Object(new S3Object()
                            .withBucket(bucket)
                            .withName(photo));

            // Search collection for faces similar to the largest face in the image.
            SearchFacesByImageRequest searchFacesByImageRequest = new SearchFacesByImageRequest()
                    .withCollectionId(collectionId)
                    .withImage(image)
                    .withFaceMatchThreshold(70F)
                    .withMaxFaces(2);

            SearchFacesByImageResult searchFacesByImageResult =
                    rekognitionClient.searchFacesByImage(searchFacesByImageRequest);

            System.out.println("Faces matching largest face in image from" + photo);
            List<FaceMatch> faceImageMatches = searchFacesByImageResult.getFaceMatches();
            for (FaceMatch face : faceImageMatches) {
                System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(face));
                System.out.println();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
