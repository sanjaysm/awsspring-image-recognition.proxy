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

    public String imgStoreagepath="";
    public String bucketname="";
    public String accessKey="";
    public String secretKey="";
    public String awscollectionname="";
    public String awsRegion="";
   // public String facematchThreshold="";

    public Response upload_imageServer(String path, MultipartFile file) {
        Response response = null;
        if (file == null)
            return Response.status(400).entity("Invalid form data").build();
        // create our destination folder, if it not exists
        try {
            createFolderIfNotExists(path);
        } catch (SecurityException se) {
            return Response.status(500).entity("Error in upload_imageServer : Can not create destination folder on server ").build();
        }
        try {
            response = store(file ,path);
        } catch (Exception e) {
            return Response.status(500).entity("Error in upload_imageServer : Can not save file").build();
        }
        System.out.println("upload_imageServer Response : File saved to " + path);
        return response;

    }

    public Response store(MultipartFile file, String path) {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        System.out.println("filename  " + filename);
        Response res = null;
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Exception in store : Failed to store empty file " + filename);
            }
            // This is a security check
           /* if (filename.contains("..")) {
                throw new RuntimeException("Error in store : Cannot store file with relative path outside current directory " + filename);
            }*/
            try (InputStream inputStream = file.getInputStream()) {

                Files.copy(inputStream, Paths.get(path + filename), StandardCopyOption.REPLACE_EXISTING);
               // Files.copy(inputStream, Paths.get("C:\\uploadedFiles\\" + filename), StandardCopyOption.REPLACE_EXISTING);
                res = upload_S3(path + filename, filename);
               // res = upload_S3("C:\\uploadedFiles\\" + filename, filename);
            }
        } catch (IOException e) {
            //  throw new RuntimeException("Failed to store file " + filename, e);
            return Response.status(500).entity("Exception in store : " + e).build();
        }
        return res;
    }

    private Response upload_S3(String filepath, String filename) {
        Regions clientRegion = Regions.DEFAULT_REGION;
        String bucketName = "mys3-bucket-orange2019";

        File file = new File(filepath);
        long contentLength = file.length();
        long partSize = 5 * 1024 * 1024; // Set part size to 5 MB.

        try {
            AWSCredentials credentials = new BasicAWSCredentials("", "");
            AmazonS3 s3Client = new AmazonS3Client(credentials);

            PutObjectResult por = s3Client.putObject(new PutObjectRequest(bucketName, filename, new File(filepath))
                    .withCannedAcl(CannedAccessControlList.PublicRead));

        } catch (Exception e) {
            return Response.status(500).entity("Exception in upload_S3 : " + e).build();
        }

        return Response.status(200).entity("upload_S3: File upload successfully  in s3 ").build();
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

    public Response s3_to_collection() {
        final String collectionId = "MyCollection";
        final String bucket = "mytests3bucket19";
        final String photo = "sachin1.jpg";
        try {
            AWSCredentials credentials = new BasicAWSCredentials("", "");
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
            return Response.status(500).entity("Exception in s3_to_collection" + e).build();
        }

        return Response.status(200).entity("successfully move s3 to collection").build();
    }

    public Response make_Collection() {
        String collectionId = "Orance_collection";//"MyCollection";
        try {
            AWSCredentials credentials = new BasicAWSCredentials("", "");
            AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard().withRegion("ap-south-1").withCredentials(new AWSStaticCredentialsProvider(credentials)).build();
            System.out.println("Creating collection: " + collectionId);

            CreateCollectionRequest request = new CreateCollectionRequest().withCollectionId(collectionId);
            CreateCollectionResult createCollectionResult = rekognitionClient.createCollection(request);

        } catch (Exception e) {
            return Response.status(500).entity("Exception in make_Collection " + e).build();
        }

        return Response.status(200).entity("create collection " + collectionId).build();
    }

    public Response check_face_validation() {

        final String collectionId = "orange";
        final String bucket = "mytests3bucket19";
        final String photo = "sachin1.jpg";

        try {
            AWSCredentials credentials = new BasicAWSCredentials("", "");
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
                    .withFaceMatchThreshold(80F)
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
            return Response.status(500).entity("Exception in check_face_validation : " + e).build();
        }
        return Response.status(200).entity("check_face_validation Response").build();
    }

    private void createFolderIfNotExists(String dirName)
            throws SecurityException {
        File theDir = new File(dirName);
        if (!theDir.exists()) {
            theDir.mkdir();
        }
    }
}
